package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeRequest;
import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.entity.Code;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;
    private final CodeTypeService codeTypeService;

    public List<CodeResponse> getAllByCodeTypeId(Long codeTypeId) {
        codeTypeService.findOrThrow(codeTypeId);
        return codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId)
                .stream().map(CodeResponse::from).toList();
    }

    @Transactional
    public CodeResponse create(Long codeTypeId, CodeRequest request) {
        var codeType = codeTypeService.findOrThrow(codeTypeId);
        if (codeRepository.existsByCodeTypeIdAndCodeAndDeletedFalse(codeTypeId, request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 코드입니다: " + request.code());
        }
        Code code = Code.builder()
                .codeType(codeType)
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .extra(request.extra())
                .sortOrder(request.sortOrder())
                .build();
        return CodeResponse.from(codeRepository.save(code));
    }

    @Transactional
    public CodeResponse update(Long codeTypeId, Long codeId, CodeRequest request) {
        codeTypeService.findOrThrow(codeTypeId);
        Code code = findOrThrow(codeId, codeTypeId);
        code.update(request.name(), request.description(), request.extra(), request.sortOrder());
        return CodeResponse.from(code);
    }

    @Transactional
    public void delete(Long codeTypeId, Long codeId) {
        codeTypeService.findOrThrow(codeTypeId);
        findOrThrow(codeId, codeTypeId).delete();
    }

    public byte[] exportCsv(Long codeTypeId) throws IOException {
        codeTypeService.findOrThrow(codeTypeId);
        List<Code> list = codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM
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
        var codeType = codeTypeService.findOrThrow(codeTypeId);
        int created = 0, skipped = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = format.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord r : parser) {
                String codeVal = r.get("code").toUpperCase();
                if (codeRepository.existsByCodeTypeIdAndCodeAndDeletedFalse(codeTypeId, codeVal)) { skipped++; continue; }
                String extra = nullIfBlank(r.get("extra"));
                codeRepository.save(Code.builder()
                        .codeType(codeType)
                        .code(codeVal)
                        .name(r.get("name"))
                        .description(nullIfBlank(r.get("description")))
                        .extra(extra)
                        .sortOrder(parseIntOrZero(r.get("sortOrder")))
                        .build());
                created++;
            }
        }
        return Map.of("created", created, "skipped", skipped);
    }

    private Code findOrThrow(Long codeId, Long codeTypeId) {
        return codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(codeId, codeTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드를 찾을 수 없습니다: " + codeId));
    }

    private static String nullIfBlank(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static int parseIntOrZero(String v) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; } }
}
