CREATE TABLE code_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_code_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE code (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code_type_id BIGINT       NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    extra        JSON,
    sort_order   INT          NOT NULL DEFAULT 0,
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_code_type_id_code (code_type_id, code),
    CONSTRAINT fk_code_code_type FOREIGN KEY (code_type_id) REFERENCES code_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
