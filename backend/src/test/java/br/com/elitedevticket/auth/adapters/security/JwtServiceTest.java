package br.com.elitedevticket.auth.adapters.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.auth.application.AuthUser;
import br.com.elitedevticket.auth.domain.Role;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");

    @Test
    void issuesHs256TokenWithConfiguredEightHourTtlAndDecodesIdentity() {
        var fixture = fixture(Clock.fixed(NOW, ZoneOffset.UTC));
        AuthUser user = new AuthUser(UUID.randomUUID(), "gate@demo.test", Role.GATE, "unused");

        JwtService.IssuedJwt issued = fixture.service().issue(user);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plusSeconds(8 * 60 * 60));
        String header = new String(
                Base64.getUrlDecoder().decode(issued.value().substring(0, issued.value().indexOf('.'))),
                StandardCharsets.UTF_8);
        assertThat(header).contains("\"alg\":\"HS256\"");
        assertThat(fixture.service().decode(issued.value()))
                .extracting("id", "email", "role")
                .containsExactly(user.id(), user.email(), user.role());
    }

    @Test
    void rejectsAnExpiredToken() {
        var issuer = fixture(Clock.fixed(NOW, ZoneOffset.UTC));
        AuthUser user = new AuthUser(UUID.randomUUID(), "organizer@demo.test", Role.ORGANIZER, "unused");
        String token = issuer.service().issue(user).value();
        var expired = fixture(Clock.fixed(NOW.plusSeconds(9 * 60 * 60), ZoneOffset.UTC));

        assertThatThrownBy(() -> expired.service().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenImmediatelyAfterExpirationWithoutClockSkew() {
        var issuer = fixture(Clock.fixed(NOW, ZoneOffset.UTC));
        AuthUser user = new AuthUser(UUID.randomUUID(), "customer@demo.test", Role.CUSTOMER, "unused");
        JwtService.IssuedJwt issued = issuer.service().issue(user);
        var afterExpiration = fixture(Clock.fixed(issued.expiresAt().plusNanos(1), ZoneOffset.UTC));

        assertThatThrownBy(() -> afterExpiration.service().decode(issued.value())).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenWithoutExpirationClaim() {
        var fixture = fixture(Clock.fixed(NOW, ZoneOffset.UTC));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("elite-dev-ticket")
                .subject(UUID.randomUUID().toString())
                .issuedAt(NOW)
                .claim("email", "gate@demo.test")
                .claim("role", Role.GATE.name())
                .build();
        String token = fixture.encoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> fixture.service().decode(token)).isInstanceOf(JwtException.class);
    }

    private Fixture fixture(Clock clock) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        AuthProperties properties = new AuthProperties(environment);
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[32]));
        properties.validate();
        SecurityConfiguration configuration = new SecurityConfiguration();
        SecretKey key = configuration.jwtSecretKey(properties);
        JwtEncoder encoder = configuration.jwtEncoder(key);
        return new Fixture(
                new JwtService(encoder, configuration.jwtDecoder(key, clock), clock, properties),
                encoder);
    }

    private record Fixture(JwtService service, JwtEncoder encoder) {
    }
}
