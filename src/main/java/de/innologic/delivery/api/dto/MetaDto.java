package de.innologic.delivery.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record MetaDto(
        @Size(max = 255) String clientRef,
        List<@Size(max = 50) String> tags
) {
}
