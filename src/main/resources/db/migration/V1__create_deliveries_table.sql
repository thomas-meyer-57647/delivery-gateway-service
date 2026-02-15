-- V1__create_deliveries_table.sql
-- Schema for DeliveryLogEntity + DeliveryEventEntity
-- MariaDB 10.4 compatible

CREATE TABLE delivery_log (
                              id BIGINT NOT NULL AUTO_INCREMENT,

                              company_id VARCHAR(100) NOT NULL,
                              attempt_id VARCHAR(100) NOT NULL,

                              channel VARCHAR(20) NOT NULL,

                              to_address VARCHAR(255) NOT NULL,
                              subject VARCHAR(255) NULL,

                              content_text TEXT NOT NULL,
                              content_html TEXT NULL,
                              attachments_json TEXT NULL,

                              request_correlation_id VARCHAR(100) NULL,

                              state VARCHAR(20) NOT NULL,

                              provider_message_id VARCHAR(255) NULL,

                              error_code VARCHAR(100) NULL,
                              error_message TEXT NULL,

                              created_at_utc DATETIME(6) NOT NULL,
                              updated_at_utc DATETIME(6) NOT NULL,

                              PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_delivery_log_company_attempt
    ON delivery_log (company_id, attempt_id);

CREATE INDEX idx_delivery_log_state
    ON delivery_log (state);

CREATE INDEX idx_delivery_log_channel
    ON delivery_log (channel);

CREATE INDEX idx_delivery_log_created_at
    ON delivery_log (created_at_utc);


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

CREATE INDEX idx_delivery_event_company_attempt
    ON delivery_event (company_id, attempt_id);

CREATE INDEX idx_delivery_event_provider
    ON delivery_event (provider);

CREATE INDEX idx_delivery_event_channel
    ON delivery_event (channel);

CREATE INDEX idx_delivery_event_type
    ON delivery_event (event_type);

CREATE INDEX idx_delivery_event_event_at
    ON delivery_event (event_at_utc);

CREATE INDEX idx_delivery_event_created_at
    ON delivery_event (created_at_utc);

