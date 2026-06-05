package com.vanilla2hub.connector.dto;

import com.vanilla2hub.connector.entity.ConnectorConfig;

import java.time.Instant;

public record ConnectorConfigResponse(
        Long id,
        String name,
        String baseUrl,
        String authType,
        String vaultSecretPath,
        int timeoutMs,
        int retryCount,
        boolean enabled,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConnectorConfigResponse from(ConnectorConfig config) {
        return new ConnectorConfigResponse(
                config.getId(),
                config.getName(),
                config.getBaseUrl(),
                config.getAuthType(),
                config.getVaultSecretPath(),
                config.getTimeoutMs(),
                config.getRetryCount(),
                config.isEnabled(),
                config.getCreatedBy(),
                config.getUpdatedBy(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
