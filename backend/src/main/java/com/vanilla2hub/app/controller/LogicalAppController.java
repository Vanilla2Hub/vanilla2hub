package com.vanilla2hub.app.controller;

import com.vanilla2hub.app.dto.LogicalAppRequest;
import com.vanilla2hub.app.dto.LogicalAppResponse;
import com.vanilla2hub.app.service.LogicalAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class LogicalAppController {

    private final LogicalAppService logicalAppService;

    @GetMapping
    public List<LogicalAppResponse> getAll() {
        return logicalAppService.getAll();
    }

    @GetMapping("/{id}")
    public LogicalAppResponse getById(@PathVariable Long id) {
        return logicalAppService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogicalAppResponse create(@Valid @RequestBody LogicalAppRequest request) {
        return logicalAppService.create(request);
    }

    @PutMapping("/{id}")
    public LogicalAppResponse update(@PathVariable Long id, @Valid @RequestBody LogicalAppRequest request) {
        return logicalAppService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        logicalAppService.delete(id);
    }
}
