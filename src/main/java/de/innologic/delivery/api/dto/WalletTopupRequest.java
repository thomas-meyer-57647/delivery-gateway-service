package de.innologic.delivery.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WalletTopupRequest(
        @NotNull
        @Positive
        Long amount
) {
}
