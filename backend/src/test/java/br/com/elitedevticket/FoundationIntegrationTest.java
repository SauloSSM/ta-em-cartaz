package br.com.elitedevticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.elitedevticket.shared.configuration.MissingDatasourceConfigurationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIntegrationTest {
    private static final String TEST_JWT_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @LocalServerPort
    private int port;

    @Test
    void localProfileStartsAndSeedsOnlyTheApprovedUsers() {
        assertSeededProfile("local");
    }

    @Test
    void testProfileStartsAndSeedsOnlyTheApprovedUsers() {
        assertSeededProfile("test");
    }

    @Test
    void demoProfileStartsAndSeedsOnlyTheApprovedUsers() {
        assertSeededProfile("demo");
    }

    @Test
    void prodProfileStartsWithOnlyTheUserSchemaAndNoDemoAccounts() {
        String schema = startProfile("prod");

        assertApplicationTables(schema);
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".users", Integer.class);
        assertThat(users).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "prod,DATABASE_URL",
        "prod,DATABASE_USERNAME",
        "prod,DATABASE_PASSWORD",
        "demo,DATABASE_URL",
        "demo,DATABASE_USERNAME",
        "demo,DATABASE_PASSWORD"
    })
    void externallyConfiguredProfilesFailFastWhenARequiredSettingIsAbsent(
            String profile, String missingConfiguration) {
        Map<String, String> settings = databaseSettings("public");
        settings.remove(missingConfiguration);

        assertMissingDatasourceConfiguration(profile, missingConfiguration, settings);
    }

    @ParameterizedTest
    @CsvSource({
        "prod,DATABASE_URL",
        "prod,DATABASE_USERNAME",
        "prod,DATABASE_PASSWORD",
        "demo,DATABASE_URL",
        "demo,DATABASE_USERNAME",
        "demo,DATABASE_PASSWORD"
    })
    void externallyConfiguredProfilesFailFastWhenARequiredSettingIsBlank(
            String profile, String blankConfiguration) {
        Map<String, String> settings = databaseSettings("public");
        settings.put(blankConfiguration, "   ");

        assertMissingDatasourceConfiguration(profile, blankConfiguration, settings);
    }

    @Test
    void defaultProfileIsProdAndDoesNotSeedDemoAccounts() {
        String schema = "default_profile";
        jdbcTemplate.execute("CREATE SCHEMA " + schema);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EliteDevTicketApplication.class)
                .web(WebApplicationType.NONE)
                .run(commandLineArguments(databaseSettings(schema)))) {
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(context.getEnvironment().getDefaultProfiles()).containsExactly("prod");
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        }

        assertApplicationTables(schema);
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".users", Integer.class);
        assertThat(users).isZero();
    }

    @Test
    void livenessAndReadinessExposeOnlyStatusAndReadinessIncludesDatabaseAndFlyway() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String liveness = healthResponse(client, "liveness");
        String readiness = healthResponse(client, "readiness");

        assertThat(liveness).isEqualTo("{\"status\":\"UP\"}");
        assertThat(readiness).isEqualTo("{\"status\":\"UP\"}");
        assertThat(environment.getProperty("management.endpoint.health.group.readiness.include"))
                .contains("db", "flyway");
    }

    private String healthResponse(HttpClient client, String probe) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health/" + probe))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private void assertSeededProfile(String profile) {
        String schema = startProfile(profile);

        assertApplicationTables(schema);
        List<SeededUser> users = jdbcTemplate.query(
                "SELECT email, role, password_hash FROM " + schema + ".users",
                (resultSet, rowNumber) -> new SeededUser(
                        resultSet.getString("email"),
                        resultSet.getString("role"),
                        resultSet.getString("password_hash")));
        assertThat(users)
                .extracting(SeededUser::email, SeededUser::role)
                .containsExactlyInAnyOrder(
                        tuple("organizer@demo.elitedevticket.local", "ORGANIZER"),
                        tuple("customer.one@demo.elitedevticket.local", "CUSTOMER"),
                        tuple("customer.two@demo.elitedevticket.local", "CUSTOMER"),
                        tuple("gate@demo.elitedevticket.local", "GATE"));
        assertThat(users).allSatisfy(user -> assertThat(BCrypt.checkpw("password", user.passwordHash())).isTrue());
    }

    private String startProfile(String profile) {
        String schema = profile + "_profile";
        jdbcTemplate.execute("CREATE SCHEMA " + schema);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EliteDevTicketApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(profile)
                .run(commandLineArguments(databaseSettings(schema)))) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly(profile);
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        }
        return schema;
    }

    private void assertMissingDatasourceConfiguration(
            String profile, String configurationName, Map<String, String> settings) {
        StandardEnvironment isolatedEnvironment = new StandardEnvironment();
        isolatedEnvironment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        isolatedEnvironment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

        assertThatThrownBy(() -> new SpringApplicationBuilder(EliteDevTicketApplication.class)
                        .environment(isolatedEnvironment)
                        .web(WebApplicationType.NONE)
                        .profiles(profile)
                        .run(commandLineArguments(settings)))
                .isExactlyInstanceOf(MissingDatasourceConfigurationException.class)
                .hasMessage("Missing required configuration: " + configurationName)
                .satisfies(exception -> assertThat(exception.getMessage())
                        .doesNotContain(POSTGRES.getPassword()));
    }

    private Map<String, String> databaseSettings(String schema) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("DATABASE_URL", databaseUrlFor(schema));
        settings.put("DATABASE_USERNAME", POSTGRES.getUsername());
        settings.put("DATABASE_PASSWORD", POSTGRES.getPassword());
        settings.put("AUTH_JWT_SECRET", TEST_JWT_SECRET);
        return settings;
    }

    private String[] commandLineArguments(Map<String, String> settings) {
        return settings.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private void assertApplicationTables(String schema) {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = ? AND table_type = 'BASE TABLE' "
                        + "AND table_name <> 'flyway_schema_history'",
                String.class,
                schema);
        assertThat(tables).containsExactly("users");
    }

    private String databaseUrlFor(String schema) {
        return POSTGRES.getJdbcUrl() + "&currentSchema=" + schema;
    }

    private record SeededUser(String email, String role, String passwordHash) {
    }
}
