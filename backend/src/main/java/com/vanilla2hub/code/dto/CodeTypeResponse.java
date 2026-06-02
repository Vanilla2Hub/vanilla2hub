package com.vanilla2hub.code.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilla2hub.code.entity.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public record CodeTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        List<AttributeField> attributeSchema,
        boolean systemDefault,
        int sortOrder,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    private static final Logger log = LoggerFactory.getLogger(CodeTypeResponse.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<AttributeField>> SCHEMA_TYPE = new TypeReference<>() {};

    public static CodeTypeResponse from(CodeType codeType) {
        List<AttributeField> schema = List.of();
        String json = codeType.getAttributeSchema();
        if (json != null && !json.isBlank()) {
            try {
                schema = MAPPER.readValue(json, SCHEMA_TYPE);
            } catch (Exception e) {
                log.warn("Failed to parse attribute_schema for codeType {}: {}", codeType.getId(), e.getMessage());
            }
        }
        return new CodeTypeResponse(
                codeType.getId(),
                codeType.getCode(),
                codeType.getName(),
                codeType.getDescription(),
                schema,
                codeType.isSystemDefault(),
                codeType.getSortOrder(),
                codeType.getCreatedBy(),
                codeType.getUpdatedBy(),
                codeType.getCreatedAt(),
                codeType.getUpdatedAt()
        );
    }
}
