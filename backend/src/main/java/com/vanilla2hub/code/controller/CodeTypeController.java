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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
