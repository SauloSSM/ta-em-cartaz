package br.com.elitedevticket.auth.adapters.security;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth")
public final class AuthProperties {
    private final Environment environment;
    private int bcryptCost = 10;
    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Cookies cookies = new Cookies();

    public AuthProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (bcryptCost < 4 || bcryptCost > 31) {
            throw new IllegalStateException("AUTH_BCRYPT_COST deve estar entre 4 e 31.");
        }
        if (jwt.ttl.isZero() || jwt.ttl.isNegative()) {
            throw new IllegalStateException("AUTH_JWT_TTL deve ser positivo.");
        }
        if (requiresSecureCookies() && Boolean.FALSE.equals(cookies.secure)) {
            throw new IllegalStateException("AUTH_COOKIES_SECURE não pode ser false em demo/prod.");
        }
        if (cors.allowedOrigins.contains("*")) {
            throw new IllegalStateException("AUTH_CORS_ALLOWED_ORIGINS nao aceita wildcard com credenciais.");
        }
        if (isDevelopment() && jwt.secret.isBlank()) {
            return;
        }
        if (jwt.secret.isBlank()) {
            throw new IllegalStateException("AUTH_JWT_SECRET é obrigatório fora de local/test.");
        }
        try {
            if (Base64.getDecoder().decode(jwt.secret).length < 32) {
                throw new IllegalStateException("AUTH_JWT_SECRET deve conter pelo menos 256 bits.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AUTH_JWT_SECRET deve usar Base64 válido.", exception);
        }
    }

    public boolean isDevelopment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0
                && Arrays.stream(activeProfiles).allMatch(profile -> profile.equals("local") || profile.equals("test"));
    }

    public boolean secureCookies() {
        return cookies.secure == null ? requiresSecureCookies() : cookies.secure;
    }

    private boolean requiresSecureCookies() {
        return environment.matchesProfiles("demo", "prod") || environment.getActiveProfiles().length == 0;
    }

    public int getBcryptCost() {
        return bcryptCost;
    }

    public void setBcryptCost(int bcryptCost) {
        this.bcryptCost = bcryptCost;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Cookies getCookies() {
        return cookies;
    }

    public static final class Jwt {
        private String secret = "";
        private Duration ttl = Duration.ofHours(8);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret == null ? "" : secret.trim();
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static final class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return List.copyOf(allowedOrigins);
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            if (allowedOrigins == null) {
                this.allowedOrigins = new ArrayList<>();
                return;
            }
            this.allowedOrigins = allowedOrigins.stream()
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }

    public static final class Cookies {
        private Boolean secure;

        public Boolean getSecure() {
            return secure;
        }

        public void setSecure(Boolean secure) {
            this.secure = secure;
        }
    }
}
