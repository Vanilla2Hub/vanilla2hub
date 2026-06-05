CREATE TABLE logical_app (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    parent_app_id  BIGINT,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    owner          VARCHAR(100),
    status_code    VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    app_type_code  VARCHAR(50),
    extra          JSON,
    deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_logical_app_parent FOREIGN KEY (parent_app_id) REFERENCES logical_app (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
