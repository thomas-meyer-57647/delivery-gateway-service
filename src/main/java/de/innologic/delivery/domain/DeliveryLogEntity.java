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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "delivery_log")
public class DeliveryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, length = 100)
    private String companyId;

    @Column(name = "attempt_id", nullable = false, length = 100)
    private String attemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(name = "to_address", nullable = false, length = 255)
    private String toAddress;

    @Column(length = 255)
    private String subject;

    @Lob
    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Lob
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Lob
    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "request_correlation_id", length = 100)
    private String requestCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryState state;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at_utc", nullable = false)
    private OffsetDateTime createdAtUtc;

    @Column(name = "updated_at_utc", nullable = false)
    private OffsetDateTime updatedAtUtc;

    public Long getId() {
        return id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
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

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }

    public String getRequestCorrelationId() {
        return requestCorrelationId;
    }

    public void setRequestCorrelationId(String requestCorrelationId) {
        this.requestCorrelationId = requestCorrelationId;
    }

    public DeliveryState getState() {
        return state;
    }

    public void setState(DeliveryState state) {
        this.state = state;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
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

    public OffsetDateTime getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = nowUtcMicros();
        if (state == null) {
            state = DeliveryState.UNKNOWN;
        }
        createdAtUtc = now;
        updatedAtUtc = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAtUtc = nowUtcMicros();
    }

    private OffsetDateTime nowUtcMicros() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int micros = now.getNano() / 1_000;
        return now.withNano(micros * 1_000);
    }
}
