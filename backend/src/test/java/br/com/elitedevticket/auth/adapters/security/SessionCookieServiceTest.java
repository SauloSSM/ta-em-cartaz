package br.com.elitedevticket.auth.adapters.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfTokenRepository;

class SessionCookieServiceTest {
    @Test
    void alignsCookieMaxAgeWithJwtExpiration() {
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AuthProperties properties = new AuthProperties(environment);
        var service = new SessionCookieService(properties, Clock.fixed(now, ZoneOffset.UTC));
        var response = new MockHttpServletResponse();

        service.set(response, new JwtService.IssuedJwt("signed-token", now.plusSeconds(37 * 60)));

        assertThat(response.getHeader("Set-Cookie"))
                .contains("EDT_SESSION=signed-token", "Max-Age=2220", "HttpOnly", "SameSite=Lax", "Path=/")
                .doesNotContain("Secure");
    }

    @Test
    void demoUsesSecureAttributesForSessionAndReadableCsrfCookies() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("demo");
        AuthProperties properties = new AuthProperties(environment);
        var response = new MockHttpServletResponse();
        var service = new SessionCookieService(properties, Clock.systemUTC());

        service.clear(response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("EDT_SESSION=", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Lax", "Path=/");

        CsrfTokenRepository repository = new SecurityConfiguration().csrfTokenRepository(properties);
        var csrfResponse = new MockHttpServletResponse();
        var request = new MockHttpServletRequest();
        repository.saveToken(repository.generateToken(request), request, csrfResponse);

        assertThat(csrfResponse.getHeader("Set-Cookie"))
                .contains("XSRF-TOKEN=", "Secure", "Path=/")
                .doesNotContain("HttpOnly");
        assertThat(csrfResponse.getCookie("XSRF-TOKEN").getAttribute("SameSite")).isEqualTo("Lax");
    }
}
