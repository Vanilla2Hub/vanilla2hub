CREATE TABLE connector_config (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    base_url         VARCHAR(500) NOT NULL,
    auth_type        VARCHAR(50)  NOT NULL,
    vault_secret_path VARCHAR(500) NOT NULL,
    timeout_ms       INT          NOT NULL DEFAULT 30000,
    retry_count      INT          NOT NULL DEFAULT 3,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    deleted          TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_connector_config_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
