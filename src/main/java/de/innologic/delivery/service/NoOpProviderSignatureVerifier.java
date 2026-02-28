package de.innologic.delivery.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class NoOpProviderSignatureVerifier implements ProviderSignatureVerifier {

    @Override
    public boolean verify(String provider, HttpServletRequest request) {
        return true;
    }
}
