package de.innologic.delivery.common.error;

public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(String attemptId) {
        super("Delivery attempt not found: " + attemptId);
    }
}
