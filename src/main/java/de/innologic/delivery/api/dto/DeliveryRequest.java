package de.innologic.delivery.api.dto;

import de.innologic.delivery.api.dto.MetaDto;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeliveryRequest(
        @NotBlank @Size(max = 100) String attemptId,
        @NotNull Channel channel,
        @NotBlank @Size(max = 2000) String to,
        @Size(max = 100) String provider,
        @Size(max = 255) String from,
        DeliveryMode deliveryMode,
        @Size(max = 255) String subject,
        @NotNull @Valid DeliveryContentDto content,
        @Valid List<AttachmentDto> attachments,
        @Valid MetaDto meta,
        @Size(max = 100) String correlationId
) {
}
