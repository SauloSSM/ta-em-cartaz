package br.com.elitedevticket.auth.adapters.security;

import br.com.elitedevticket.auth.application.AuthUser;
import br.com.elitedevticket.auth.domain.Role;
import br.com.elitedevticket.auth.domain.SessionUser;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final AuthProperties properties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, Clock clock, AuthProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.clock = clock;
        this.properties = properties;
    }

    public IssuedJwt issue(AuthUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getJwt().getTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("elite-dev-ticket")
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", user.email())
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedJwt(value, expiresAt);
    }

    public SessionUser decode(String token) {
        var jwt = decoder.decode(token);
        String subject = requiredClaim(jwt.getSubject());
        String email = requiredClaim(jwt.getClaimAsString("email"));
        String role = requiredClaim(jwt.getClaimAsString("role"));
        return new SessionUser(
                UUID.fromString(subject),
                email,
                Role.valueOf(role));
    }

    private String requiredClaim(String value) {
        if (value == null || value.isBlank()) {
            throw new org.springframework.security.oauth2.jwt.JwtException("JWT sem claims obrigatórias.");
        }
        return value;
    }

    public record IssuedJwt(String value, Instant expiresAt) {
    }
}
