package br.com.elitedevticket.catalog;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.catalog.adapters.ticketmaster.TicketmasterProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TicketmasterPropertiesTest {

    @Test
    void defaultPropertiesAreValid() {
        TicketmasterProperties properties = new TicketmasterProperties();
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsMaxRetriesOutOfRange() {
        TicketmasterProperties properties = new TicketmasterProperties();
        properties.setMaxRetries(-1);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-retries");

        properties.setMaxRetries(2);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-retries");
    }

    @Test
    void acceptsMaxRetriesZeroOrOne() {
        TicketmasterProperties properties = new TicketmasterProperties();
        properties.setMaxRetries(0);
        assertThatCode(properties::validate).doesNotThrowAnyException();

        properties.setMaxRetries(1);
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsTotalBudgetExceedingFiveSecondsOrNonPositive() {
        TicketmasterProperties properties = new TicketmasterProperties();
        properties.setTotalBudget(Duration.ofMillis(5001));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("total-budget");

        properties.setTotalBudget(Duration.ZERO);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("total-budget");

        properties.setTotalBudget(Duration.ofSeconds(-1));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("total-budget");

        properties.setTotalBudget(null);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("total-budget");
    }

    @Test
    void rejectsNonPositiveConnectOrReadTimeouts() {
        TicketmasterProperties properties = new TicketmasterProperties();
        properties.setConnectTimeout(Duration.ZERO);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("connect-timeout");

        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ZERO);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-timeout");
    }

    @Test
    void rejectsBlankBaseUrl() {
        TicketmasterProperties properties = new TicketmasterProperties();
        properties.setBaseUrl("   ");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base-url");
    }
}
