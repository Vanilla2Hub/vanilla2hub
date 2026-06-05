package com.vanilla2hub.connector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnectorConfigRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotBlank @Size(max = 500)
        String baseUrl,

        @NotBlank @Size(max = 50)
        String authType,

        @NotBlank @Size(max = 500)
        String vaultSecretPath,

        @Min(1000) @Max(300000)
        int timeoutMs,

        @Min(0) @Max(10)
        int retryCount,

        boolean enabled
) {}
