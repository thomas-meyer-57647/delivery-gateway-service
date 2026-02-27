package de.innologic.delivery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.api.dto.DeliveryAttemptResponse;
import de.innologic.delivery.api.dto.DeliveryEvent;
import de.innologic.delivery.api.dto.DeliveryEventResponse;
import de.innologic.delivery.api.dto.DeliveryReceipt;
import de.innologic.delivery.api.dto.DeliveryRequest;
import de.innologic.delivery.api.dto.ProviderEventRequest;
import de.innologic.delivery.common.error.BadRequestException;
import de.innologic.delivery.common.error.DeliveryNotFoundException;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryAttemptEntity;
import de.innologic.delivery.domain.DeliveryEventEntity;
import de.innologic.delivery.domain.DeliveryEventType;
import de.innologic.delivery.domain.DeliveryMode;
import de.innologic.delivery.domain.DeliveryStatus;
import de.innologic.delivery.persistence.DeliveryAttemptRepository;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeliveryApplicationService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final DeliveryEventForwarder deliveryEventForwarder;
    private final ObjectMapper objectMapper;

    public DeliveryApplicationService(DeliveryAttemptRepository deliveryAttemptRepository,
                                      DeliveryEventRepository deliveryEventRepository,
                                      DeliveryEventForwarder deliveryEventForwarder,
                                      ObjectMapper objectMapper) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.deliveryEventRepository = deliveryEventRepository;
        this.deliveryEventForwarder = deliveryEventForwarder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeliveryReceipt createDelivery(String companyId, DeliveryRequest request, String headerCorrelationId) {
        List<String> recipients = normalizeRecipients(request.to());
        DeliveryMode mode = DeliveryMode.resolve(request.deliveryMode());
        return deliveryAttemptRepository.findByCompanyIdAndAttemptId(companyId, request.attemptId())
                .map(attempt -> toReceipt(attempt, parseRecipients(attempt.getToAddress())))
                .orElseGet(() -> createAttempt(companyId, request, recipients, mode, headerCorrelationId));
    }

    @Transactional
    public DeliveryAttemptResponse getDeliveryAttempt(String companyId, String attemptId) {
        DeliveryAttemptEntity attempt = deliveryAttemptRepository.findByCompanyIdAndAttemptId(companyId, attemptId)
                .orElseThrow(() -> new DeliveryNotFoundException(attemptId));
        List<DeliveryEventEntity> events = deliveryEventRepository
                .findAllByCompanyIdAndAttemptIdOrderByEventAtUtcAsc(companyId, attemptId);
        return toAttemptResponse(attempt, events);
    }

    @Transactional
    public DeliveryEventResponse handleProviderEvent(String companyId, String provider, ProviderEventRequest request) {
        DeliveryEventType eventType = parseEventType(request.eventType());
        if (!StringUtils.hasText(request.attemptId()) && !StringUtils.hasText(request.providerMessageId())) {
            throw new BadRequestException("attemptId or providerMessageId is required");
        }
        DeliveryAttemptEntity attempt = findOrCreateAttempt(companyId, provider, request, eventType);
        updateAttemptFromEvent(attempt, request, eventType);

        DeliveryEventEntity eventEntity = new DeliveryEventEntity();
        eventEntity.setCompanyId(companyId);
        eventEntity.setProvider(provider);
        eventEntity.setAttemptId(attempt.getAttemptId());
        eventEntity.setChannel(attempt.getChannel());
        eventEntity.setEventType(eventType);
        eventEntity.setEventAtUtc(eventAtUtc(request.eventAtUtc()));
        eventEntity.setProviderMessageId(request.providerMessageId());
        eventEntity.setRawStatus(request.rawStatus());
        eventEntity.setErrorCode(request.errorCode());
        eventEntity.setErrorMessage(request.errorMessage());

        DeliveryEventEntity savedEvent = deliveryEventRepository.save(eventEntity);
        deliveryEventForwarder.forward(toForwardEvent(savedEvent));
        return toEventResponse(savedEvent);
    }

    private DeliveryReceipt createAttempt(String companyId,
                                          DeliveryRequest request,
                                          List<String> recipients,
                                          DeliveryMode mode,
                                          String headerCorrelationId) {
        DeliveryAttemptEntity attempt = new DeliveryAttemptEntity();
        attempt.setCompanyId(companyId);
        attempt.setAttemptId(request.attemptId());
        attempt.setChannel(request.channel());
        attempt.setDeliveryMode(mode);
        attempt.setToAddress(String.join(";", recipients));
        attempt.setSubject(request.channel() == Channel.EMAIL ? request.subject() : null);
        attempt.setContentText(request.content().text());
        attempt.setContentHtml(request.content().html());
        attempt.setAttachmentsJson(serializeAttachments(request.attachments()));
        attempt.setRequestCorrelationId(resolveCorrelationId(request.correlationId(), headerCorrelationId));
        attempt.setState(DeliveryStatus.QUEUED);
        attempt.setProviderMessageId(null);
        attempt.setErrorCode(null);
        attempt.setErrorMessage(null);

        try {
            DeliveryAttemptEntity savedAttempt = deliveryAttemptRepository.save(attempt);
            DeliveryEventEntity queuedEvent = createQueuedEvent(savedAttempt);
            DeliveryEventEntity savedEvent = deliveryEventRepository.save(queuedEvent);
            deliveryEventForwarder.forward(toForwardEvent(savedEvent));
            return toReceipt(savedAttempt, recipients);
        } catch (DataIntegrityViolationException ex) {
            return deliveryAttemptRepository.findByCompanyIdAndAttemptId(companyId, request.attemptId())
                    .map(existing -> toReceipt(existing, parseRecipients(existing.getToAddress())))
                    .orElseThrow(() -> ex);
        }
    }

    private DeliveryEventEntity createQueuedEvent(DeliveryAttemptEntity attempt) {
        DeliveryEventEntity event = new DeliveryEventEntity();
        event.setCompanyId(attempt.getCompanyId());
        event.setProvider("internal");
        event.setAttemptId(attempt.getAttemptId());
        event.setChannel(attempt.getChannel());
        event.setEventType(DeliveryEventType.QUEUED);
        event.setEventAtUtc(nowUtcMicros());
        return event;
    }

    private DeliveryAttemptEntity findOrCreateAttempt(String companyId,
                                                      String provider,
                                                      ProviderEventRequest request,
                                                      DeliveryEventType eventType) {
        DeliveryAttemptEntity attempt = null;
        if (StringUtils.hasText(request.providerMessageId())) {
            attempt = deliveryAttemptRepository.findByCompanyIdAndProviderMessageId(companyId, request.providerMessageId()).orElse(null);
        }
        if (attempt == null && StringUtils.hasText(request.attemptId())) {
            attempt = deliveryAttemptRepository.findByCompanyIdAndAttemptId(companyId, request.attemptId()).orElse(null);
        }
        if (attempt == null) {
            attempt = new DeliveryAttemptEntity();
            attempt.setCompanyId(companyId);
            attempt.setAttemptId(StringUtils.hasText(request.attemptId()) ? request.attemptId() : request.providerMessageId());
            attempt.setChannel(request.channel());
            attempt.setDeliveryMode(DeliveryMode.SINGLE);
            attempt.setToAddress("unknown");
            attempt.setContentText("callback-only");
            attempt.setState(DeliveryStatus.forEvent(eventType));
            attempt.setRequestCorrelationId(null);
            attempt.setProviderMessageId(request.providerMessageId());
        }
        return deliveryAttemptRepository.save(attempt);
    }

    private void updateAttemptFromEvent(DeliveryAttemptEntity attempt,
                                        ProviderEventRequest request,
                                        DeliveryEventType eventType) {
        attempt.setChannel(request.channel());
        if (StringUtils.hasText(request.providerMessageId())) {
            attempt.setProviderMessageId(request.providerMessageId());
        }
        if (StringUtils.hasText(request.errorCode())) {
            attempt.setErrorCode(request.errorCode());
        }
        if (StringUtils.hasText(request.errorMessage())) {
            attempt.setErrorMessage(request.errorMessage());
        }
        attempt.setState(DeliveryStatus.forEvent(eventType));
        deliveryAttemptRepository.save(attempt);
    }

    private DeliveryReceipt toReceipt(DeliveryAttemptEntity attempt, List<String> recipients) {
        return new DeliveryReceipt(
                attempt.getAttemptId(),
                attempt.getChannel(),
                attempt.getDeliveryMode(),
                recipients,
                attempt.getState(),
                attempt.getProviderMessageId(),
                attempt.getErrorCode(),
                attempt.getErrorMessage(),
                attempt.getRequestCorrelationId(),
                attempt.getCreatedAtUtc(),
                attempt.getUpdatedAtUtc()
        );
    }

    private DeliveryAttemptResponse toAttemptResponse(DeliveryAttemptEntity attempt,
                                                      List<DeliveryEventEntity> events) {
        List<String> recipients = parseRecipients(attempt.getToAddress());
        List<DeliveryEventResponse> responses = events.stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
        return new DeliveryAttemptResponse(
                attempt.getAttemptId(),
                attempt.getChannel(),
                attempt.getDeliveryMode(),
                recipients,
                attempt.getSubject(),
                attempt.getState(),
                attempt.getProviderMessageId(),
                attempt.getErrorCode(),
                attempt.getErrorMessage(),
                attempt.getRequestCorrelationId(),
                attempt.getCreatedAtUtc(),
                attempt.getUpdatedAtUtc(),
                responses
        );
    }

    private DeliveryEventResponse toEventResponse(DeliveryEventEntity entity) {
        return new DeliveryEventResponse(
                entity.getChannel(),
                entity.getProvider(),
                entity.getEventType(),
                entity.getEventAtUtc(),
                entity.getProviderMessageId(),
                entity.getRawStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage()
        );
    }

    private DeliveryEvent toForwardEvent(DeliveryEventEntity entity) {
        return new DeliveryEvent(
                entity.getAttemptId(),
                entity.getChannel(),
                entity.getEventType(),
                entity.getEventAtUtc(),
                entity.getProviderMessageId(),
                entity.getRawStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage()
        );
    }

    private String resolveCorrelationId(String requestCorrelationId, String headerCorrelationId) {
        if (StringUtils.hasText(requestCorrelationId)) {
            return requestCorrelationId;
        }
        return headerCorrelationId;
    }

    private List<String> normalizeRecipients(String to) {
        if (!StringUtils.hasText(to)) {
            throw new BadRequestException("to is required");
        }
        Set<String> normalized = Arrays.stream(to.split("[,;]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new BadRequestException("to must contain at least one recipient");
        }
        return Collections.unmodifiableList(List.copyOf(normalized));
    }

    private List<String> parseRecipients(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(";"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private DeliveryEventType parseEventType(String eventType) {
        try {
            return DeliveryEventType.from(eventType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
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

    private OffsetDateTime eventAtUtc(OffsetDateTime provided) {
        return provided != null ? provided : nowUtcMicros();
    }

    private OffsetDateTime nowUtcMicros() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int micros = now.getNano() / 1_000;
        return now.withNano(micros * 1_000);
    }
}
