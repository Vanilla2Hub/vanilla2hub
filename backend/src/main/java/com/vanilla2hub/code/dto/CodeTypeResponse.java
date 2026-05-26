package com.vanilla2hub.code.dto;

import com.vanilla2hub.code.entity.CodeType;

import java.time.Instant;

public record CodeTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        int sortOrder,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static CodeTypeResponse from(CodeType codeType) {
        return new CodeTypeResponse(
                codeType.getId(),
                codeType.getCode(),
                codeType.getName(),
                codeType.getDescription(),
                codeType.getSortOrder(),
                codeType.getCreatedBy(),
                codeType.getUpdatedBy(),
                codeType.getCreatedAt(),
                codeType.getUpdatedAt()
        );
    }
}
