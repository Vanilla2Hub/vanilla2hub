package com.vanilla2hub.app.dto;

import com.vanilla2hub.app.entity.LogicalApp;

import java.time.Instant;

public record LogicalAppResponse(
        Long id,
        Long parentAppId,
        String parentAppName,
        String name,
        String description,
        String owner,
        String statusCode,
        String appTypeCode,
        String extra,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static LogicalAppResponse from(LogicalApp app) {
        return new LogicalAppResponse(
                app.getId(),
                app.getParent() != null ? app.getParent().getId() : null,
                app.getParent() != null ? app.getParent().getName() : null,
                app.getName(),
                app.getDescription(),
                app.getOwner(),
                app.getStatusCode(),
                app.getAppTypeCode(),
                app.getExtra(),
                app.getCreatedBy(),
                app.getUpdatedBy(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
