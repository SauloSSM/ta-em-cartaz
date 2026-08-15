package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventsEndpointsIntegrationTest {

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

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void anonymousRequestToCreateDraftReturns401Unauthenticated() throws Exception {
        String csrf = bootstrapCsrf();
        HttpResponse<String> response = post(
                "/api/v1/events/drafts",
                "XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Festival Não Autenticado\"}"
        );

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"AUTH_UNAUTHENTICATED\"");
    }

    @Test
    void customerOrGateRoleCannotCreateDraftReturns403Forbidden() throws Exception {
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();
        HttpResponse<String> response = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Tentativa de Cliente\"}"
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");
    }

    @Test
    void createDraftWithoutCsrfReturns403Forbidden() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        HttpResponse<String> response = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession,
                "",
                "{\"title\":\"Sem CSRF\"}"
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"AUTH_CSRF_INVALID\"");
    }

    @Test
    void organizerCanCreateDraftAndRetrieveItWithOwnershipChecks() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String payload = """
                {
                  "externalId": "tm-100",
                  "title": "Rock in Rio 2026",
                  "description": "Festival de música",
                  "imageUrl": "https://images.example.com/rock.jpg",
                  "category": "Rock"
                }
                """;

        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );

        assertThat(createResponse.statusCode()).isEqualTo(201);
        assertThat(createResponse.headers().firstValue("Location")).isPresent();
        String location = createResponse.headers().firstValue("Location").get();
        assertThat(location).startsWith("/api/v1/events/");

        String responseBody = createResponse.body();
        assertThat(responseBody)
                .contains("\"title\":\"Rock in Rio 2026\"")
                .contains("\"externalId\":\"tm-100\"")
                .contains("\"description\":\"Festival de música\"")
                .contains("\"imageUrl\":\"https://images.example.com/rock.jpg\"")
                .contains("\"category\":\"Rock\"")
                .contains("\"status\":\"DRAFT\"")
                .contains("\"organizerId\":\"00000000-0000-0000-0000-000000000001\"");

        // Reusing same externalId is allowed and creates distinct event
        HttpResponse<String> secondCreateResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(secondCreateResponse.statusCode()).isEqualTo(201);
        assertThat(secondCreateResponse.headers().firstValue("Location").get()).isNotEqualTo(location);

        // Organizer owner can retrieve draft
        HttpResponse<String> getResponse = get(location, "EDT_SESSION=" + organizerSession);
        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.body())
                .contains("\"title\":\"Rock in Rio 2026\"")
                .contains("\"status\":\"DRAFT\"");

        // Customer cannot retrieve draft (403)
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        HttpResponse<String> customerGetResponse = get(location, "EDT_SESSION=" + customerSession);
        assertThat(customerGetResponse.statusCode()).isEqualTo(403);
        assertThat(customerGetResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");
    }

    @Test
    void createDraftWithBlankTitleReturns400BadRequest() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        HttpResponse<String> response = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"\"}"
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"code\":\"AUTH_INVALID_REQUEST\"")
                .contains("\"fieldErrors\":")
                .contains("\"field\":\"title\"");
    }

    @Test
    void getNonExistentEventReturns404NotFound() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        HttpResponse<String> response = get("/api/v1/events/" + UUID.randomUUID(), "EDT_SESSION=" + organizerSession);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");
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
