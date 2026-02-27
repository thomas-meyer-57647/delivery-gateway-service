-- V1__create_deliveries_table.sql
-- Schema for DeliveryAttempt + DeliveryEvent
-- MariaDB 10.4 compatible

CREATE TABLE delivery_attempt (
                              id BIGINT NOT NULL AUTO_INCREMENT,

                              company_id VARCHAR(100) NOT NULL,
                              attempt_id VARCHAR(100) NOT NULL,

                              channel VARCHAR(20) NOT NULL,
                              delivery_mode VARCHAR(20) NOT NULL,

                              to_address VARCHAR(255) NOT NULL,
                              subject VARCHAR(255) NULL,

                              content_text TEXT,
                              content_html TEXT,
                              attachments_json TEXT,

                              request_correlation_id VARCHAR(100) NULL,

                              state VARCHAR(20) NOT NULL,

                              provider_message_id VARCHAR(255) NULL,

                              error_code VARCHAR(100) NULL,
                              error_message TEXT NULL,

                              created_at_utc DATETIME(6) NOT NULL,
                              updated_at_utc DATETIME(6) NOT NULL,

                              PRIMARY KEY (id),
                              UNIQUE KEY uidx_company_attempt (company_id, attempt_id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_attempt_company_status
    ON delivery_attempt (company_id, state);

CREATE INDEX idx_delivery_attempt_provider_message_id
    ON delivery_attempt (provider_message_id);

CREATE TABLE delivery_event (
                                id BIGINT NOT NULL AUTO_INCREMENT,

                                company_id VARCHAR(100) NOT NULL,

                                provider VARCHAR(50) NOT NULL,

                                attempt_id VARCHAR(100) NOT NULL,

                                channel VARCHAR(20) NOT NULL,

                                event_type VARCHAR(20) NOT NULL,

                                event_at_utc DATETIME(6) NOT NULL,

                                provider_message_id VARCHAR(255) NULL,
                                raw_status VARCHAR(255) NULL,

                                error_code VARCHAR(100) NULL,
                                error_message TEXT NULL,

                                created_at_utc DATETIME(6) NOT NULL,

                                PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_event_attempt
    ON delivery_event (attempt_id, event_at_utc);

CREATE INDEX idx_delivery_event_provider_message_id
    ON delivery_event (provider_message_id);

CREATE INDEX idx_delivery_event_company_attempt
    ON delivery_event (company_id, attempt_id);
