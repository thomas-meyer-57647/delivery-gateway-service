package de.innologic.delivery.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompanyIdResolver {

    public String resolveRequiredCompanyId(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("Missing JWT");
        }
        String companyId = jwt.getClaimAsString("companyId");
        if (!StringUtils.hasText(companyId)) {
            throw new AccessDeniedException("Missing required claim: companyId");
        }
        return companyId;
    }
}
