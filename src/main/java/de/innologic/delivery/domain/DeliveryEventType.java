package de.innologic.delivery.domain;

public enum DeliveryEventType {
    QUEUED,
    PROCESSING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELED,
    BOUNCED,
    REJECTED,
    UNDELIVERABLE,
    READ;

    public static DeliveryEventType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        String normalized = value.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        for (DeliveryEventType eventType : values()) {
            if (eventType.name().equals(normalized)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Unsupported eventType: " + value);
    }
}
