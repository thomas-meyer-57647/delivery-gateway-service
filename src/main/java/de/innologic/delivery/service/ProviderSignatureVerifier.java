package de.innologic.delivery.service;

import jakarta.servlet.http.HttpServletRequest;

public interface ProviderSignatureVerifier {

    boolean verify(String provider, HttpServletRequest request);
}
