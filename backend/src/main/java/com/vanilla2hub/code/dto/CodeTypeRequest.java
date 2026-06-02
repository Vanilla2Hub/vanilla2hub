package com.vanilla2hub.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CodeTypeRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        List<AttributeField> attributeSchema,
        int sortOrder
) {}
