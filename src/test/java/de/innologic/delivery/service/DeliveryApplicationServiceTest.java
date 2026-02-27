package de.innologic.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.api.dto.DeliveryContentDto;
import de.innologic.delivery.api.dto.DeliveryRequest;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryAttemptEntity;
import de.innologic.delivery.persistence.DeliveryAttemptRepository;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceTest {

    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock
    private DeliveryEventRepository deliveryEventRepository;
    @Mock
    private DeliveryEventForwarder deliveryEventForwarder;

    private DeliveryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryApplicationService(
                deliveryAttemptRepository,
                deliveryEventRepository,
                deliveryEventForwarder,
                new ObjectMapper()
        );
        when(deliveryAttemptRepository.findByCompanyIdAndAttemptId(any(), any())).thenReturn(java.util.Optional.empty());
        when(deliveryAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldNormalizeRecipientListForResponse() {
        DeliveryRequest request = new DeliveryRequest(
                "attempt-1",
                Channel.EMAIL,
                "foo@example.com;bar@example.com;foo@example.com",
                null,
                "Subject",
                new DeliveryContentDto("text", null),
                null,
                null
        );

        var response = service.createDelivery("company-a", request, "corr-id");

        assertThat(response.to()).containsExactly("foo@example.com", "bar@example.com");
    }

    @Test
    void shouldIgnoreSubjectForNonEmailChannels() {
        DeliveryRequest request = new DeliveryRequest(
                "attempt-2",
                Channel.SMS,
                "+491700000000",
                null,
                "should be ignored",
                new DeliveryContentDto("text", null),
                null,
                null
        );

        service.createDelivery("company-a", request, null);

        ArgumentCaptor<DeliveryAttemptEntity> attemptCaptor = ArgumentCaptor.forClass(DeliveryAttemptEntity.class);
        verify(deliveryAttemptRepository).save(attemptCaptor.capture());

        DeliveryAttemptEntity captured = attemptCaptor.getValue();
        assertThat(captured.getChannel()).isEqualTo(Channel.SMS);
        assertThat(captured.getSubject()).isNull();
    }

    @Test
    void shouldNotSerializeAttachmentsWhenEmpty() {
        DeliveryRequest request = new DeliveryRequest(
                "attempt-3",
                Channel.EMAIL,
                "user@example.com",
                null,
                null,
                new DeliveryContentDto("text", null),
                Collections.emptyList(),
                null
        );

        service.createDelivery("company-a", request, null);

        ArgumentCaptor<DeliveryAttemptEntity> attemptCaptor = ArgumentCaptor.forClass(DeliveryAttemptEntity.class);
        verify(deliveryAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getAttachmentsJson()).isNull();
    }
}
