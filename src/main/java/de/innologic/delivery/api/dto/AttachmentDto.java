package de.innologic.delivery.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttachmentDto(
        @Size(max = 255) String fileId,
        @Size(max = 2048) String url,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String mimeType,
        @Min(0) long size
) {
}
