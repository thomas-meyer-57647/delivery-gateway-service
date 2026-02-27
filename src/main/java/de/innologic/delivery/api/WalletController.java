package de.innologic.delivery.api;

import de.innologic.delivery.api.dto.WalletBalanceResponse;
import de.innologic.delivery.api.dto.WalletTopupRequest;
import de.innologic.delivery.common.security.CompanyIdResolver;
import de.innologic.delivery.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;
    private final CompanyIdResolver companyIdResolver;

    public WalletController(WalletService walletService, CompanyIdResolver companyIdResolver) {
        this.walletService = walletService;
        this.companyIdResolver = companyIdResolver;
    }

    @GetMapping
    public WalletBalanceResponse balance(@AuthenticationPrincipal Jwt jwt) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        return walletService.getBalance(companyId);
    }

    @PostMapping("/topups")
    public WalletBalanceResponse topup(@Valid @RequestBody WalletTopupRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        String companyId = companyIdResolver.resolveRequiredCompanyId(jwt);
        return walletService.topup(companyId, request.amount());
    }
}
