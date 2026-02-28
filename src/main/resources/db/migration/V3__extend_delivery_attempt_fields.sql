-- V3__extend_delivery_attempt_fields.sql
-- Align schema with JPA entity (longer to_address, metadata, idempotency)

ALTER TABLE delivery_attempt
    MODIFY to_address VARCHAR(2000) NOT NULL,
    ADD COLUMN provider VARCHAR(100) NULL,
    ADD COLUMN from_value VARCHAR(255) NULL,
    ADD COLUMN meta_json TEXT NULL,
    ADD COLUMN idempotency_key VARCHAR(100) NULL;

CREATE UNIQUE INDEX uidx_delivery_attempt_company_idempotency
    ON delivery_attempt (company_id, idempotency_key);
