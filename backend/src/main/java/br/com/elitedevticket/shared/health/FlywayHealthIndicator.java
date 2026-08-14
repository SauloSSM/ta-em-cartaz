package br.com.elitedevticket.shared.health;

import org.flywaydb.core.Flyway;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FlywayHealthIndicator implements HealthIndicator {
    private final Flyway flyway;

    public FlywayHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            boolean valid = flyway.validateWithResult().validationSuccessful;
            return valid ? Health.up().build() : Health.down().build();
        } catch (RuntimeException exception) {
            return Health.down().build();
        }
    }
}
