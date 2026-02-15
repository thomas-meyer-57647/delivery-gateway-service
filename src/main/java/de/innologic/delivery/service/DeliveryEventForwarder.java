package de.innologic.delivery.service;

import de.innologic.delivery.api.dto.DeliveryEvent;

public interface DeliveryEventForwarder {

    void forward(DeliveryEvent event);
}
