package com.vanilla2hub.code.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilla2hub.code.dto.AttributeField;
import com.vanilla2hub.code.dto.CodeTypeRequest;
import com.vanilla2hub.code.dto.CodeTypeResponse;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeRepository;
import com.vanilla2hub.code.repository.CodeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeTypeService {

    private final CodeTypeRepository codeTypeRepository;
    private final CodeRepository codeRepository;
    private final CodeCacheService codeCacheService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<AttributeField>> SCHEMA_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> EXTRA_TYPE   = new TypeReference<>() {};

    public List<CodeTypeResponse> getAll() {
        return codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()
                .stream().map(CodeTypeResponse::from).toList();
    }

    public CodeTypeResponse getById(Long id) {
        return CodeTypeResponse.from(findOrThrow(id));
    }

    @Transactional
    public CodeTypeResponse create(CodeTypeRequest request) {
        if (codeTypeRepository.existsByCodeAndDeletedFalse(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 코드타입입니다: " + request.code());
        }
        CodeType codeType = CodeType.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .attributeSchema(serializeSchema(request.attributeSchema()))
                .sortOrder(request.sortOrder())
                .build();
        return CodeTypeResponse.from(codeTypeRepository.save(codeType));
    }

    @Transactional
    public CodeTypeResponse update(Long id, CodeTypeRequest request) {
        CodeType codeType = findOrThrow(id);
        if (codeType.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 기본 코드타입은 수정할 수 없습니다.");
        }

        List<AttributeField> oldSchema = parseSchema(codeType.getAttributeSchema());
        List<AttributeField> newSchema = request.attributeSchema() != null ? request.attributeSchema() : List.of();

        Set<String> oldKeys = oldSchema.stream().map(AttributeField::key).collect(Collectors.toSet());
        Set<String> newKeys = newSchema.stream().map(AttributeField::key).collect(Collectors.toSet());

        // required=false → true 전환 시 기존 코드 값 보유 여부 확인
        Map<String, Boolean> oldRequired = oldSchema.stream()
                .collect(Collectors.toMap(AttributeField::key, AttributeField::required));
        for (AttributeField field : newSchema) {
            if (field.required() && oldKeys.contains(field.key()) && !oldRequired.getOrDefault(field.key(), false)) {
                validateRequiredTransition(id, field);
            }
        }

        // 삭제된 key → 모든 Code.extra에서 제거
        Set<String> deletedKeys = new HashSet<>(oldKeys);
        deletedKeys.removeAll(newKeys);
        for (String deletedKey : deletedKeys) {
            codeRepository.removeExtraKeyByCodeTypeId(id, deletedKey);
        }

        codeType.update(request.name(), request.description(), serializeSchema(newSchema), request.sortOrder());
        codeCacheService.refresh(codeType.getId(), codeType.getCode());
        return CodeTypeResponse.from(codeType);
    }

    @Transactional
    public void delete(Long id) {
        CodeType codeType = findOrThrow(id);
        if (codeType.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 기본 코드타입은 삭제할 수 없습니다.");
        }
        List<String> usages = findCodeReferences(codeType.getCode(), null);
        if (!usages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다음 코드에서 참조 중이어서 삭제할 수 없습니다: " + String.join(", ", usages));
        }
        codeType.delete();
        codeCacheService.evict(codeType.getCode());
    }

    public byte[] exportCsv() throws IOException {
        List<CodeType> list = codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader("code", "name", "description", "sortOrder").build())) {
            for (CodeType ct : list) {
                printer.printRecord(ct.getCode(), ct.getName(), ct.getDescription(), ct.getSortOrder());
            }
        }
        return out.toByteArray();
    }

    @Transactional
    public Map<String, Integer> importCsv(MultipartFile file) throws IOException {
        int created = 0, skipped = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = format.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord r : parser) {
                String code = r.get("code").toUpperCase();
                if (codeTypeRepository.existsByCodeAndDeletedFalse(code)) { skipped++; continue; }
                codeTypeRepository.save(CodeType.builder()
                        .code(code)
                        .name(r.get("name"))
                        .description(nullIfBlank(r.get("description")))
                        .sortOrder(parseIntOrZero(r.get("sortOrder")))
                        .build());
                created++;
            }
        }
        return Map.of("created", created, "skipped", skipped);
    }

    CodeType findOrThrow(Long id) {
        return codeTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드타입을 찾을 수 없습니다: " + id));
    }

    /**
     * targetCodeValue=null 이면 해당 CodeType을 참조하는 코드 전체 검색.
     * targetCodeValue 지정 시 그 값을 참조하는 코드만 검색.
     */
    List<String> findCodeReferences(String targetCodeTypeCode, String targetCodeValue) {
        List<String> usages = new ArrayList<>();
        for (CodeType ct : codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()) {
            collectReferencesFromCodeType(ct, targetCodeTypeCode, targetCodeValue, usages);
        }
        return usages;
    }

    private void collectReferencesFromCodeType(CodeType ct, String targetCodeTypeCode, String targetCodeValue, List<String> usages) {
        List<AttributeField> refFields = parseSchema(ct.getAttributeSchema()).stream()
                .filter(f -> AttributeField.TYPE_CODE_REF.equals(f.type()) && targetCodeTypeCode.equals(f.refCodeTypeCode()))
                .toList();
        if (refFields.isEmpty()) return;
        for (var code : codeRepository.findAllByCodeTypeIdAndDeletedFalse(ct.getId())) {
            Map<String, Object> extra = parseExtra(code.getExtra());
            for (AttributeField field : refFields) {
                if (matchesReference(extra.get(field.key()), targetCodeValue)) {
                    usages.add(ct.getCode() + " > " + code.getCode());
                }
            }
        }
    }

    private boolean matchesReference(Object val, String targetCodeValue) {
        if (val == null || val.toString().isBlank()) return false;
        return targetCodeValue == null || targetCodeValue.equals(val.toString());
    }

    private void validateRequiredTransition(Long codeTypeId, AttributeField field) {
        List<com.vanilla2hub.code.entity.Code> codes = codeRepository.findAllByCodeTypeIdAndDeletedFalse(codeTypeId);
        for (var code : codes) {
            Map<String, Object> extra = parseExtra(code.getExtra());
            Object val = extra.get(field.key());
            if (val == null || val.toString().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "일부 코드에 '" + field.label() + "' 값이 없어 필수로 변경할 수 없습니다.");
            }
        }
    }

    List<AttributeField> parseSchema(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, SCHEMA_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    String serializeSchema(List<AttributeField> schema) {
        if (schema == null || schema.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "attribute_schema 직렬화 실패");
        }
    }

    Map<String, Object> parseExtra(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, EXTRA_TYPE);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String nullIfBlank(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static int parseIntOrZero(String v) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; } }
}
