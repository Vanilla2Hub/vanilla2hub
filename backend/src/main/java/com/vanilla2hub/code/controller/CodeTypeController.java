package com.vanilla2hub.code.controller;

import com.vanilla2hub.code.dto.CodeRequest;
import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.dto.CodeTypeRequest;
import com.vanilla2hub.code.dto.CodeTypeResponse;
import com.vanilla2hub.code.service.CodeService;
import com.vanilla2hub.code.service.CodeTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/code-types")
@RequiredArgsConstructor
public class CodeTypeController {

    private final CodeTypeService codeTypeService;
    private final CodeService codeService;

    @GetMapping
    public List<CodeTypeResponse> getAll() {
        return codeTypeService.getAll();
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportCodeTypes() throws IOException {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"code-types.csv\"")
                .body(codeTypeService.exportCsv());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Integer> importCodeTypes(@RequestParam("file") MultipartFile file) throws IOException {
        return codeTypeService.importCsv(file);
    }

    @GetMapping("/{id}")
    public CodeTypeResponse getById(@PathVariable Long id) {
        return codeTypeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodeTypeResponse create(@Valid @RequestBody CodeTypeRequest request) {
        return codeTypeService.create(request);
    }

    @PutMapping("/{id}")
    public CodeTypeResponse update(@PathVariable Long id, @Valid @RequestBody CodeTypeRequest request) {
        return codeTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        codeTypeService.delete(id);
    }

    @GetMapping("/{codeTypeId}/codes")
    public List<CodeResponse> getCodes(@PathVariable Long codeTypeId) {
        return codeService.getAllByCodeTypeId(codeTypeId);
    }

    @GetMapping(value = "/{codeTypeId}/codes/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportCodes(@PathVariable Long codeTypeId) throws IOException {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"codes.csv\"")
                .body(codeService.exportCsv(codeTypeId));
    }

    @PostMapping(value = "/{codeTypeId}/codes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Integer> importCodes(@PathVariable Long codeTypeId,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        return codeService.importCsv(codeTypeId, file);
    }

    @PostMapping("/{codeTypeId}/codes")
    @ResponseStatus(HttpStatus.CREATED)
    public CodeResponse createCode(@PathVariable Long codeTypeId, @Valid @RequestBody CodeRequest request) {
        return codeService.create(codeTypeId, request);
    }

    @PutMapping("/{codeTypeId}/codes/{codeId}")
    public CodeResponse updateCode(@PathVariable Long codeTypeId, @PathVariable Long codeId,
                                   @Valid @RequestBody CodeRequest request) {
        return codeService.update(codeTypeId, codeId, request);
    }

    @DeleteMapping("/{codeTypeId}/codes/{codeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCode(@PathVariable Long codeTypeId, @PathVariable Long codeId) {
        codeService.delete(codeTypeId, codeId);
    }
}
