package br.com.elitedevticket.auth.adapters.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public final class CsrfTokenRotation {
    private final CsrfTokenRepository repository;

    public CsrfTokenRotation(CsrfTokenRepository repository) {
        this.repository = repository;
    }

    public CsrfToken rotate(HttpServletRequest request, HttpServletResponse response) {
        repository.saveToken(null, request, response);
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);
        return token;
    }
}
