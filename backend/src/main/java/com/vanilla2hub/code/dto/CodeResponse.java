package com.vanilla2hub.code.dto;

import com.vanilla2hub.code.entity.Code;

import java.time.Instant;

public record CodeResponse(
        Long id,
        Long codeTypeId,
        String code,
        String name,
        String description,
        String extra,
        int sortOrder,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static CodeResponse from(Code code) {
        return new CodeResponse(
                code.getId(),
                code.getCodeType().getId(),
                code.getCode(),
                code.getName(),
                code.getDescription(),
                code.getExtra(),
                code.getSortOrder(),
                code.getCreatedBy(),
                code.getUpdatedBy(),
                code.getCreatedAt(),
                code.getUpdatedAt()
        );
    }
}
