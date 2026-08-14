package br.com.elitedevticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProbeFailureIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 1_000);
        registry.add("spring.datasource.hikari.validation-timeout", () -> 500);
    }

    @LocalServerPort
    private int port;

    @Test
    void databaseFailureMakesReadinessDownWhileLivenessRemainsUp() throws Exception {
        POSTGRES.stop();

        HttpResponse<String> liveness = healthResponse("liveness");
        HttpResponse<String> readiness = healthResponse("readiness");

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(liveness.body()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(readiness.statusCode()).isEqualTo(503);
        assertThat(readiness.body()).isEqualTo("{\"status\":\"DOWN\"}");
    }

    private HttpResponse<String> healthResponse(String probe) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health/" + probe))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
