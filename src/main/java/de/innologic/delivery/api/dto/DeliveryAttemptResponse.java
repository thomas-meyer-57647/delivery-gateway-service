package de.innologic.delivery.api.dto;

import de.innologic.delivery.api.dto.MetaDto;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryMode;
import de.innologic.delivery.domain.DeliveryStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record DeliveryAttemptResponse(
        String attemptId,
        Channel channel,
        DeliveryMode deliveryMode,
        List<String> to,
        String provider,
        String from,
        MetaDto meta,
        String subject,
        DeliveryStatus state,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        String correlationId,
        OffsetDateTime createdAtUtc,
        OffsetDateTime updatedAtUtc,
        List<DeliveryEventResponse> events
) {
}
