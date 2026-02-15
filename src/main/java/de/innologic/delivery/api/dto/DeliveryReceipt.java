package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.DeliveryState;

import java.time.OffsetDateTime;

public record DeliveryReceipt(
        String attemptId,
        DeliveryState state,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        OffsetDateTime createdAtUtc
) {
}
