package com.vanilla2hub.connector.controller;

import com.vanilla2hub.connector.dto.ConnectorConfigRequest;
import com.vanilla2hub.connector.dto.ConnectorConfigResponse;
import com.vanilla2hub.connector.service.ConnectorConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
public class ConnectorConfigController {

    private final ConnectorConfigService connectorConfigService;

    @GetMapping
    public List<ConnectorConfigResponse> getAll() {
        return connectorConfigService.getAll();
    }

    @GetMapping("/{id}")
    public ConnectorConfigResponse getById(@PathVariable Long id) {
        return connectorConfigService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectorConfigResponse create(@Valid @RequestBody ConnectorConfigRequest request) {
        return connectorConfigService.create(request);
    }

    @PutMapping("/{id}")
    public ConnectorConfigResponse update(@PathVariable Long id, @Valid @RequestBody ConnectorConfigRequest request) {
        return connectorConfigService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        connectorConfigService.delete(id);
    }
}
