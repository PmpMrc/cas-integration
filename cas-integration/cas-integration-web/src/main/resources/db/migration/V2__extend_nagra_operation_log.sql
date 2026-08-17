ALTER TABLE nagra_operation_log
    ADD COLUMN http_method VARCHAR(10),
    ADD COLUMN endpoint VARCHAR(500),
    ADD COLUMN duration_ms BIGINT;