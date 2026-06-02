ALTER TABLE code_type
    ADD COLUMN attribute_schema JSON       NULL          AFTER description,
    ADD COLUMN system_default   TINYINT(1) NOT NULL DEFAULT 0 AFTER attribute_schema;

ALTER TABLE code
    ADD COLUMN system_default   TINYINT(1) NOT NULL DEFAULT 0 AFTER extra;
