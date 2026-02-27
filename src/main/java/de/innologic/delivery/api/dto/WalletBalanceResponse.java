package de.innologic.delivery.api.dto;

public record WalletBalanceResponse(
        String companyId,
        long balance
) {
}
