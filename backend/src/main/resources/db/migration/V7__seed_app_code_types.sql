INSERT INTO code_type (code, name, description, attribute_schema, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES ('APP_STATUS', '앱 상태', '논리적 앱 상태 코드', NULL, 1, 0, 0, 'system', 'system', NOW(), NOW());

SET @app_status_id = LAST_INSERT_ID();

INSERT INTO code (code_type_id, code, name, description, extra, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (@app_status_id, 'ACTIVE',     '활성',    NULL, NULL, 1, 0, 0, 'system', 'system', NOW(), NOW()),
    (@app_status_id, 'INACTIVE',   '비활성',  NULL, NULL, 1, 1, 0, 'system', 'system', NOW(), NOW()),
    (@app_status_id, 'DEPRECATED', '지원종료', NULL, NULL, 1, 2, 0, 'system', 'system', NOW(), NOW());

INSERT INTO code_type (code, name, description, attribute_schema, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES ('APP_TYPE', '앱 유형', '논리적 앱 유형 코드', NULL, 1, 1, 0, 'system', 'system', NOW(), NOW());

SET @app_type_id = LAST_INSERT_ID();

INSERT INTO code (code_type_id, code, name, description, extra, system_default, sort_order, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (@app_type_id, 'WEB',    'Web App',      NULL, NULL, 1, 0, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'API',    'API Service',  NULL, NULL, 1, 1, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'BATCH',  'Batch Job',    NULL, NULL, 1, 2, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'DAEMON', 'Daemon',       NULL, NULL, 1, 3, 0, 'system', 'system', NOW(), NOW()),
    (@app_type_id, 'MOBILE', 'Mobile App',   NULL, NULL, 1, 4, 0, 'system', 'system', NOW(), NOW());
