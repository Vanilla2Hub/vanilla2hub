package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeTypeRequest;
import com.vanilla2hub.code.dto.CodeTypeResponse;
import com.vanilla2hub.code.entity.CodeType;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeTypeService {

    private final CodeTypeRepository codeTypeRepository;

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
                .sortOrder(request.sortOrder())
                .build();
        return CodeTypeResponse.from(codeTypeRepository.save(codeType));
    }

    @Transactional
    public CodeTypeResponse update(Long id, CodeTypeRequest request) {
        CodeType codeType = findOrThrow(id);
        codeType.update(request.name(), request.description(), request.sortOrder());
        return CodeTypeResponse.from(codeType);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id).delete();
    }

    public byte[] exportCsv() throws IOException {
        List<CodeType> list = codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM (Excel 호환)
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

    private static String nullIfBlank(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static int parseIntOrZero(String v) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; } }
}
