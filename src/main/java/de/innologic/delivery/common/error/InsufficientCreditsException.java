package de.innologic.delivery.common.error;

public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException() {
        super("INSUFFICIENT_CREDITS");
    }
}
