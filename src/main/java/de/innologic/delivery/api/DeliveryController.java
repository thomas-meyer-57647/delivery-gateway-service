package de.innologic.delivery.api;

import de.innologic.delivery.api.dto.DeliveryAttemptResponse;
import de.innologic.delivery.api.dto.DeliveryReceipt;
import de.innologic.delivery.api.dto.DeliveryRequest;
import de.innologic.delivery.common.security.CompanyIdResolver;
import de.innologic.delivery.common.web.CorrelationIdFilter;
import de.innologic.delivery.service.DeliveryApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryApplicationService deliveryApplicationService;
    private final CompanyIdResolver companyIdResolver;

    public DeliveryController(DeliveryApplicationService deliveryApplicationService,
                              CompanyIdResolver companyIdResolver) {
        this.deliveryApplicationService = deliveryApplicationService;
        this.companyIdResolver = companyIdResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeliveryReceipt create(@Valid @RequestBody DeliveryRequest request,
                                  @AuthenticationPrincipal Jwt jwt,
                                  HttpServletRequest servletRequest) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        String correlationId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return deliveryApplicationService.createDelivery(companyId, request, correlationId);
    }

    @GetMapping("/{attemptId}")
    public DeliveryAttemptResponse get(@PathVariable String attemptId,
                                       @AuthenticationPrincipal Jwt jwt) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        return deliveryApplicationService.getDeliveryAttempt(companyId, attemptId);
    }
}
