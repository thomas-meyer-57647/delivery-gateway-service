package de.innologic.delivery.service;

import de.innologic.delivery.api.dto.WalletBalanceResponse;
import de.innologic.delivery.common.error.BadRequestException;
import de.innologic.delivery.common.error.InsufficientCreditsException;
import de.innologic.delivery.domain.TenantWalletEntity;
import de.innologic.delivery.domain.WalletLedgerEntity;
import de.innologic.delivery.domain.WalletLedgerType;
import de.innologic.delivery.persistence.TenantWalletRepository;
import de.innologic.delivery.persistence.WalletLedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class WalletService {

    private final TenantWalletRepository tenantWalletRepository;
    private final WalletLedgerRepository walletLedgerRepository;

    public WalletService(TenantWalletRepository tenantWalletRepository,
                         WalletLedgerRepository walletLedgerRepository) {
        this.tenantWalletRepository = tenantWalletRepository;
        this.walletLedgerRepository = walletLedgerRepository;
    }

    public WalletBalanceResponse getBalance(String companyId) {
        long balance = tenantWalletRepository.findByCompanyId(companyId)
                .map(TenantWalletEntity::getBalance)
                .orElse(0L);
        return new WalletBalanceResponse(companyId, balance);
    }

    public WalletBalanceResponse topup(String companyId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException("amount must be greater than zero");
        }
        TenantWalletEntity wallet = tenantWalletRepository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    TenantWalletEntity entity = new TenantWalletEntity();
                    entity.setCompanyId(companyId);
                    return entity;
                });
        wallet.setBalance(wallet.getBalance() + amount);
        tenantWalletRepository.save(wallet);
        WalletLedgerEntity ledger = new WalletLedgerEntity();
        ledger.setCompanyId(companyId);
        ledger.setType(WalletLedgerType.TOPUP);
        ledger.setAmount(amount);
        walletLedgerRepository.save(ledger);
        return new WalletBalanceResponse(companyId, wallet.getBalance());
    }

    public void charge(String companyId, String attemptId, long amount) {
        if (amount <= 0) {
            return;
        }
        TenantWalletEntity wallet = tenantWalletRepository.findByCompanyId(companyId)
                .orElseThrow(InsufficientCreditsException::new);
        if (wallet.getBalance() < amount) {
            throw new InsufficientCreditsException();
        }
        wallet.setBalance(wallet.getBalance() - amount);
        tenantWalletRepository.save(wallet);
        WalletLedgerEntity ledger = new WalletLedgerEntity();
        ledger.setCompanyId(companyId);
        ledger.setAttemptId(attemptId);
        ledger.setType(WalletLedgerType.DEBIT);
        ledger.setAmount(amount);
        walletLedgerRepository.save(ledger);
    }
}
