package de.innologic.delivery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "tenant_wallet")
public class TenantWalletEntity {

    @Id
    @Column(name = "company_id", length = 100)
    private String companyId;

    @Column(nullable = false)
    private long balance;

    @Column(name = "updated_at_utc", nullable = false)
    private OffsetDateTime updatedAtUtc;

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public OffsetDateTime getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    @PrePersist
    void onCreate() {
        updatedAtUtc = nowUtcMicros();
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
