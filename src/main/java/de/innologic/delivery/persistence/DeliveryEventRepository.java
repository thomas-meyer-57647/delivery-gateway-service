package de.innologic.delivery.persistence;

import de.innologic.delivery.domain.DeliveryEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEventEntity, Long> {

    long countByCompanyIdAndAttemptId(String companyId, String attemptId);

    List<DeliveryEventEntity> findAllByCompanyIdAndAttemptIdOrderByEventAtUtcAsc(String companyId, String attemptId);
}
