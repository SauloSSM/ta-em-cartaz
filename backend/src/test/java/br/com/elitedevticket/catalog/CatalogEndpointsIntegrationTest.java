package br.com.elitedevticket.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.catalog.application.CatalogProvider;
import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import br.com.elitedevticket.catalog.domain.CatalogUnavailableException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogEndpointsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private CatalogProvider catalogProvider;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void anonymousRequestReturns401Unauthenticated() throws Exception {
        HttpResponse<String> response = get("/api/v1/catalog/events?keyword=Rock", "");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(response.body())
                .contains("\"code\":\"AUTH_UNAUTHENTICATED\"")
                .contains("\"message\":", "\"traceId\":", "\"timestamp\":");
    }

    @Test
    void customerRoleReturns403Forbidden() throws Exception {
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        HttpResponse<String> response = get("/api/v1/catalog/events?keyword=Rock", "EDT_SESSION=" + customerSession);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body())
                .contains("\"code\":\"AUTH_FORBIDDEN\"")
                .contains("\"message\":", "\"traceId\":", "\"timestamp\":");
    }

    @Test
    void gateRoleReturns403Forbidden() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        HttpResponse<String> response = get("/api/v1/catalog/events?keyword=Rock", "EDT_SESSION=" + gateSession);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body())
                .contains("\"code\":\"AUTH_FORBIDDEN\"")
                .contains("\"message\":", "\"traceId\":", "\"timestamp\":");
    }

    @Test
    void organizerRoleReturns200WithCatalogSearchResponse() throws Exception {
        when(catalogProvider.searchEvents("Rock")).thenReturn(List.of(
                new CatalogEventReference(
                        "tm-100",
                        "Rock Festival Live",
                        "Descrição detalhada",
                        "https://images.example.com/live.jpg",
                        "Rock")));

        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        HttpResponse<String> response = get("/api/v1/catalog/events?keyword=Rock", "EDT_SESSION=" + organizerSession);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(response.body())
                .contains("\"events\":[")
                .contains("\"externalId\":\"tm-100\"")
                .contains("\"title\":\"Rock Festival Live\"")
                .contains("\"description\":\"Descrição detalhada\"")
                .contains("\"imageUrl\":\"https://images.example.com/live.jpg\"")
                .contains("\"category\":\"Rock\"");
    }

    @Test
    void catalogUnavailableReturns503Envelope() throws Exception {
        when(catalogProvider.searchEvents(any()))
                .thenThrow(new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível."));

        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        HttpResponse<String> response = get("/api/v1/catalog/events?keyword=Error", "EDT_SESSION=" + organizerSession);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(response.body())
                .contains("\"code\":\"CATALOG_UNAVAILABLE\"")
                .contains("\"message\":\"Catálogo Ticketmaster temporariamente indisponível.\"")
                .contains("\"traceId\":", "\"timestamp\":")
                .doesNotContain("stackTrace", "exception");
    }

    private String loginSession(String email) throws Exception {
        String csrf = bootstrapCsrf();
        HttpResponse<String> login = post(
                "/api/v1/auth/login",
                "XSRF-TOKEN=" + csrf,
                csrf,
                "{\"email\":\"" + email + "\",\"password\":\"password\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        return cookieValue(login, "EDT_SESSION");
    }

    private String bootstrapCsrf() throws Exception {
        HttpResponse<String> response = get("/api/v1/auth/session", "");
        return cookieValue(response, "XSRF-TOKEN");
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String cookie, String csrf, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (!csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String cookieValue(HttpResponse<String> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .reduce((first, second) -> second)
                .map(cookie -> cookie.substring(name.length() + 1, cookie.indexOf(';')))
                .orElseThrow();
    }
}
