package com.vanilla2hub.connector.entity;

import com.vanilla2hub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "connector_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConnectorConfig extends BaseEntity {

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 50)
    private String authType;

    @Column(nullable = false, length = 500)
    private String vaultSecretPath;

    @Column(nullable = false)
    private int timeoutMs;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public ConnectorConfig(String name, String baseUrl, String authType, String vaultSecretPath,
                           int timeoutMs, int retryCount, boolean enabled) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.authType = authType;
        this.vaultSecretPath = vaultSecretPath;
        this.timeoutMs = timeoutMs;
        this.retryCount = retryCount;
        this.enabled = enabled;
        this.deleted = false;
    }

    public void update(String name, String baseUrl, String authType, String vaultSecretPath,
                       int timeoutMs, int retryCount, boolean enabled) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.authType = authType;
        this.vaultSecretPath = vaultSecretPath;
        this.timeoutMs = timeoutMs;
        this.retryCount = retryCount;
        this.enabled = enabled;
    }

    public void delete() {
        this.deleted = true;
    }
}
