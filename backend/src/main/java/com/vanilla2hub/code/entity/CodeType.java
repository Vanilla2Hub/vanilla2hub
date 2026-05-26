package com.vanilla2hub.code.entity;

import com.vanilla2hub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "code_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public CodeType(String code, String name, String description, int sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.deleted = false;
    }

    public void update(String name, String description, int sortOrder) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public void delete() {
        this.deleted = true;
    }
}
