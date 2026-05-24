CREATE TABLE audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    actor        VARCHAR(200) NOT NULL,
    action       VARCHAR(100) NOT NULL,
    resource     VARCHAR(200) NOT NULL,
    resource_id  VARCHAR(100),
    detail       JSON,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_log_actor (actor),
    INDEX idx_audit_log_resource (resource, resource_id),
    INDEX idx_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
