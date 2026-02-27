package de.innologic.delivery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "delivery_event")
public class DeliveryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, length = 100)
    private String companyId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "attempt_id", nullable = false, length = 100)
    private String attemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private DeliveryEventType eventType;

    @Column(name = "event_at_utc", nullable = false)
    private OffsetDateTime eventAtUtc;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "raw_status", length = 255)
    private String rawStatus;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at_utc", nullable = false)
    private OffsetDateTime createdAtUtc;

    public Long getId() {
        return id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public DeliveryEventType getEventType() {
        return eventType;
    }

    public void setEventType(DeliveryEventType eventType) {
        this.eventType = eventType;
    }

    public OffsetDateTime getEventAtUtc() {
        return eventAtUtc;
    }

    public void setEventAtUtc(OffsetDateTime eventAtUtc) {
        this.eventAtUtc = eventAtUtc;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAtUtc() {
        return createdAtUtc;
    }

    @PrePersist
    void onCreate() {
        if (createdAtUtc == null) {
            createdAtUtc = nowUtcMicros();
        }
    }

    private OffsetDateTime nowUtcMicros() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int micros = now.getNano() / 1_000;
        return now.withNano(micros * 1_000);
    }
}
