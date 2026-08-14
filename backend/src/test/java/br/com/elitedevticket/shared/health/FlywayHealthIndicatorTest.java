package br.com.elitedevticket.shared.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class FlywayHealthIndicatorTest {
    @Test
    void reportsDownWhenFlywayValidationIsNotSuccessful() {
        Flyway flyway = mock(Flyway.class);
        ValidateResult invalidResult = new ValidateResult(
                "11.14.1", "database", null, false, 0, List.of(), List.of());
        when(flyway.validateWithResult()).thenReturn(invalidResult);

        assertThat(new FlywayHealthIndicator(flyway).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
