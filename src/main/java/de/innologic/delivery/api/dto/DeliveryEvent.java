package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryEventType;

import java.time.OffsetDateTime;

public record DeliveryEvent(
        String attemptId,
        Channel channel,
        DeliveryEventType eventType,
        OffsetDateTime eventAtUtc,
        String providerMessageId,
        String rawStatus,
        String errorCode,
        String errorMessage
) {
}
