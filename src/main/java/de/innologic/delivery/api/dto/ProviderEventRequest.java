package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ProviderEventRequest(
        @Size(max = 100) String attemptId,
        @Size(max = 255) String providerMessageId,
        @NotNull Channel channel,
        @NotBlank String eventType,
        OffsetDateTime eventAtUtc,
        @Size(max = 255) String rawStatus,
        @Size(max = 100) String errorCode,
        @Size(max = 2000) String errorMessage
) {
}
