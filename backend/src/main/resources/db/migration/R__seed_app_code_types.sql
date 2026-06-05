-- code_type: APP_STATUS
INSERT INTO code_type (code, name, description, attribute_schema, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES ('APP_STATUS', 'App Status', 'Logical App Status Code', NULL, 1, -1, 0, 'system', 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), sort_order = VALUES(sort_order), updated_at = NOW();

SET @app_status_id = (SELECT id FROM code_type WHERE code = 'APP_STATUS');

INSERT INTO code (code_type_id, code, name, description, extra, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (@app_status_id, 'ACTIVE',     'Active',     NULL, NULL, 1, 1, 0, 'system', 'system', NOW(), NOW()),
    (@app_status_id, 'INACTIVE',   'Inactive',   NULL, NULL, 1, 2, 0, 'system', 'system', NOW(), NOW()),
    (@app_status_id, 'DEPRECATED', 'Deprecated', NULL, NULL, 1, 3, 0, 'system', 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order), updated_at = NOW();

-- code_type: APP_TYPE
INSERT INTO code_type (code, name, description, attribute_schema, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES ('APP_TYPE', 'App Type', 'Logical App Type Code', NULL, 1, -1, 0, 'system', 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), sort_order = VALUES(sort_order), updated_at = NOW();

SET @app_type_id = (SELECT id FROM code_type WHERE code = 'APP_TYPE');

INSERT INTO code (code_type_id, code, name, description, extra, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (@app_type_id, 'WEB',    'Web App',     NULL, NULL, 1, 1, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'API',    'API Service', NULL, NULL, 1, 2, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'BATCH',  'Batch Job',   NULL, NULL, 1, 3, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'DAEMON', 'Daemon',      NULL, NULL, 1, 4, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'MOBILE', 'Mobile App',  NULL, NULL, 1, 5, 0, 'system', 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order), updated_at = NOW();
