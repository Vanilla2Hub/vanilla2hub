package com.vanilla2hub.app.entity;

import com.vanilla2hub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logical_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogicalApp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_app_id")
    private LogicalApp parent;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String owner;

    @Column(nullable = false, length = 50)
    private String statusCode;

    @Column(length = 50)
    private String appTypeCode;

    @Column(columnDefinition = "JSON")
    private String extra;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public LogicalApp(LogicalApp parent, String name, String description, String owner,
                      String statusCode, String appTypeCode, String extra) {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.statusCode = statusCode;
        this.appTypeCode = appTypeCode;
        this.extra = extra;
        this.deleted = false;
    }

    public void update(LogicalApp parent, String name, String description, String owner,
                       String statusCode, String appTypeCode, String extra) {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.statusCode = statusCode;
        this.appTypeCode = appTypeCode;
        this.extra = extra;
    }

    public void delete() {
        this.deleted = true;
    }
}
