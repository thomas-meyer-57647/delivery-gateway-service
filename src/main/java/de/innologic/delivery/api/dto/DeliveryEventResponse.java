package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryEventType;

import java.time.OffsetDateTime;

public record DeliveryEventResponse(
        Channel channel,
        String provider,
        DeliveryEventType eventType,
        OffsetDateTime eventAtUtc,
        String providerMessageId,
        String rawStatus,
        String errorCode,
        String errorMessage
) {
}
