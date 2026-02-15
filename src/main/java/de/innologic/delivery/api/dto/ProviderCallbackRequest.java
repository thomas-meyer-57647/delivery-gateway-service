package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ProviderCallbackRequest(
        @NotBlank @Size(max = 100) String attemptId,
        @NotNull Channel channel,
        @NotNull DeliveryEventType eventType,
        @NotNull OffsetDateTime eventAtUtc,
        @Size(max = 255) String providerMessageId,
        @Size(max = 255) String rawStatus,
        @Size(max = 100) String errorCode,
        @Size(max = 2000) String errorMessage
) {
}
