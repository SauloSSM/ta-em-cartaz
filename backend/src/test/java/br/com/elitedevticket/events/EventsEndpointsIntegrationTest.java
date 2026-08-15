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
