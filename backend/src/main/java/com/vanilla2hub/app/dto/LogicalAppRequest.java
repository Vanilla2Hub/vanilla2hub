package com.vanilla2hub.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogicalAppRequest(
        Long parentAppId,

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @Size(max = 100)
        String owner,

        @NotBlank @Size(max = 50)
        String statusCode,

        @Size(max = 50)
        String appTypeCode,

        String extra
) {}
