package br.com.elitedevticket.auth.adapters.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public final class SessionCookieService {
    public static final String COOKIE_NAME = "EDT_SESSION";

    private final AuthProperties properties;
    private final Clock clock;

    public SessionCookieService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void set(HttpServletResponse response, JwtService.IssuedJwt jwt) {
        Duration maxAge = Duration.between(clock.instant(), jwt.expiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(jwt.value()).maxAge(maxAge).build().toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("").maxAge(Duration.ZERO).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Lax")
                .path("/");
    }
}
