package br.com.elitedevticket.auth.adapters.security;

import br.com.elitedevticket.auth.http.ApiErrorResponse;
import br.com.elitedevticket.auth.http.AuthErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class AuthHttpErrorWriter {
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public AuthHttpErrorWriter(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public void csrf(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                AuthErrorCode.AUTH_CSRF_INVALID,
                "Token CSRF inválido.",
                null,
                UUID.randomUUID().toString(),
                clock.instant()));
    }
}
