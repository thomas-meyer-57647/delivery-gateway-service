package de.innologic.delivery.persistence;

import de.innologic.delivery.domain.WalletLedgerEntity;
import de.innologic.delivery.domain.WalletLedgerType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntity, Long> {

    long countByCompanyIdAndAttemptIdAndType(String companyId, String attemptId, WalletLedgerType type);
}
