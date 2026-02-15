package de.innologic.delivery.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(
        name = "ErrorResponse",
        description = "Standardisiertes Fehlerobjekt fuer API-Fehlerantworten."
)
public record ErrorResponse(
        @Schema(
                description = "Zeitpunkt des Fehlers in UTC (ISO-8601).",
                example = "2026-02-15T18:30:21Z"
        )
        OffsetDateTime timestampUtc,

        @Schema(
                description = "HTTP-Statuscode.",
                example = "400"
        )
        int status,

        @Schema(
                description = "HTTP-Statusbezeichnung.",
                example = "Bad Request"
        )
        String error,

        @Schema(
                description = "Fachliche oder technische Fehlermeldung.",
                example = "recipient: must not be blank"
        )
        String message,

        @Schema(
                description = "Request-Pfad, auf dem der Fehler auftrat.",
                example = "/api/deliveries"
        )
        String path,

        @Schema(
                description = "Korrelations-ID zur Nachverfolgung im Logging.",
                example = "f9f5c6ce-3ba8-4ec8-a9c0-68266b8025b8"
        )
        String correlationId
) {
}
