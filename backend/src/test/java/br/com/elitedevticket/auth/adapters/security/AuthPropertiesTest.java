package br.com.elitedevticket.auth.adapters.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthPropertiesTest {
    @Test
    void demoRequiresA256BitBase64SecretAndSecureCookies() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "demo");
        environment.setActiveProfiles("demo");
        AuthProperties properties = new AuthProperties(environment);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_JWT_SECRET");

        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[32]));
        properties.validate();
        assertThat(properties.secureCookies()).isTrue();

        properties.getCookies().setSecure(false);
        assertThatThrownBy(properties::validate).hasMessageContaining("AUTH_COOKIES_SECURE");
    }

    @Test
    void localMayGenerateAnEphemeralSecretAndUsesNonSecureCookies() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AuthProperties properties = new AuthProperties(environment);

        properties.validate();

        assertThat(properties.secureCookies()).isFalse();
    }

    @Test
    void mixedDevelopmentAndSecureProfilesStillRequireAnExternalSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local", "prod");
        AuthProperties properties = new AuthProperties(environment);

        assertThatThrownBy(properties::validate).hasMessageContaining("AUTH_JWT_SECRET");

        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[32]));
        properties.validate();
        assertThat(properties.secureCookies()).isTrue();
    }

    @Test
    void defaultProfileRequiresAnExternalSecret() {
        AuthProperties properties = new AuthProperties(new MockEnvironment());

        assertThatThrownBy(properties::validate).hasMessageContaining("AUTH_JWT_SECRET");

        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[32]));
        properties.validate();
        assertThat(properties.secureCookies()).isTrue();
    }

    @Test
    void rejectsCorsWildcardBecauseRequestsUseCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AuthProperties properties = new AuthProperties(environment);
        properties.getCors().setAllowedOrigins(List.of("*"));

        assertThatThrownBy(properties::validate).hasMessageContaining("AUTH_CORS_ALLOWED_ORIGINS");
    }

    @Test
    void rejectsInvalidCostTtlAndShortOrMalformedExternalSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AuthProperties invalidCost = new AuthProperties(environment);
        invalidCost.setBcryptCost(3);
        assertThatThrownBy(invalidCost::validate).hasMessageContaining("AUTH_BCRYPT_COST");

        AuthProperties invalidTtl = new AuthProperties(environment);
        invalidTtl.getJwt().setTtl(Duration.ZERO);
        assertThatThrownBy(invalidTtl::validate).hasMessageContaining("AUTH_JWT_TTL");

        AuthProperties shortSecret = new AuthProperties(environment);
        shortSecret.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[31]));
        assertThatThrownBy(shortSecret::validate).hasMessageContaining("256 bits");

        AuthProperties malformedSecret = new AuthProperties(environment);
        malformedSecret.getJwt().setSecret("not-base64");
        assertThatThrownBy(malformedSecret::validate).hasMessageContaining("Base64");
    }

    @Test
    void normalizesConfiguredCorsOriginsAndBcryptCost() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        AuthProperties properties = new AuthProperties(environment);
        properties.setBcryptCost(12);
        properties.getCors().setAllowedOrigins(List.of(" ", "http://localhost:5173", "http://localhost:5173"));

        String hash = new SecurityConfiguration().passwordEncoder(properties).encode("password");

        assertThat(hash).startsWith("$2a$12$");
        assertThat(properties.getCors().getAllowedOrigins()).containsExactly("http://localhost:5173");
    }
}
