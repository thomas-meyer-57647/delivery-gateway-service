package de.innologic.delivery.api.dto;

import de.innologic.delivery.domain.Channel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeliveryRequest(
        @NotBlank @Size(max = 100) String attemptId,
        @NotNull Channel channel,
        @NotBlank @Size(max = 255) String to,
        @Size(max = 255) String subject,
        @NotNull @Valid DeliveryContentDto content,
        @Valid List<AttachmentDto> attachments,
        @Size(max = 100) String correlationId
) {
}
