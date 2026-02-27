package de.innologic.delivery.persistence;

import de.innologic.delivery.domain.DeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttemptEntity, Long> {

    Optional<DeliveryAttemptEntity> findByCompanyIdAndAttemptId(String companyId, String attemptId);

    Optional<DeliveryAttemptEntity> findByCompanyIdAndProviderMessageId(String companyId, String providerMessageId);

    long countByCompanyIdAndAttemptId(String companyId, String attemptId);
}
