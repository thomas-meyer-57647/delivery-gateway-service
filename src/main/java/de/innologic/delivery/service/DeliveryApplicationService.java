package de.innologic.delivery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.api.dto.DeliveryEvent;
import de.innologic.delivery.api.dto.DeliveryRequest;
import de.innologic.delivery.api.dto.DeliveryReceipt;
import de.innologic.delivery.api.dto.ProviderCallbackRequest;
import de.innologic.delivery.common.error.BadRequestException;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryEventEntity;
import de.innologic.delivery.domain.DeliveryEventType;
import de.innologic.delivery.domain.DeliveryLogEntity;
import de.innologic.delivery.domain.DeliveryState;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import de.innologic.delivery.persistence.DeliveryLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DeliveryApplicationService {

    private final DeliveryLogRepository deliveryLogRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final DeliveryEventForwarder deliveryEventForwarder;
    private final ObjectMapper objectMapper;

    public DeliveryApplicationService(DeliveryLogRepository deliveryLogRepository,
                                      DeliveryEventRepository deliveryEventRepository,
                                      DeliveryEventForwarder deliveryEventForwarder,
                                      ObjectMapper objectMapper) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.deliveryEventRepository = deliveryEventRepository;
        this.deliveryEventForwarder = deliveryEventForwarder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeliveryReceipt createDelivery(String companyId, DeliveryRequest request, String headerCorrelationId) {
        validateRequest(request);

        return deliveryLogRepository.findByCompanyIdAndAttemptId(companyId, request.attemptId())
                .map(this::toReceipt)
                .orElseGet(() -> createWithIdempotency(companyId, request, headerCorrelationId));
    }

    @Transactional
    public DeliveryEvent handleProviderCallback(String companyId, String provider, ProviderCallbackRequest request) {
        DeliveryEventEntity eventEntity = new DeliveryEventEntity();
        eventEntity.setCompanyId(companyId);
        eventEntity.setProvider(provider);
        eventEntity.setAttemptId(request.attemptId());
        eventEntity.setChannel(request.channel());
        eventEntity.setEventType(request.eventType());
        eventEntity.setEventAtUtc(request.eventAtUtc());
        eventEntity.setProviderMessageId(request.providerMessageId());
        eventEntity.setRawStatus(request.rawStatus());
        eventEntity.setErrorCode(request.errorCode());
        eventEntity.setErrorMessage(request.errorMessage());

        DeliveryEventEntity savedEvent = deliveryEventRepository.save(eventEntity);
        upsertDeliveryLogFromCallback(companyId, request);

        DeliveryEvent event = toEvent(savedEvent);
        deliveryEventForwarder.forward(event);
        return event;
    }

    private DeliveryReceipt createWithIdempotency(String companyId, DeliveryRequest request, String headerCorrelationId) {
        DeliveryLogEntity log = new DeliveryLogEntity();
        log.setCompanyId(companyId);
        log.setAttemptId(request.attemptId());
        log.setChannel(request.channel());
        log.setToAddress(request.to());
        log.setSubject(request.subject());
        log.setContentText(request.content().text());
        log.setContentHtml(request.content().html());
        log.setAttachmentsJson(serializeAttachments(request.attachments()));
        log.setRequestCorrelationId(resolveCorrelationId(request.correlationId(), headerCorrelationId));
        log.setState(DeliveryState.ACCEPTED);

        try {
            return toReceipt(deliveryLogRepository.save(log));
        } catch (DataIntegrityViolationException ex) {
            return deliveryLogRepository.findByCompanyIdAndAttemptId(companyId, request.attemptId())
                    .map(this::toReceipt)
                    .orElseThrow(() -> ex);
        }
    }

    private void upsertDeliveryLogFromCallback(String companyId, ProviderCallbackRequest callback) {
        DeliveryLogEntity log = deliveryLogRepository.findByCompanyIdAndAttemptId(companyId, callback.attemptId())
                .orElseGet(() -> createUnknownLog(companyId, callback));

        log.setChannel(callback.channel());
        if (StringUtils.hasText(callback.providerMessageId())) {
            log.setProviderMessageId(callback.providerMessageId());
        }
        if (StringUtils.hasText(callback.errorCode())) {
            log.setErrorCode(callback.errorCode());
        }
        if (StringUtils.hasText(callback.errorMessage())) {
            log.setErrorMessage(callback.errorMessage());
        }
        log.setState(mapState(callback.eventType()));

        deliveryLogRepository.save(log);
    }

    private DeliveryLogEntity createUnknownLog(String companyId, ProviderCallbackRequest callback) {
        DeliveryLogEntity log = new DeliveryLogEntity();
        log.setCompanyId(companyId);
        log.setAttemptId(callback.attemptId());
        log.setChannel(callback.channel());
        log.setToAddress("unknown");
        log.setContentText("callback-only");
        log.setState(DeliveryState.UNKNOWN);
        return log;
    }

    private DeliveryState mapState(DeliveryEventType eventType) {
        return switch (eventType) {
            case FAILED -> DeliveryState.REJECTED;
            case SENT, DELIVERED, READ -> DeliveryState.ACCEPTED;
        };
    }

    private void validateRequest(DeliveryRequest request) {
        if (request.channel() == Channel.SMS && StringUtils.hasText(request.subject())) {
            throw new BadRequestException("SMS channel must not contain subject");
        }
    }

    private String resolveCorrelationId(String requestCorrelationId, String headerCorrelationId) {
        if (StringUtils.hasText(requestCorrelationId)) {
            return requestCorrelationId;
        }
        return headerCorrelationId;
    }

    private String serializeAttachments(List<?> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("attachments cannot be serialized");
        }
    }

    private DeliveryReceipt toReceipt(DeliveryLogEntity log) {
        return new DeliveryReceipt(
                log.getAttemptId(),
                log.getState(),
                log.getProviderMessageId(),
                log.getErrorCode(),
                log.getErrorMessage(),
                log.getCreatedAtUtc()
        );
    }

    private DeliveryEvent toEvent(DeliveryEventEntity event) {
        return new DeliveryEvent(
                event.getAttemptId(),
                event.getChannel(),
                event.getEventType(),
                event.getEventAtUtc(),
                event.getProviderMessageId(),
                event.getRawStatus(),
                event.getErrorCode(),
                event.getErrorMessage()
        );
    }
}
