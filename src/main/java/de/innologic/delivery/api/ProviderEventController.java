package de.innologic.delivery.api;

import de.innologic.delivery.api.dto.DeliveryEventResponse;
import de.innologic.delivery.api.dto.ProviderEventRequest;
import de.innologic.delivery.common.security.CompanyIdResolver;
import de.innologic.delivery.service.DeliveryApplicationService;
import de.innologic.delivery.service.ProviderSignatureVerifier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ProviderEventController {

    private final DeliveryApplicationService deliveryApplicationService;
    private final CompanyIdResolver companyIdResolver;
    private final ProviderSignatureVerifier signatureVerifier;

    public ProviderEventController(DeliveryApplicationService deliveryApplicationService,
                                   CompanyIdResolver companyIdResolver,
                                   ProviderSignatureVerifier signatureVerifier) {
        this.deliveryApplicationService = deliveryApplicationService;
        this.companyIdResolver = companyIdResolver;
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping(path = {
            "/api/v1/providers/{provider}/events",
            "/api/v1/provider-callbacks/{provider}"
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeliveryEventResponse handle(@PathVariable @NotBlank @Size(max = 50) String provider,
                                        @Valid @RequestBody ProviderEventRequest request,
                                    @AuthenticationPrincipal Jwt jwt,
                                    HttpServletRequest servletRequest) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        if (!signatureVerifier.verify(provider, servletRequest)) {
            throw new AccessDeniedException("Invalid provider signature");
        }
        return deliveryApplicationService.handleProviderEvent(companyId, provider, request);
    }
}
