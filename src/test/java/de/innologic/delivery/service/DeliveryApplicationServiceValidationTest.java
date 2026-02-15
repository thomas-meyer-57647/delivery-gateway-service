package de.innologic.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.api.dto.DeliveryContentDto;
import de.innologic.delivery.api.dto.DeliveryRequest;
import de.innologic.delivery.common.error.BadRequestException;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import de.innologic.delivery.persistence.DeliveryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceValidationTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;
    @Mock
    private DeliveryEventRepository deliveryEventRepository;
    @Mock
    private DeliveryEventForwarder deliveryEventForwarder;

    private DeliveryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryApplicationService(
                deliveryLogRepository,
                deliveryEventRepository,
                deliveryEventForwarder,
                new ObjectMapper()
        );
    }

    @Test
    void shouldRejectSmsWithSubject() {
        DeliveryRequest request = new DeliveryRequest(
                "attempt-1",
                Channel.SMS,
                "+491701234567",
                "This must fail",
                new DeliveryContentDto("text", null),
                null,
                "corr-123"
        );

        assertThatThrownBy(() -> service.createDelivery("company-a", request, "corr-header"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SMS channel must not contain subject");

        verifyNoInteractions(deliveryLogRepository, deliveryEventRepository, deliveryEventForwarder);
    }
}
