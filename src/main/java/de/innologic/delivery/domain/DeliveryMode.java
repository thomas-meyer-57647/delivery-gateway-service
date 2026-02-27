package de.innologic.delivery.domain;

public enum DeliveryMode {
    SINGLE,
    INDIVIDUAL;

    public static DeliveryMode resolve(DeliveryMode mode) {
        return mode == null ? SINGLE : mode;
    }
}
