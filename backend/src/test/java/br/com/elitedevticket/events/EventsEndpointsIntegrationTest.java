package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
    void listMyEventsReturnsOnlyOrganizerEvents() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Customer cannot list organizer events
        HttpResponse<String> customerListResponse = get("/api/v1/events/mine", "EDT_SESSION=" + customerSession);
        assertThat(customerListResponse.statusCode()).isEqualTo(403);

        // Organizer lists events
        post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Meu Evento Para Listagem\"}"
        );

        HttpResponse<String> listResponse = get("/api/v1/events/mine", "EDT_SESSION=" + organizerSession);
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body())
                .contains("\"events\":")
                .contains("\"title\":\"Meu Evento Para Listagem\"");
    }

    @Test
    void updateDraftEventUpdatesFieldsAndValidatesState() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Create draft
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Título Inicial\"}"
        );
        String location = createResponse.headers().firstValue("Location").get();

        // Update without CSRF fails 403
        HttpResponse<String> noCsrfUpdate = put(
                location,
                "EDT_SESSION=" + organizerSession,
                "",
                "{\"title\":\"Sem CSRF\"}"
        );
        assertThat(noCsrfUpdate.statusCode()).isEqualTo(403);

        // Customer cannot update draft (403)
        HttpResponse<String> customerUpdate = put(
                location,
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Tentativa de Cliente\"}"
        );
        assertThat(customerUpdate.statusCode()).isEqualTo(403);

        // Blank title fails 400
        HttpResponse<String> blankTitleUpdate = put(
                location,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"   \"}"
        );
        assertThat(blankTitleUpdate.statusCode()).isEqualTo(400);
        assertThat(blankTitleUpdate.body()).contains("\"code\":\"AUTH_INVALID_REQUEST\"");

        // Successful update 200
        String updatePayload = """
                {
                  "title": "Título Atualizado",
                  "description": "Nova descrição",
                  "imageUrl": "https://images.example.com/updated.jpg",
                  "category": "Jazz",
                  "venueName": "Teatro Central",
                  "venueAddress": "Rua Central, 100, São Paulo - SP",
                  "startsAt": "2026-10-01T19:30:00Z"
                }
                """;
        HttpResponse<String> successUpdate = put(
                location,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                updatePayload
        );
        assertThat(successUpdate.statusCode()).isEqualTo(200);
        assertThat(successUpdate.body())
                .contains("\"title\":\"Título Atualizado\"")
                .contains("\"description\":\"Nova descrição\"")
                .contains("\"imageUrl\":\"https://images.example.com/updated.jpg\"")
                .contains("\"category\":\"Jazz\"")
                .contains("\"venueName\":\"Teatro Central\"")
                .contains("\"venueAddress\":\"Rua Central, 100, São Paulo - SP\"")
                .contains("\"startsAt\":\"2026-10-01T19:30:00Z\"");
    }

    @Test
    void deleteDraftEventDeletesEventAndPreventsUnauthorizedAccess() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Create draft
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Evento Para Exclusão\"}"
        );
        String location = createResponse.headers().firstValue("Location").get();

        // Customer cannot delete (403)
        HttpResponse<String> customerDelete = delete(
                location,
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(customerDelete.statusCode()).isEqualTo(403);

        // Delete without CSRF fails (403)
        HttpResponse<String> noCsrfDelete = delete(
                location,
                "EDT_SESSION=" + organizerSession,
                ""
        );
        assertThat(noCsrfDelete.statusCode()).isEqualTo(403);

        // Successful delete (204)
        HttpResponse<String> successDelete = delete(
                location,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(successDelete.statusCode()).isEqualTo(204);

        // Subsequent GET returns 404
        HttpResponse<String> getDeleted = get(location, "EDT_SESSION=" + organizerSession);
        assertThat(getDeleted.statusCode()).isEqualTo(404);
        assertThat(getDeleted.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");
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

    @Test
    void organizerCanCreateListUpdateAndDeleteTicketSectorsOnDraftEvent() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Cria evento rascunho
        HttpResponse<String> eventResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Festival de Ingressos\"}"
        );
        assertThat(eventResponse.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(eventResponse.body(), "id");

        // 2. Adiciona setor Pista
        String createPistaPayload = """
                {
                  "name": "Pista Comum",
                  "description": "Acesso à pista geral",
                  "capacity": 500,
                  "price": 120.50
                }
                """;
        HttpResponse<String> pistaResponse = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                createPistaPayload
        );
        assertThat(pistaResponse.statusCode()).isEqualTo(201);
        assertThat(pistaResponse.body())
                .contains("\"name\":\"Pista Comum\"")
                .contains("\"capacity\":500")
                .contains("\"availableQuantity\":500")
                .contains("\"price\":120.5");
        String pistaId = extractJsonField(pistaResponse.body(), "id");

        // 3. Adiciona setor Camarote
        String createCamarotePayload = """
                {
                  "name": "Camarote VIP",
                  "description": "Open bar e vista privilegiada",
                  "capacity": 80,
                  "price": 350.00
                }
                """;
        HttpResponse<String> camaroteResponse = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                createCamarotePayload
        );
        assertThat(camaroteResponse.statusCode()).isEqualTo(201);
        String camaroteId = extractJsonField(camaroteResponse.body(), "id");

        // 4. Lista setores do evento
        HttpResponse<String> listResponse = get(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession
        );
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body())
                .contains("\"name\":\"Pista Comum\"")
                .contains("\"name\":\"Camarote VIP\"")
                .contains("\"capacity\":500")
                .contains("\"capacity\":80");

        // 5. Atualiza setor Pista
        String updatePistaPayload = """
                {
                  "name": "Pista Premium",
                  "description": "Pista com entrada prioritária",
                  "capacity": 600,
                  "price": 150.00
                }
                """;
        HttpResponse<String> updateResponse = put(
                "/api/v1/events/" + eventId + "/sectors/" + pistaId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                updatePistaPayload
        );
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.body())
                .contains("\"name\":\"Pista Premium\"")
                .contains("\"capacity\":600")
                .contains("\"availableQuantity\":600")
                .contains("\"price\":150");

        // 6. Exclui setor Camarote
        HttpResponse<String> deleteResponse = delete(
                "/api/v1/events/" + eventId + "/sectors/" + camaroteId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(204);

        // 7. Lista novamente e confirma remoção
        HttpResponse<String> listAfterDelete = get(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession
        );
        assertThat(listAfterDelete.statusCode()).isEqualTo(200);
        assertThat(listAfterDelete.body()).contains("\"name\":\"Pista Premium\"");
        assertThat(listAfterDelete.body()).doesNotContain("Camarote VIP");
    }

    @Test
    void createSectorValidationRejectsInvalidCapacityAndPrice() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        HttpResponse<String> eventResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Evento para Teste de Validação\"}"
        );
        assertThat(eventResponse.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(eventResponse.body(), "id");

        // Capacidade <= 0
        HttpResponse<String> invalidCapacity = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Setor Inválido\",\"capacity\":0,\"price\":50.00}"
        );
        assertThat(invalidCapacity.statusCode()).isEqualTo(400);
        assertThat(invalidCapacity.body())
                .contains("\"code\":\"AUTH_INVALID_REQUEST\"")
                .contains("\"field\":\"capacity\"");

        // Preço negativo
        HttpResponse<String> invalidPrice = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Setor Inválido\",\"capacity\":100,\"price\":-10.00}"
        );
        assertThat(invalidPrice.statusCode()).isEqualTo(400);
        assertThat(invalidPrice.body())
                .contains("\"code\":\"AUTH_INVALID_REQUEST\"")
                .contains("\"field\":\"price\"");

        // Nome em branco
        HttpResponse<String> blankName = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"   \",\"capacity\":100,\"price\":50.00}"
        );
        assertThat(blankName.statusCode()).isEqualTo(400);
        assertThat(blankName.body())
                .contains("\"code\":\"AUTH_INVALID_REQUEST\"")
                .contains("\"field\":\"name\"");
    }

    @Test
    void sectorOperationsRequireAuthenticationAndOwnership() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        HttpResponse<String> eventResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Evento Restrito\"}"
        );
        String eventId = extractJsonField(eventResponse.body(), "id");

        // Customer tentando criar setor -> 403
        HttpResponse<String> customerCreate = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Setor do Cliente\",\"capacity\":100,\"price\":50.00}"
        );
        assertThat(customerCreate.statusCode()).isEqualTo(403);
        assertThat(customerCreate.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");

        // Sem CSRF na criação -> 403 CSRF
        HttpResponse<String> noCsrf = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession,
                "",
                "{\"name\":\"Setor Sem CSRF\",\"capacity\":100,\"price\":50.00}"
        );
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        assertThat(noCsrf.body()).contains("\"code\":\"AUTH_CSRF_INVALID\"");

        // Evento inexistente -> 404
        HttpResponse<String> notFoundEvent = post(
                "/api/v1/events/" + UUID.randomUUID() + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Setor\",\"capacity\":100,\"price\":50.00}"
        );
        assertThat(notFoundEvent.statusCode()).isEqualTo(404);
        assertThat(notFoundEvent.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");
    }

    @Test
    void organizerCanPublishValidDraftEventSuccessfullyAndCannotRepublish() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar draft
        String createDraftPayload = """
                {
                  "externalId": "tm-pub-1",
                  "title": "Festival de Publicação",
                  "description": "Descrição do festival",
                  "imageUrl": "https://images.example.com/pub.jpg",
                  "category": "Música"
                }
                """;
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                createDraftPayload
        );
        assertThat(createResponse.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(createResponse.body(), "id");

        // 2. Atualizar draft com local e data futura válida
        String updatePayload = """
                {
                  "title": "Festival de Publicação Oficial",
                  "venueName": "Allianz Parque",
                  "venueAddress": "Av. Francisco Matarazzo, 1705, São Paulo - SP",
                  "startsAt": "2026-11-20T21:00:00Z"
                }
                """;
        HttpResponse<String> updateResponse = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                updatePayload
        );
        assertThat(updateResponse.statusCode()).isEqualTo(200);

        // 3. Tentar publicar sem setores -> 400
        HttpResponse<String> publishWithoutSectors = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishWithoutSectors.statusCode()).isEqualTo(400);
        assertThat(publishWithoutSectors.body()).contains("\"code\":\"AUTH_INVALID_REQUEST\"");

        // 4. Adicionar setores válidos
        String sectorPayload = """
                {
                  "name": "Pista Premium",
                  "description": "Acesso frontal ao palco",
                  "capacity": 300,
                  "price": 250.00
                }
                """;
        HttpResponse<String> sectorResponse = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                sectorPayload
        );
        assertThat(sectorResponse.statusCode()).isEqualTo(201);

        // 5. Publicar evento com sucesso -> 200 PUBLISHED
        HttpResponse<String> publishResponse = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishResponse.statusCode()).isEqualTo(200);
        assertThat(publishResponse.body()).contains("\"status\":\"PUBLISHED\"");
        assertThat(publishResponse.body()).contains("\"title\":\"Festival de Publicação Oficial\"");
        assertThat(publishResponse.body()).contains("\"venueName\":\"Allianz Parque\"");
        assertThat(publishResponse.body()).contains("\"venueAddress\":\"Av. Francisco Matarazzo, 1705, São Paulo - SP\"");

        // 6. Consultar evento -> deve estar PUBLISHED
        HttpResponse<String> getEventResponse = get(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession
        );
        assertThat(getEventResponse.statusCode()).isEqualTo(200);
        assertThat(getEventResponse.body()).contains("\"status\":\"PUBLISHED\"");

        // 7. Consultar setores -> disponibilidade inicial preservada
        HttpResponse<String> getSectorsResponse = get(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession
        );
        assertThat(getSectorsResponse.statusCode()).isEqualTo(200);
        assertThat(getSectorsResponse.body()).contains("\"availableQuantity\":300");

        // 8. Tentar publicar novamente evento já PUBLISHED -> 409 Conflict (sem duplicação de efeitos)
        HttpResponse<String> republishResponse = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(republishResponse.statusCode()).isEqualTo(409);
        assertThat(republishResponse.body()).contains("\"code\":\"EVENT_CANNOT_BE_MODIFIED\"");

        // 9. Tentar excluir evento publicado -> 409 Conflict
        HttpResponse<String> deleteResponse = delete(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(409);
        assertThat(deleteResponse.body()).contains("\"code\":\"EVENT_CANNOT_BE_DELETED\"");
    }

    @Test
    void cannotPublishEventWithPastDateOrMissingVenue() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Criar draft
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"externalId\":\"tm-past\",\"title\":\"Evento Passado\"}"
        );
        String eventId = extractJsonField(createResponse.body(), "id");

        // Adicionar setor
        post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Pista\",\"capacity\":100,\"price\":50.00}"
        );

        // Atualizar com data passada
        put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Evento Passado\",\"venueName\":\"Local\",\"venueAddress\":\"Endereço\",\"startsAt\":\"2020-01-01T20:00:00Z\"}"
        );

        // Tentar publicar -> 400 Bad Request
        HttpResponse<String> publishPast = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishPast.statusCode()).isEqualTo(400);
        assertThat(publishPast.body()).contains("\"code\":\"AUTH_INVALID_REQUEST\"");
    }

    @Test
    void publishSecurityAndOwnershipGuardsEnforced() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar draft pelo organizer
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"externalId\":\"tm-sec\",\"title\":\"Evento Segurança\"}"
        );
        String eventId = extractJsonField(createResponse.body(), "id");

        // 2. Anônimo tentando publicar -> 401
        HttpResponse<String> anonPublish = post(
                "/api/v1/events/" + eventId + "/publish",
                "XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(anonPublish.statusCode()).isEqualTo(401);
        assertThat(anonPublish.body()).contains("\"code\":\"AUTH_UNAUTHENTICATED\"");

        // 3. Customer tentando publicar -> 403
        HttpResponse<String> custPublish = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(custPublish.statusCode()).isEqualTo(403);
        assertThat(custPublish.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");

        // 4. Sem CSRF -> 403
        HttpResponse<String> noCsrfPublish = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession,
                "",
                ""
        );
        assertThat(noCsrfPublish.statusCode()).isEqualTo(403);
        assertThat(noCsrfPublish.body()).contains("\"code\":\"AUTH_CSRF_INVALID\"");

        // 5. Evento inexistente -> 404
        HttpResponse<String> notFoundPublish = post(
                "/api/v1/events/" + UUID.randomUUID() + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(notFoundPublish.statusCode()).isEqualTo(404);
        assertThat(notFoundPublish.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");
    }

    @Test
    void organizerCanUpdateNonStructuralFieldsOfPublishedEventAndCannotMutateStructuralFields() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Create draft event
        HttpResponse<String> draftResp = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "externalId": "tm-published-edit-1",
                  "title": "Festival de Jazz 2026",
                  "description": "Descrição inicial",
                  "imageUrl": "https://img.com/jazz.jpg",
                  "category": "Jazz"
                }
                """
        );
        assertThat(draftResp.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(draftResp.body(), "id");

        // 2. Set venue and future startsAt
        HttpResponse<String> updateResp = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Festival de Jazz 2026",
                  "venueName": "Auditório Ibirapuera",
                  "venueAddress": "Av. Pedro Álvares Cabral, São Paulo",
                  "startsAt": "2026-11-20T21:00:00Z"
                }
                """
        );
        assertThat(updateResp.statusCode()).isEqualTo(200);

        // 3. Add a ticket sector
        HttpResponse<String> sectorResp = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Plateia",
                  "capacity": 200,
                  "price": 120.00
                }
                """
        );
        assertThat(sectorResp.statusCode()).isEqualTo(201);

        // 4. Publish event
        HttpResponse<String> publishResp = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishResp.statusCode()).isEqualTo(200);
        assertThat(publishResp.body()).contains("\"status\":\"PUBLISHED\"");

        // 5. Attempt to change structural fields (title, venueName, venueAddress, startsAt) -> 409
        HttpResponse<String> badTitle = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Festival de Jazz Alterado",
                  "venueName": "Auditório Ibirapuera",
                  "venueAddress": "Av. Pedro Álvares Cabral, São Paulo",
                  "startsAt": "2026-11-20T21:00:00Z"
                }
                """
        );
        assertThat(badTitle.statusCode()).isEqualTo(409);
        assertThat(badTitle.body()).contains("\"code\":\"EVENT_CANNOT_BE_MODIFIED\"");

        HttpResponse<String> badVenue = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Festival de Jazz 2026",
                  "venueName": "Outro Local",
                  "venueAddress": "Av. Pedro Álvares Cabral, São Paulo",
                  "startsAt": "2026-11-20T21:00:00Z"
                }
                """
        );
        assertThat(badVenue.statusCode()).isEqualTo(409);
        assertThat(badVenue.body()).contains("\"code\":\"EVENT_CANNOT_BE_MODIFIED\"");

        // 6. Update non-structural fields (description, imageUrl, category) -> 200 OK
        HttpResponse<String> goodUpdate = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Festival de Jazz 2026",
                  "venueName": "Auditório Ibirapuera",
                  "venueAddress": "Av. Pedro Álvares Cabral, São Paulo",
                  "startsAt": "2026-11-20T21:00:00Z",
                  "description": "Nova descrição enriquecida com line-up",
                  "imageUrl": "https://img.com/jazz-new.jpg",
                  "category": "Jazz & Blues"
                }
                """
        );
        assertThat(goodUpdate.statusCode()).isEqualTo(200);
        assertThat(goodUpdate.body()).contains("\"description\":\"Nova descrição enriquecida com line-up\"");
        assertThat(goodUpdate.body()).contains("\"imageUrl\":\"https://img.com/jazz-new.jpg\"");
        assertThat(goodUpdate.body()).contains("\"category\":\"Jazz & Blues\"");
        assertThat(goodUpdate.body()).contains("\"title\":\"Festival de Jazz 2026\"");

        // 7. Deletion is forbidden -> 409
        HttpResponse<String> deleteResp = delete(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(deleteResp.statusCode()).isEqualTo(409);
        assertThat(deleteResp.body()).contains("\"code\":\"EVENT_CANNOT_BE_DELETED\"");
    }

    @Test
    void organizerCanManagePublishedTicketSectorsPreservingCommittedQuantity() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Create and configure event
        HttpResponse<String> draftResp = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "externalId": "tm-published-sec-1",
                  "title": "Concerto Clássico",
                  "description": "Música clássica",
                  "category": "Clássica"
                }
                """
        );
        String eventId = extractJsonField(draftResp.body(), "id");

        put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Concerto Clássico",
                  "venueName": "Sala São Paulo",
                  "venueAddress": "Praça Júlio Prestes, 16",
                  "startsAt": "2026-12-01T20:00:00Z"
                }
                """
        );

        HttpResponse<String> sector1Resp = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Plateia Central",
                  "capacity": 100,
                  "price": 150.00
                }
                """
        );
        String sector1Id = extractJsonField(sector1Resp.body(), "id");

        // 2. Publish event
        post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );

        // 3. Update capacity of published sector (increase 100 -> 180) and price (150.00 -> 175.00)
        HttpResponse<String> updateSec = put(
                "/api/v1/events/" + eventId + "/sectors/" + sector1Id,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Plateia Central VIP",
                  "capacity": 180,
                  "price": 175.00
                }
                """
        );
        assertThat(updateSec.statusCode()).isEqualTo(200);
        assertThat(updateSec.body()).contains("\"name\":\"Plateia Central VIP\"");
        assertThat(updateSec.body()).contains("\"capacity\":180");
        assertThat(updateSec.body()).contains("\"availableQuantity\":180");
        assertThat(updateSec.body()).contains("\"price\":175.0");

        // 4. Create new sector on PUBLISHED event
        HttpResponse<String> newSector = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Camarote Superior",
                  "capacity": 40,
                  "price": 250.00
                }
                """
        );
        assertThat(newSector.statusCode()).isEqualTo(201);
        String sector2Id = extractJsonField(newSector.body(), "id");

        // 5. Delete uncommitted sector on PUBLISHED event -> 204
        HttpResponse<String> delSec = delete(
                "/api/v1/events/" + eventId + "/sectors/" + sector2Id,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(delSec.statusCode()).isEqualTo(204);

        // 6. Simulate committed tickets (simulate available_quantity = 140 out of 180, committed = 40)
        jdbcTemplate.update(
                "UPDATE ticket_sectors SET available_quantity = 140 WHERE id = ?",
                UUID.fromString(sector1Id)
        );

        // 7. Try to decrease capacity below committed (e.g. newCapacity = 35 < 40) -> 409
        HttpResponse<String> badCapacity = put(
                "/api/v1/events/" + eventId + "/sectors/" + sector1Id,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Plateia Central VIP",
                  "capacity": 35,
                  "price": 175.00
                }
                """
        );
        assertThat(badCapacity.statusCode()).isEqualTo(409);
        assertThat(badCapacity.body()).contains("\"code\":\"EVENT_CANNOT_BE_MODIFIED\"");

        // 8. Decrease capacity to valid value (e.g. newCapacity = 50 >= 40 committed) -> 200 OK
        // newAvailable should be 50 - 40 = 10
        HttpResponse<String> validDecrease = put(
                "/api/v1/events/" + eventId + "/sectors/" + sector1Id,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "name": "Plateia Central VIP",
                  "capacity": 50,
                  "price": 175.00
                }
                """
        );
        assertThat(validDecrease.statusCode()).isEqualTo(200);
        assertThat(validDecrease.body()).contains("\"capacity\":50");
        assertThat(validDecrease.body()).contains("\"availableQuantity\":10");

        // 9. Attempt to delete committed sector -> 409
        HttpResponse<String> delCommitted = delete(
                "/api/v1/events/" + eventId + "/sectors/" + sector1Id,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf
        );
        assertThat(delCommitted.statusCode()).isEqualTo(409);
        assertThat(delCommitted.body()).contains("\"code\":\"EVENT_CANNOT_BE_MODIFIED\"");
    }

    @Test
    void databaseIntegrityConstraintsAreEnforcedByPostgres() {
        UUID organizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE role = 'ORGANIZER' LIMIT 1",
                UUID.class
        );
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO events (id, organizer_id, external_source, external_id, title, status, created_at, updated_at)
                VALUES (?, ?, 'TICKETMASTER', 'tm-constraint-1', 'Evento Constraint', 'DRAFT', NOW(), NOW())
                """, eventId, organizerId);

        // 1. capacity <= 0 fails check constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO ticket_sectors (id, event_id, name, capacity, available_quantity, price, created_at, updated_at)
                    VALUES (?, ?, 'Invalido', 0, 0, 10.00, NOW(), NOW())
                    """, UUID.randomUUID(), eventId);
        });

        // 2. available_quantity < 0 fails check constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO ticket_sectors (id, event_id, name, capacity, available_quantity, price, created_at, updated_at)
                    VALUES (?, ?, 'Invalido', 100, -1, 10.00, NOW(), NOW())
                    """, UUID.randomUUID(), eventId);
        });

        // 3. available_quantity > capacity fails check constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO ticket_sectors (id, event_id, name, capacity, available_quantity, price, created_at, updated_at)
                    VALUES (?, ?, 'Invalido', 100, 101, 10.00, NOW(), NOW())
                    """, UUID.randomUUID(), eventId);
        });

        // 4. price < 0 fails check constraint
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO ticket_sectors (id, event_id, name, capacity, available_quantity, price, created_at, updated_at)
                    VALUES (?, ?, 'Invalido', 100, 100, -5.00, NOW(), NOW())
                    """, UUID.randomUUID(), eventId);
        });
    }

    private String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Field " + field + " not found in: " + json);
        }
        int valueStart = start + marker.length();
        int end = json.indexOf("\"", valueStart);
        return json.substring(valueStart, end);
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

    @Test
    void anonymousCanListPublicPublishedEventsAndSearchByTitle() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Create event 1
        String event1Payload = """
                {
                  "externalId": "tm-rock-1",
                  "externalSource": "TICKETMASTER",
                  "title": "Festival Primavera Rock 2026",
                  "category": "Rock",
                  "description": "Festival ao ar livre"
                }
                """;
        HttpResponse<String> create1Response = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                event1Payload
        );
        assertThat(create1Response.statusCode()).isEqualTo(201);
        UUID event1Id = UUID.fromString(extractJsonField(create1Response.body(), "id"));

        // Update event 1 with full details
        String update1Payload = """
                {
                  "title": "Festival Primavera Rock 2026",
                  "venueName": "Parque Ibirapuera",
                  "venueAddress": "Av. Pedro Álvares Cabral, s/n",
                  "startsAt": "2026-11-20T20:00:00Z"
                }
                """;
        put("/api/v1/events/" + event1Id, "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf, csrf, update1Payload);

        // Add sectors with different prices
        post(
                "/api/v1/events/" + event1Id + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Pista\",\"capacity\":500,\"price\":120.00}"
        );
        post(
                "/api/v1/events/" + event1Id + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Camarote\",\"capacity\":100,\"price\":350.00}"
        );

        // Publish event 1
        HttpResponse<String> publishResponse = post(
                "/api/v1/events/" + event1Id + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishResponse.statusCode()).isEqualTo(200);

        // 2. Create event 2 and leave as DRAFT
        String event2Payload = """
                {
                  "title": "Evento Rascunho Oculto",
                  "category": "Jazz"
                }
                """;
        HttpResponse<String> create2Response = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                event2Payload
        );
        assertThat(create2Response.statusCode()).isEqualTo(201);
        UUID event2Id = UUID.fromString(extractJsonField(create2Response.body(), "id"));

        // 3. Anonymous request to GET /api/v1/events
        HttpResponse<String> publicListResponse = get("/api/v1/events", "");
        assertThat(publicListResponse.statusCode()).isEqualTo(200);
        assertThat(publicListResponse.body()).contains("\"events\":[");
        assertThat(publicListResponse.body()).contains(event1Id.toString());
        assertThat(publicListResponse.body()).contains("Festival Primavera Rock 2026");
        assertThat(publicListResponse.body()).contains("\"startingPrice\":120.00");
        assertThat(publicListResponse.body()).contains("\"salesClosed\":false");
        assertThat(publicListResponse.body()).contains("\"status\":\"PUBLISHED\"");
        // DRAFT must NEVER be present
        assertThat(publicListResponse.body()).doesNotContain(event2Id.toString());
        assertThat(publicListResponse.body()).doesNotContain("Evento Rascunho Oculto");

        // 4. Search with matching keyword
        HttpResponse<String> searchMatchResponse = get("/api/v1/events?search=Primavera", "");
        assertThat(searchMatchResponse.statusCode()).isEqualTo(200);
        assertThat(searchMatchResponse.body()).contains(event1Id.toString());
        assertThat(searchMatchResponse.body()).contains("Festival Primavera Rock 2026");

        // 5. Search with non-matching keyword
        HttpResponse<String> searchNoMatchResponse = get("/api/v1/events?search=Inexistente123456", "");
        assertThat(searchNoMatchResponse.statusCode()).isEqualTo(200);
        assertThat(searchNoMatchResponse.body()).isEqualTo("{\"events\":[]}");
    }

    @Test
    void publicEventDetailAndSectorsAccessFlow() throws Exception {
        String csrf = bootstrapCsrf();
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");

        // 1. Criar e publicar um evento com setores
        String eventPayload = """
                {
                  "externalId": "tm-gastro-1",
                  "externalSource": "TICKETMASTER",
                  "title": "Festival Gastronômico 2026",
                  "category": "Gastronomia",
                  "description": "O melhor da gastronomia regional e internacional",
                  "imageUrl": "https://example.com/gastronomia.jpg"
                }
                """;
        HttpResponse<String> createResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                eventPayload
        );
        assertThat(createResponse.statusCode()).isEqualTo(201);
        UUID publishedEventId = UUID.fromString(extractJsonField(createResponse.body(), "id"));

        put(
                "/api/v1/events/" + publishedEventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Festival Gastronômico 2026",
                  "venueName": "Memorial da América Latina",
                  "venueAddress": "Av. Auro Soares de Moura Andrade, 664",
                  "startsAt": "2026-12-10T18:00:00Z"
                }
                """
        );

        post(
                "/api/v1/events/" + publishedEventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Entrada Geral\",\"description\":\"Acesso a todos os quiosques\",\"capacity\":1000,\"price\":80.00}"
        );
        post(
                "/api/v1/events/" + publishedEventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Área VIP Degustação\",\"description\":\"Área coberta com degustação inclusa\",\"capacity\":200,\"price\":250.00}"
        );

        post(
                "/api/v1/events/" + publishedEventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );

        // 2. Criar um evento DRAFT que não será publicado
        HttpResponse<String> draftCreateResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Evento Exclusivo Secreto DRAFT\"}"
        );
        UUID draftEventId = UUID.fromString(extractJsonField(draftCreateResponse.body(), "id"));

        post(
                "/api/v1/events/" + draftEventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Setor Secreto\",\"capacity\":50,\"price\":500.00}"
        );

        // 3. Consulta pública anônima do evento PUBLISHED -> 200 OK
        HttpResponse<String> anonEventResponse = get("/api/v1/events/" + publishedEventId, "");
        assertThat(anonEventResponse.statusCode()).isEqualTo(200);
        assertThat(anonEventResponse.body()).contains("\"title\":\"Festival Gastronômico 2026\"");
        assertThat(anonEventResponse.body()).contains("\"venueName\":\"Memorial da América Latina\"");
        assertThat(anonEventResponse.body()).contains("\"venueAddress\":\"Av. Auro Soares de Moura Andrade, 664\"");
        assertThat(anonEventResponse.body()).contains("\"status\":\"PUBLISHED\"");

        // 4. Consulta pública anônima dos setores do evento PUBLISHED -> 200 OK
        HttpResponse<String> anonSectorsResponse = get("/api/v1/events/" + publishedEventId + "/sectors", "");
        assertThat(anonSectorsResponse.statusCode()).isEqualTo(200);
        assertThat(anonSectorsResponse.body()).contains("\"name\":\"Entrada Geral\"");
        assertThat(anonSectorsResponse.body()).contains("\"availableQuantity\":1000");
        assertThat(anonSectorsResponse.body()).contains("\"price\":80.00");
        assertThat(anonSectorsResponse.body()).contains("\"name\":\"Área VIP Degustação\"");
        assertThat(anonSectorsResponse.body()).contains("\"availableQuantity\":200");
        assertThat(anonSectorsResponse.body()).contains("\"price\":250.00");

        // 5. Tentativa anônima de consultar evento DRAFT -> 403 Forbidden
        HttpResponse<String> anonDraftResponse = get("/api/v1/events/" + draftEventId, "");
        assertThat(anonDraftResponse.statusCode()).isEqualTo(403);
        assertThat(anonDraftResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");

        // 6. Tentativa anônima de consultar setores de evento DRAFT -> 403 Forbidden
        HttpResponse<String> anonDraftSectorsResponse = get("/api/v1/events/" + draftEventId + "/sectors", "");
        assertThat(anonDraftSectorsResponse.statusCode()).isEqualTo(403);
        assertThat(anonDraftSectorsResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");

        // 7. Tentativa de Customer consultar evento DRAFT -> 403 Forbidden
        HttpResponse<String> customerDraftResponse = get("/api/v1/events/" + draftEventId, "EDT_SESSION=" + customerSession);
        assertThat(customerDraftResponse.statusCode()).isEqualTo(403);
        assertThat(customerDraftResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");

        // 8. Consulta de evento inexistente -> 404 Not Found
        UUID randomId = UUID.randomUUID();
        HttpResponse<String> notFoundEventResponse = get("/api/v1/events/" + randomId, "");
        assertThat(notFoundEventResponse.statusCode()).isEqualTo(404);
        assertThat(notFoundEventResponse.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");

        HttpResponse<String> notFoundSectorsResponse = get("/api/v1/events/" + randomId + "/sectors", "");
        assertThat(notFoundSectorsResponse.statusCode()).isEqualTo(404);
        assertThat(notFoundSectorsResponse.body()).contains("\"code\":\"EVENT_NOT_FOUND\"");
    }

    @Test
    @DisplayName("Story 7.1 — Usuário com papel GATE lista apenas eventos PUBLISHED e recebe 403 ao acessar DRAFT")
    void gateUserListsOnlyPublishedEventsAndCannotAccessDrafts() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Create draft event
        HttpResponse<String> draftCreateResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"title\":\"Draft Event For Gate Test\"}"
        );
        assertThat(draftCreateResponse.statusCode()).isEqualTo(201);
        UUID draftEventId = UUID.fromString(extractJsonField(draftCreateResponse.body(), "id"));

        // Create published event
        HttpResponse<String> pubCreateResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Published Event For Gate Test",
                  "externalSource": "TICKETMASTER",
                  "externalId": "tm-gate-123"
                }
                """
        );
        assertThat(pubCreateResponse.statusCode()).isEqualTo(201);
        UUID pubEventId = UUID.fromString(extractJsonField(pubCreateResponse.body(), "id"));

        HttpResponse<String> updateResponse = put(
                "/api/v1/events/" + pubEventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                """
                {
                  "title": "Published Event For Gate Test",
                  "venueName": "Gate Arena",
                  "venueAddress": "Av. Portaria, 100",
                  "startsAt": "2026-12-01T20:00:00Z"
                }
                """
        );
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        HttpResponse<String> sectorResponse = post(
                "/api/v1/events/" + pubEventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"name\":\"Geral\",\"capacity\":200,\"price\":50.00}"
        );
        assertThat(sectorResponse.statusCode()).isEqualTo(201);
        HttpResponse<String> publishResponse = post(
                "/api/v1/events/" + pubEventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishResponse.statusCode()).isEqualTo(200);

        // Login as GATE
        String gateSession = loginSession("gate@demo.elitedevticket.local");

        // 1. GATE lists published events via GET /api/v1/events
        HttpResponse<String> gateEventsResponse = get("/api/v1/events", "EDT_SESSION=" + gateSession);
        assertThat(gateEventsResponse.statusCode()).isEqualTo(200);
        assertThat(gateEventsResponse.body()).contains("Published Event For Gate Test");
        assertThat(gateEventsResponse.body()).doesNotContain("Draft Event For Gate Test");

        // 2. GATE attempts to access DRAFT event directly -> 403 Forbidden
        HttpResponse<String> gateDraftResponse = get("/api/v1/events/" + draftEventId, "EDT_SESSION=" + gateSession);
        assertThat(gateDraftResponse.statusCode()).isEqualTo(403);
        assertThat(gateDraftResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");

        // 3. GATE attempts to access DRAFT sectors -> 403 Forbidden
        HttpResponse<String> gateDraftSectorsResponse = get("/api/v1/events/" + draftEventId + "/sectors", "EDT_SESSION=" + gateSession);
        assertThat(gateDraftSectorsResponse.statusCode()).isEqualTo(403);
        assertThat(gateDraftSectorsResponse.body()).contains("\"code\":\"EVENT_FORBIDDEN\"");

        // 4. GATE accesses published event directly -> 200 OK
        HttpResponse<String> gatePubResponse = get("/api/v1/events/" + pubEventId, "EDT_SESSION=" + gateSession);
        assertThat(gatePubResponse.statusCode()).isEqualTo(200);
        assertThat(gatePubResponse.body()).contains("Published Event For Gate Test");
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

    private HttpResponse<String> put(String path, String cookie, String csrf, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (!csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String cookie, String csrf) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .DELETE();
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
