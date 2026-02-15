package de.innologic.delivery.service;

import de.innologic.delivery.api.dto.DeliveryEvent;
import org.springframework.stereotype.Component;

@Component
public class NoOpDeliveryEventForwarder implements DeliveryEventForwarder {

    @Override
    public void forward(DeliveryEvent event) {
        // v1 intentionally does not forward events to external services.
    }
}
