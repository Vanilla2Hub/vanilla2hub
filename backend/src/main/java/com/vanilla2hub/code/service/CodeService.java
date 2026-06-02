package com.vanilla2hub.code.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilla2hub.code.dto.AttributeField;
import com.vanilla2hub.code.dto.CodeRequest;
import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.entity.Code;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;
    private final CodeTypeService codeTypeService;
    private final CodeCacheService codeCacheService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> EXTRA_TYPE = new TypeReference<>() {};

    public List<CodeResponse> getAllByCodeTypeId(Long codeTypeId) {
        codeTypeService.findOrThrow(codeTypeId);
        return codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId)
                .stream().map(CodeResponse::from).toList();
    }

    @Transactional
    public CodeResponse create(Long codeTypeId, CodeRequest request) {
        CodeType codeType = codeTypeService.findOrThrow(codeTypeId);
        if (codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(codeTypeId, request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 코드입니다: " + request.code());
        }

        List<AttributeField> schema = codeTypeService.parseSchema(codeType.getAttributeSchema());
        Map<String, Object> incomingExtra = parseExtra(request.extra());
        String extraJson = schema.isEmpty() ? request.extra()
                : serializeExtra(validateAndBuildExtra(schema, incomingExtra, Map.of(), true));

        Code code = Code.builder()
                .codeType(codeType)
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .extra(extraJson)
                .sortOrder(request.sortOrder())
                .build();
        CodeResponse response = CodeResponse.from(codeRepository.save(code));
        codeCacheService.refresh(codeTypeId, codeType.getCode());
        return response;
    }

    @Transactional
    public CodeResponse update(Long codeTypeId, Long codeId, CodeRequest request) {
        CodeType codeType = codeTypeService.findOrThrow(codeTypeId);
        Code code = findOrThrow(codeId, codeTypeId);
        if (code.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 기본 코드는 수정할 수 없습니다.");
        }

        List<AttributeField> schema = codeTypeService.parseSchema(codeType.getAttributeSchema());
        Map<String, Object> existingExtra = parseExtra(code.getExtra());
        Map<String, Object> incomingExtra = parseExtra(request.extra());
        String extraJson = schema.isEmpty() ? request.extra()
                : serializeExtra(validateAndBuildExtra(schema, incomingExtra, existingExtra, false));

        code.update(request.name(), request.description(), extraJson, request.sortOrder());
        CodeResponse response = CodeResponse.from(code);
        codeCacheService.refresh(codeTypeId, codeType.getCode());
        return response;
    }

    @Transactional
    public void delete(Long codeTypeId, Long codeId) {
        CodeType codeType = codeTypeService.findOrThrow(codeTypeId);
        Code code = findOrThrow(codeId, codeTypeId);
        if (code.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 기본 코드는 삭제할 수 없습니다.");
        }
        List<String> usages = codeTypeService.findCodeReferences(codeType.getCode(), code.getCode());
        if (!usages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다음 코드에서 참조 중이어서 삭제할 수 없습니다: " + String.join(", ", usages));
        }
        code.delete();
        codeCacheService.refresh(codeTypeId, codeType.getCode());
    }

    public byte[] exportCsv(Long codeTypeId) throws IOException {
        codeTypeService.findOrThrow(codeTypeId);
        List<Code> list = codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader("code", "name", "description", "extra", "sortOrder").build())) {
            for (Code c : list) {
                printer.printRecord(c.getCode(), c.getName(), c.getDescription(), c.getExtra(), c.getSortOrder());
            }
        }
        return out.toByteArray();
    }

    @Transactional
    public Map<String, Integer> importCsv(Long codeTypeId, MultipartFile file) throws IOException {
        CodeType codeType = codeTypeService.findOrThrow(codeTypeId);
        int created = 0, skipped = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = format.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord r : parser) {
                String codeVal = r.get("code").toUpperCase();
                if (codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(codeTypeId, codeVal)) { skipped++; continue; }
                codeRepository.save(Code.builder()
                        .codeType(codeType)
                        .code(codeVal)
                        .name(r.get("name"))
                        .description(nullIfBlank(r.get("description")))
                        .extra(nullIfBlank(r.get("extra")))
                        .sortOrder(parseIntOrZero(r.get("sortOrder")))
                        .build());
                created++;
            }
        }
        if (created > 0) codeCacheService.refresh(codeTypeId, codeType.getCode());
        return Map.of("created", created, "skipped", skipped);
    }

    private Map<String, Object> validateAndBuildExtra(
            List<AttributeField> schema, Map<String, Object> incoming, Map<String, Object> existing, boolean isCreate) {
        Map<String, Object> result = new HashMap<>(incoming);
        for (AttributeField field : schema) {
            if (!isCreate && !field.editable()) {
                result.put(field.key(), existing.get(field.key()));
                continue;
            }
            applyDefaultValue(field, result);
            Object val = result.get(field.key());
            validateRequired(field, val);
            validateSelectOption(field, val);
        }
        return result;
    }

    private void applyDefaultValue(AttributeField field, Map<String, Object> result) {
        Object val = result.get(field.key());
        if ((val == null || val.toString().isBlank())
                && field.defaultValue() != null && !field.defaultValue().isBlank()) {
            result.put(field.key(), field.defaultValue());
        }
    }

    private void validateRequired(AttributeField field, Object val) {
        if (field.required() && (val == null || val.toString().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 필드가 누락됐습니다: " + field.label());
        }
    }

    private void validateSelectOption(AttributeField field, Object val) {
        if (!AttributeField.TYPE_SELECT.equals(field.type()) || val == null || val.toString().isBlank()) return;
        List<String> options = field.options() != null ? field.options() : List.of();
        if (!options.contains(val.toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'" + field.label() + "'의 유효하지 않은 값입니다: " + val);
        }
    }

    private Code findOrThrow(Long codeId, Long codeTypeId) {
        return codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(codeId, codeTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드를 찾을 수 없습니다: " + codeId));
    }

    private Map<String, Object> parseExtra(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, EXTRA_TYPE);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "extra 필드가 올바른 JSON 형식이 아닙니다.");
        }
    }

    private String serializeExtra(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "extra 직렬화 실패");
        }
    }

    private static String nullIfBlank(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static int parseIntOrZero(String v) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; } }
}
