package de.innologic.delivery.domain;

public enum DeliveryStatus {
    QUEUED,
    PROCESSING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELED;

    public static DeliveryStatus forEvent(DeliveryEventType eventType) {
        if (eventType == null) {
            return QUEUED;
        }
        return switch (eventType) {
            case QUEUED -> QUEUED;
            case PROCESSING -> PROCESSING;
            case SENT -> SENT;
            case DELIVERED, READ -> DELIVERED;
            case FAILED, BOUNCED, REJECTED, UNDELIVERABLE -> FAILED;
            case CANCELED -> CANCELED;
        };
    }

    public boolean isFinal() {
        return this == DELIVERED || this == FAILED || this == CANCELED;
    }
}
