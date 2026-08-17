package br.com.elitedevticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
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
class AuthEndpointsIntegrationTest {
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

    @BeforeEach
    void resetRbacFixture() {
        RbacTestFixture.reset();
    }

    @Test
    void bootstrapWithoutSessionIsAnonymousAndIssuesReadableCsrfCookie() throws Exception {
        HttpResponse<String> response = getSession("");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"authenticated\":false}");
        assertThat(setCookies(response)).anySatisfy(cookie -> {
            assertThat(cookie).startsWith("XSRF-TOKEN=");
            assertThat(cookie)
                    .contains("SameSite=Lax", "Path=/")
                    .doesNotContain("HttpOnly")
                    .doesNotContain("Secure");
        });
    }

    @Test
    void validLoginUsesHttpOnlyJwtCookieAndSessionReturnsOnlySafeIdentity() throws Exception {
        String csrf = bootstrapCsrf();

        HttpResponse<String> login = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"  CUSTOMER.ONE@DEMO.ELITEDEVTICKET.LOCAL  \",\"password\":\"password\"}");

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"authenticated\":true", "\"role\":\"CUSTOMER\"")
                .doesNotContain("password", "eyJ");
        String session = cookieValue(login, "EDT_SESSION");
        assertThat(cookieValue(login, "XSRF-TOKEN")).isNotEqualTo(csrf);
        assertThat(jwtEpochClaim(session, "exp") - jwtEpochClaim(session, "iat")).isEqualTo(60 * 60);
        String sessionCookie = setCookies(login).stream()
                .filter(cookie -> cookie.startsWith("EDT_SESSION="))
                .findFirst()
                .orElseThrow();
        assertThat(setCookies(login))
                .filteredOn(cookie -> cookie.startsWith("EDT_SESSION="))
                .singleElement()
                .satisfies(cookie -> assertThat(cookie)
                        .contains("HttpOnly", "SameSite=Lax", "Path=/")
                        .doesNotContain("Secure"));
        assertThat(cookieMaxAge(sessionCookie)).isBetween(3599L, 3600L);

        HttpResponse<String> current = getSession("EDT_SESSION=" + session);
        assertThat(current.statusCode()).isEqualTo(200);
        assertThat(current.body()).contains(
                "\"authenticated\":true",
                "\"email\":\"customer.one@demo.elitedevticket.local\"");
    }

    @Test
    void invalidCredentialsAreIndistinguishableAndDoNotCreateSession() throws Exception {
        String csrf = bootstrapCsrf();

        HttpResponse<String> response = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"missing@example.com\",\"password\":\"wrong\"}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body())
                .contains("\"code\":\"AUTH_INVALID_CREDENTIALS\"")
                .contains("\"message\":\"E-mail ou senha inválidos.\"")
                .contains("\"traceId\":", "\"timestamp\":")
                .doesNotContain("missing@example.com", "password_hash");
        assertThat(setCookies(response))
                .filteredOn(cookie -> cookie.startsWith("EDT_SESSION="))
                .singleElement()
                .satisfies(cookie -> assertThat(cookie)
                        .contains("Max-Age=0", "HttpOnly", "SameSite=Lax", "Path=/"));
    }

    @Test
    void invalidRequestUsesTheCompleteSafeEnvelopeAndFieldErrors() throws Exception {
        String csrf = bootstrapCsrf();

        HttpResponse<String> response = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"not-an-email\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"code\":\"AUTH_INVALID_REQUEST\"")
                .contains("\"fieldErrors\":[")
                .contains("\"field\":\"email\"")
                .contains("\"field\":\"password\"")
                .contains("\"traceId\":", "\"timestamp\":")
                .doesNotContain("not-an-email");
    }

    @Test
    void unknownLoginPropertyReturnsSafeBadRequestWithoutCreatingSession() throws Exception {
        String csrf = bootstrapCsrf();

        HttpResponse<String> response = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"customer.one@demo.elitedevticket.local\","
                        + "\"password\":\"password\",\"unexpected\":true}");

        assertAuthError(response, 400, "AUTH_INVALID_REQUEST");
        assertThat(setCookies(response)).noneMatch(cookie -> cookie.startsWith("EDT_SESSION="));
    }

    @Test
    void unsupportedLoginMediaTypeUsesSafeAuthEnvelope() throws Exception {
        String csrf = bootstrapCsrf();

        HttpResponse<String> response = postWithContentType(
                "/api/v1/auth/login",
                csrfCookie(csrf),
                csrf,
                "{\"email\":\"customer.one@demo.elitedevticket.local\",\"password\":\"password\"}",
                "text/plain");

        assertAuthError(response, 400, "AUTH_INVALID_REQUEST");
        assertThat(setCookies(response)).noneMatch(cookie -> cookie.startsWith("EDT_SESSION="));
    }

    @Test
    void invalidJwtBecomesAnonymousWithoutLeakingTheCause() throws Exception {
        HttpResponse<String> response = getSession("EDT_SESSION=not-a-jwt");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"authenticated\":false}");
        assertThat(setCookies(response))
                .filteredOn(cookie -> cookie.startsWith("EDT_SESSION="))
                .singleElement()
                .satisfies(cookie -> assertThat(cookie).contains("Max-Age=0"));
        assertThat(setCookies(response)).anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN="));
    }

    @Test
    void logoutIsIdempotentClearsSessionAndRotatesCsrf() throws Exception {
        String initialCsrf = bootstrapCsrf();
        HttpResponse<String> login = post("/api/v1/auth/login", csrfCookie(initialCsrf), initialCsrf,
                "{\"email\":\"gate@demo.elitedevticket.local\",\"password\":\"password\"}");
        String session = cookieValue(login, "EDT_SESSION");
        String loginCsrf = cookieValue(login, "XSRF-TOKEN");

        HttpResponse<String> first = post(
                "/api/v1/auth/logout",
                "EDT_SESSION=" + session + "; " + csrfCookie(loginCsrf),
                loginCsrf,
                null);
        assertThat(first.statusCode()).isEqualTo(204);
        assertThat(setCookies(first))
                .filteredOn(cookie -> cookie.startsWith("EDT_SESSION="))
                .singleElement()
                .satisfies(cookie -> assertThat(cookie).contains("Max-Age=0"));
        String rotated = cookieValue(first, "XSRF-TOKEN");
        assertThat(rotated).isNotEqualTo(loginCsrf);

        HttpResponse<String> second = post("/api/v1/auth/logout", csrfCookie(rotated), rotated, null);
        assertThat(second.statusCode()).isEqualTo(204);
    }

    @Test
    void invalidCsrfBlocksLoginAndLogoutWithoutMutation() throws Exception {
        HttpResponse<String> login = post("/api/v1/auth/login", "", "", "{}");
        assertAuthError(login, 403, "AUTH_CSRF_INVALID");
        assertThat(setCookies(login)).noneMatch(cookie -> cookie.startsWith("EDT_SESSION="));

        String csrf = bootstrapCsrf();
        HttpResponse<String> authenticated = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"organizer@demo.elitedevticket.local\",\"password\":\"password\"}");
        String session = cookieValue(authenticated, "EDT_SESSION");
        String rotatedCsrf = cookieValue(authenticated, "XSRF-TOKEN");

        HttpResponse<String> logout = post(
                "/api/v1/auth/logout",
                "EDT_SESSION=" + session + "; " + csrfCookie(rotatedCsrf),
                "wrong-csrf",
                null);
        assertAuthError(logout, 403, "AUTH_CSRF_INVALID");
        assertThat(setCookies(logout)).noneMatch(cookie -> cookie.startsWith("EDT_SESSION="));

        HttpResponse<String> current = getSession("EDT_SESSION=" + session);
        assertThat(current.body()).contains("\"authenticated\":true");
    }

    @Test
    void roleProtectedTestFixtureRejectsAnonymousAndIncompatibleRolesWithoutExecutingHandler() throws Exception {
        HttpResponse<String> anonymous = get("/test/rbac/organizer", "");

        assertAuthError(anonymous, 401, "AUTH_UNAUTHENTICATED");
        assertThat(RbacTestFixture.executions()).isZero();

        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        HttpResponse<String> forbidden = get("/test/rbac/organizer", "EDT_SESSION=" + customerSession);

        assertAuthError(forbidden, 403, "AUTH_FORBIDDEN");
        assertThat(RbacTestFixture.executions()).isZero();
    }

    @Test
    void roleProtectedTestFixtureAllowsMatchingRole() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String gateSession = loginSession("gate@demo.elitedevticket.local");

        HttpResponse<String> organizer = get("/test/rbac/organizer", "EDT_SESSION=" + organizerSession);
        HttpResponse<String> customer = get("/test/rbac/customer", "EDT_SESSION=" + customerSession);
        HttpResponse<String> gate = get("/test/rbac/gate", "EDT_SESSION=" + gateSession);

        assertThat(organizer.statusCode()).isEqualTo(200);
        assertThat(customer.statusCode()).isEqualTo(200);
        assertThat(gate.statusCode()).isEqualTo(200);
        assertThat(organizer.body()).isEqualTo("{\"result\":\"allowed\"}");
        assertThat(customer.body()).isEqualTo("{\"result\":\"allowed\"}");
        assertThat(gate.body()).isEqualTo("{\"result\":\"allowed\"}");
        assertThat(RbacTestFixture.executions()).isEqualTo(3);
    }

    @Test
    void corsAllowsOnlyConfiguredOriginWithCredentials() throws Exception {
        HttpResponse<String> allowed = getSessionWithOrigin("https://allowed.example");

        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("https://allowed.example");
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");

        HttpResponse<String> vercelAllowed = getSessionWithOrigin("https://ta-em-cartaz.vercel.app");
        assertThat(vercelAllowed.statusCode()).isEqualTo(200);
        assertThat(vercelAllowed.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("https://ta-em-cartaz.vercel.app");
        assertThat(vercelAllowed.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");

        HttpResponse<String> denied = getSessionWithOrigin("https://denied.example");
        assertThat(denied.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(denied.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
    }

    @Test
    void corsPreflightAllowsConfiguredLoginRequestWithCsrfHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Origin", "https://ta-em-cartaz.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("https://ta-em-cartaz.vercel.app");
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        assertThat(commaSeparatedHeader(response, "Access-Control-Allow-Methods"))
                .contains("get", "post", "put", "delete", "options");
        assertThat(commaSeparatedHeader(response, "Access-Control-Allow-Headers"))
                .contains("content-type", "x-xsrf-token");
    }

    private String bootstrapCsrf() throws Exception {
        return cookieValue(getSession(""), "XSRF-TOKEN");
    }

    private HttpResponse<String> getSession(String cookie) throws Exception {
        return get("/api/v1/auth/session", cookie);
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loginSession(String email) throws Exception {
        String csrf = bootstrapCsrf();
        HttpResponse<String> login = post("/api/v1/auth/login", csrfCookie(csrf), csrf,
                "{\"email\":\"" + email + "\",\"password\":\"password\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        return cookieValue(login, "EDT_SESSION");
    }

    private HttpResponse<String> getSessionWithOrigin(String origin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/auth/session"))
                .header("Origin", origin)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String cookie, String csrf, String body) throws Exception {
        return postWithContentType(path, cookie, csrf, body, body == null ? null : "application/json");
    }

    private HttpResponse<String> postWithContentType(
            String path,
            String cookie,
            String csrf,
            String body,
            String contentType) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).POST(publisher);
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (!csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertAuthError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.body())
                .contains("\"code\":\"" + code + "\"")
                .contains("\"message\":", "\"traceId\":", "\"timestamp\":")
                .doesNotContain("\"error\":", "\"path\":", "\"status\":");
    }

    private long jwtEpochClaim(String jwt, String claim) {
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        var matcher = Pattern.compile("\\\"" + claim + "\\\":(\\d+)").matcher(payload);
        assertThat(matcher.find()).as("claim JWT %s", claim).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private long cookieMaxAge(String cookie) {
        var matcher = Pattern.compile("(?:^|; )Max-Age=(\\d+)(?:;|$)").matcher(cookie);
        assertThat(matcher.find()).as("atributo Max-Age").isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String csrfCookie(String csrf) {
        return "XSRF-TOKEN=" + csrf;
    }

    private List<String> setCookies(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie");
    }

    private List<String> commaSeparatedHeader(HttpResponse<String> response, String name) {
        return response.headers().allValues(name).stream()
                .flatMap(value -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }

    private String cookieValue(HttpResponse<String> response, String name) {
        return setCookies(response).stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .reduce((first, second) -> second)
                .map(cookie -> cookie.substring(name.length() + 1, cookie.indexOf(';')))
                .orElseThrow();
    }
}
