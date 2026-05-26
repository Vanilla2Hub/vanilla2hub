package com.vanilla2hub.code.entity;

import com.vanilla2hub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Code extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_type_id", nullable = false)
    private CodeType codeType;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "JSON")
    private String extra;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public Code(CodeType codeType, String code, String name, String description, String extra, int sortOrder) {
        this.codeType = codeType;
        this.code = code;
        this.name = name;
        this.description = description;
        this.extra = extra;
        this.sortOrder = sortOrder;
        this.deleted = false;
    }

    public void update(String name, String description, String extra, int sortOrder) {
        this.name = name;
        this.description = description;
        this.extra = extra;
        this.sortOrder = sortOrder;
    }

    public void delete() {
        this.deleted = true;
    }
}
