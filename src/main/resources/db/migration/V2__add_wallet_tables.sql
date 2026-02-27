-- V2__add_wallet_tables.sql
-- Tenant wallet + ledger for prepaid credits
-- MariaDB 10.4 compatible

CREATE TABLE tenant_wallet (
                    company_id VARCHAR(100) NOT NULL,
                    balance BIGINT NOT NULL DEFAULT 0,
                    updated_at_utc DATETIME(6) NOT NULL,
                    PRIMARY KEY (company_id)
) ENGINE=InnoDB;

CREATE TABLE wallet_ledger (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    company_id VARCHAR(100) NOT NULL,
                    attempt_id VARCHAR(100) NULL,
                    type VARCHAR(20) NOT NULL,
                    amount BIGINT NOT NULL,
                    created_at_utc DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_wallet_ledger_company_attempt
    ON wallet_ledger (company_id, attempt_id);
