package de.innologic.delivery.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryContentDto(
        @NotBlank @Size(max = 4000) String text,
        @Size(max = 16000) String html
) {
}
