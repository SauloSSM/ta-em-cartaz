package br.com.elitedevticket.auth.adapters.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

class CsrfTokenRotationTest {
    @Test
    void rotationEmitsSingleNonEmptyCookieWithoutDeletionHeader() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AuthProperties properties = new AuthProperties(environment);
        CsrfTokenRepository repository = new SecurityConfiguration().csrfTokenRepository(properties);
        CsrfTokenRotation rotation = new CsrfTokenRotation(repository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CsrfToken token = rotation.rotate(request, response);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();

        // Must emit exactly one Set-Cookie header for XSRF-TOKEN
        var xsrfCookies = response.getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .toList();
        assertThat(xsrfCookies).hasSize(1);

        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(token.getToken());
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getMaxAge()).isEqualTo(-1);
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");

        assertThat(request.getAttribute(CsrfToken.class.getName())).isEqualTo(token);
        assertThat(request.getAttribute("_csrf")).isEqualTo(token);
    }
}
