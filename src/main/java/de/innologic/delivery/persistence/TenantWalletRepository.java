package de.innologic.delivery.persistence;

import de.innologic.delivery.domain.TenantWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantWalletRepository extends JpaRepository<TenantWalletEntity, String> {

    Optional<TenantWalletEntity> findByCompanyId(String companyId);

    long countByCompanyId(String companyId);
}
