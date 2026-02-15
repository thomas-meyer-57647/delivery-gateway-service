package de.innologic.delivery.api;

import de.innologic.delivery.api.dto.DeliveryEvent;
import de.innologic.delivery.api.dto.ProviderCallbackRequest;
import de.innologic.delivery.common.security.CompanyIdResolver;
import de.innologic.delivery.service.DeliveryApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/provider-callbacks")
public class ProviderCallbackController {

    private final DeliveryApplicationService deliveryApplicationService;
    private final CompanyIdResolver companyIdResolver;

    public ProviderCallbackController(DeliveryApplicationService deliveryApplicationService,
                                      CompanyIdResolver companyIdResolver) {
        this.deliveryApplicationService = deliveryApplicationService;
        this.companyIdResolver = companyIdResolver;
    }

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeliveryEvent callback(@PathVariable @NotBlank @Size(max = 50) String provider,
                                  @Valid @RequestBody ProviderCallbackRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        return deliveryApplicationService.handleProviderCallback(companyId, provider, request);
    }
}
