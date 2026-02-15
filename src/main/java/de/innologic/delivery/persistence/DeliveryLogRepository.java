package de.innologic.delivery.persistence;

import de.innologic.delivery.domain.DeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLogEntity, Long> {

    Optional<DeliveryLogEntity> findByCompanyIdAndAttemptId(String companyId, String attemptId);

    long countByCompanyIdAndAttemptId(String companyId, String attemptId);
}
