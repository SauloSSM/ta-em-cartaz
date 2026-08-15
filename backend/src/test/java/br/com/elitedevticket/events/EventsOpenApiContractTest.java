package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.http.CreateDraftEventRequest;
import br.com.elitedevticket.events.http.CreateTicketSectorRequest;
import br.com.elitedevticket.events.http.EventApiErrorResponse;
import br.com.elitedevticket.events.http.EventErrorCode;
import br.com.elitedevticket.events.http.EventListResponse;
import br.com.elitedevticket.events.http.EventResponse;
import br.com.elitedevticket.events.http.EventsController;
import br.com.elitedevticket.events.http.TicketSectorListResponse;
import br.com.elitedevticket.events.http.TicketSectorResponse;
import br.com.elitedevticket.events.http.UpdateDraftEventRequest;
import br.com.elitedevticket.events.http.UpdateTicketSectorRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

class EventsOpenApiContractTest {

    private static final Map<String, Class<?>> REFERENCED_TYPES = Map.ofEntries(
            Map.entry("EventStatus", EventStatus.class),
            Map.entry("CreateDraftEventRequest", CreateDraftEventRequest.class),
            Map.entry("UpdateDraftEventRequest", UpdateDraftEventRequest.class),
            Map.entry("EventResponse", EventResponse.class),
            Map.entry("EventListResponse", EventListResponse.class),
            Map.entry("TicketSectorResponse", TicketSectorResponse.class),
            Map.entry("TicketSectorListResponse", TicketSectorListResponse.class),
            Map.entry("CreateTicketSectorRequest", CreateTicketSectorRequest.class),
            Map.entry("UpdateTicketSectorRequest", UpdateTicketSectorRequest.class),
            Map.entry("EventErrorCode", EventErrorCode.class),
            Map.entry("EventApiError", EventApiErrorResponse.class),
            Map.entry("FieldError", FieldErrorResponse.class));

    @Test
    void javaEventsDtosAndOperationsRemainStructurallyAlignedWithTheVersionedOpenApi() throws IOException {
        assertControllerMappings();
        Map<String, Object> contract = loadContract();
        Map<String, Object> paths = map(contract.get("paths"));

        // POST /api/v1/events/drafts
        Map<String, Object> createDraft = assertOperation(
                paths, "/api/v1/events/drafts", "post", "createDraftEvent", Set.of("201", "400", "401", "403"));
        assertRequestSchema(createDraft, "CreateDraftEventRequest");
        assertSecurity(createDraft, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseSchema(createDraft, "201", "EventResponse");
        assertResponseReference(createDraft, "400", "AuthInvalidRequest");
        assertResponseReference(createDraft, "401", "AuthUnauthenticated");
        assertResponseReference(createDraft, "403", "AuthForbidden");

        // GET /api/v1/events/mine
        Map<String, Object> listMine = assertOperation(
                paths, "/api/v1/events/mine", "get", "listMyEvents", Set.of("200", "401", "403"));
        assertThat(listMine).doesNotContainKey("requestBody");
        assertSecurity(listMine, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(listMine, "200", "EventListResponse");
        assertResponseReference(listMine, "401", "AuthUnauthenticated");
        assertResponseReference(listMine, "403", "AuthForbidden");

        // GET /api/v1/events/{id}
        Map<String, Object> getEvent = assertOperation(
                paths, "/api/v1/events/{id}", "get", "getEvent", Set.of("200", "401", "403", "404"));
        assertThat(getEvent).doesNotContainKey("requestBody");
        assertSecurity(getEvent, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(getEvent, "200", "EventResponse");
        assertResponseReference(getEvent, "401", "AuthUnauthenticated");
        assertResponseReference(getEvent, "403", "AuthForbidden");
        assertResponseReference(getEvent, "404", "EventNotFound");

        // PUT /api/v1/events/{id}
        Map<String, Object> updateDraft = assertOperation(
                paths, "/api/v1/events/{id}", "put", "updateDraftEvent", Set.of("200", "400", "401", "403", "404", "409"));
        assertRequestSchema(updateDraft, "UpdateDraftEventRequest");
        assertSecurity(updateDraft, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseSchema(updateDraft, "200", "EventResponse");
        assertResponseReference(updateDraft, "400", "AuthInvalidRequest");
        assertResponseReference(updateDraft, "401", "AuthUnauthenticated");
        assertResponseReference(updateDraft, "403", "AuthForbidden");
        assertResponseReference(updateDraft, "404", "EventNotFound");
        assertResponseReference(updateDraft, "409", "EventConflict");

        // DELETE /api/v1/events/{id}
        Map<String, Object> deleteDraft = assertOperation(
                paths, "/api/v1/events/{id}", "delete", "deleteDraftEvent", Set.of("204", "401", "403", "404", "409"));
        assertThat(deleteDraft).doesNotContainKey("requestBody");
        assertSecurity(deleteDraft, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseReference(deleteDraft, "401", "AuthUnauthenticated");
        assertResponseReference(deleteDraft, "403", "AuthForbidden");
        assertResponseReference(deleteDraft, "404", "EventNotFound");
        assertResponseReference(deleteDraft, "409", "EventConflict");

        // GET /api/v1/events/{eventId}/sectors
        Map<String, Object> listSectors = assertOperation(
                paths, "/api/v1/events/{eventId}/sectors", "get", "listTicketSectors", Set.of("200", "401", "403", "404"));
        assertThat(listSectors).doesNotContainKey("requestBody");
        assertSecurity(listSectors, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(listSectors, "200", "TicketSectorListResponse");
        assertResponseReference(listSectors, "401", "AuthUnauthenticated");
        assertResponseReference(listSectors, "403", "AuthForbidden");
        assertResponseReference(listSectors, "404", "EventNotFound");

        // POST /api/v1/events/{eventId}/sectors
        Map<String, Object> createSector = assertOperation(
                paths, "/api/v1/events/{eventId}/sectors", "post", "createTicketSector", Set.of("201", "400", "401", "403", "404", "409"));
        assertRequestSchema(createSector, "CreateTicketSectorRequest");
        assertSecurity(createSector, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseSchema(createSector, "201", "TicketSectorResponse");
        assertResponseReference(createSector, "400", "AuthInvalidRequest");
        assertResponseReference(createSector, "401", "AuthUnauthenticated");
        assertResponseReference(createSector, "403", "AuthForbidden");
        assertResponseReference(createSector, "404", "EventNotFound");
        assertResponseReference(createSector, "409", "EventConflict");

        // PUT /api/v1/events/{eventId}/sectors/{sectorId}
        Map<String, Object> updateSector = assertOperation(
                paths, "/api/v1/events/{eventId}/sectors/{sectorId}", "put", "updateTicketSector", Set.of("200", "400", "401", "403", "404", "409"));
        assertRequestSchema(updateSector, "UpdateTicketSectorRequest");
        assertSecurity(updateSector, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseSchema(updateSector, "200", "TicketSectorResponse");
        assertResponseReference(updateSector, "400", "AuthInvalidRequest");
        assertResponseReference(updateSector, "401", "AuthUnauthenticated");
        assertResponseReference(updateSector, "403", "AuthForbidden");
        assertResponseReference(updateSector, "404", "EventNotFound");
        assertResponseReference(updateSector, "409", "EventConflict");

        // DELETE /api/v1/events/{eventId}/sectors/{sectorId}
        Map<String, Object> deleteSector = assertOperation(
                paths, "/api/v1/events/{eventId}/sectors/{sectorId}", "delete", "deleteTicketSector", Set.of("204", "401", "403", "404", "409"));
        assertThat(deleteSector).doesNotContainKey("requestBody");
        assertSecurity(deleteSector, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseReference(deleteSector, "401", "AuthUnauthenticated");
        assertResponseReference(deleteSector, "403", "AuthForbidden");
        assertResponseReference(deleteSector, "404", "EventNotFound");
        assertResponseReference(deleteSector, "409", "EventConflict");

        Map<String, Object> components = map(contract.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        assertEnumSchema(schemas, "EventStatus", EventStatus.class);
        assertRecordSchema(schemas, "CreateDraftEventRequest", CreateDraftEventRequest.class);
        assertRecordSchema(schemas, "UpdateDraftEventRequest", UpdateDraftEventRequest.class);
        assertRecordSchema(schemas, "EventResponse", EventResponse.class);
        assertRecordSchema(schemas, "EventListResponse", EventListResponse.class);
        assertRecordSchema(schemas, "TicketSectorResponse", TicketSectorResponse.class);
        assertRecordSchema(schemas, "TicketSectorListResponse", TicketSectorListResponse.class);
        assertRecordSchema(schemas, "CreateTicketSectorRequest", CreateTicketSectorRequest.class);
        assertRecordSchema(schemas, "UpdateTicketSectorRequest", UpdateTicketSectorRequest.class);
        assertEnumSchema(schemas, "EventErrorCode", EventErrorCode.class);
        assertRecordSchema(schemas, "EventApiError", EventApiErrorResponse.class);

        Map<String, Object> responses = map(components.get("responses"));
        assertThat(responses).containsKey("EventNotFound");
        assertResponseSchemaReference(responses, "EventNotFound", "EventApiError");
        assertThat(responses).containsKey("EventConflict");
        assertResponseSchemaReference(responses, "EventConflict", "EventApiError");
    }

    private void assertControllerMappings() {
        assertThat(EventsController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping baseMapping = EventsController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping).isNotNull();
        assertThat(mappingPaths(baseMapping.path(), baseMapping.value()))
                .containsExactly("/api/v1/events");

        List<String> actual = new ArrayList<>();
        Arrays.stream(EventsController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                actual.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });

        assertThat(actual).containsExactlyInAnyOrder(
                "POST /drafts -> createDraftEvent",
                "GET /mine -> listMyEvents",
                "GET /{id} -> getEvent",
                "PUT /{id} -> updateDraftEvent",
                "DELETE /{id} -> deleteDraftEvent",
                "GET /{eventId}/sectors -> listTicketSectors",
                "POST /{eventId}/sectors -> createTicketSector",
                "PUT /{eventId}/sectors/{sectorId} -> updateTicketSector",
                "DELETE /{eventId}/sectors/{sectorId} -> deleteTicketSector"
        );
    }

    private List<String> mappingPaths(String[] path, String[] value) {
        return List.of(path.length == 0 ? value : path);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadContract() throws IOException {
        String source = Files.readString(Path.of("..", "openapi", "elite-dev-ticket-v1.yaml"));
        return (Map<String, Object>) new Yaml().load(source);
    }

    private Map<String, Object> assertOperation(
            Map<String, Object> paths,
            String path,
            String method,
            String operationId,
            Set<String> expectedResponses) {
        Map<String, Object> pathItem = map(paths.get(path));
        Map<String, Object> operation = map(pathItem.get(method));
        assertThat(operation).containsEntry("operationId", operationId);
        assertThat(map(operation.get("responses")).keySet())
                .extracting(String::valueOf)
                .containsExactlyInAnyOrderElementsOf(expectedResponses);
        return operation;
    }

    private void assertRequestSchema(Map<String, Object> operation, String schemaName) {
        Map<String, Object> requestBody = map(operation.get("requestBody"));
        assertThat(requestBody).containsEntry("required", true);
        Map<String, Object> mediaType = map(map(requestBody.get("content")).get("application/json"));
        assertThat(referenceName(map(mediaType.get("schema")))).isEqualTo(schemaName);
    }

    private void assertSecurity(Map<String, Object> operation, Set<Set<String>> expectedRequirements) {
        Set<Set<String>> actual = list(operation.get("security")).stream()
                .map(this::map)
                .map(requirement -> Set.copyOf(requirement.keySet()))
                .collect(Collectors.toSet());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expectedRequirements);
    }

    private void assertResponseSchema(Map<String, Object> operation, String status, String schemaName) {
        Map<String, Object> response = map(map(operation.get("responses")).get(status));
        Map<String, Object> mediaType = map(map(response.get("content")).get("application/json"));
        assertThat(referenceName(map(mediaType.get("schema")))).isEqualTo(schemaName);
    }

    private void assertResponseReference(Map<String, Object> operation, String status, String responseName) {
        Map<String, Object> response = map(map(operation.get("responses")).get(status));
        assertThat(String.valueOf(response.get("$ref")))
                .isEqualTo("#/components/responses/" + responseName);
    }

    private void assertResponseSchemaReference(Map<String, Object> responses, String responseName, String schemaName) {
        Map<String, Object> response = map(responses.get(responseName));
        Map<String, Object> mediaType = map(map(response.get("content")).get("application/json"));
        assertThat(referenceName(map(mediaType.get("schema")))).isEqualTo(schemaName);
    }

    private void assertEnumSchema(Map<String, Object> schemas, String name, Class<? extends Enum<?>> type) {
        Map<String, Object> schema = map(schemas.get(name));
        assertThat(schema).containsEntry("type", "string");
        assertThat(list(schema.get("enum")))
                .extracting(String::valueOf)
                .containsExactly(Arrays.stream(type.getEnumConstants()).map(Enum::name).toArray(String[]::new));
    }

    private void assertRecordSchema(Map<String, Object> schemas, String name, Class<?> type) {
        Map<String, Object> schema = map(schemas.get(name));
        assertThat(type.isRecord()).isTrue();
        assertThat(schema).containsEntry("type", "object").containsEntry("additionalProperties", false);

        Map<String, Object> properties = map(schema.get("properties"));
        Set<String> componentNames = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertThat(componentNames).containsExactlyInAnyOrderElementsOf(properties.keySet());
        Set<String> requiredComponents = Arrays.stream(type.getRecordComponents())
                .filter(component -> {
                    JsonInclude include = component.getAccessor().getAnnotation(JsonInclude.class);
                    return include == null || include.value() != JsonInclude.Include.NON_NULL;
                })
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertThat(list(schema.get("required")))
                .extracting(String::valueOf)
                .containsExactlyInAnyOrderElementsOf(requiredComponents);

        for (RecordComponent component : type.getRecordComponents()) {
            assertComponentType(component, map(properties.get(component.getName())));
        }
    }

    private void assertComponentType(RecordComponent component, Map<String, Object> property) {
        if ("array".equals(property.get("type"))) {
            assertThat(component.getType()).isEqualTo(List.class);
            var genericType = (ParameterizedType) component.getGenericType();
            String itemReference = referenceName(map(property.get("items")));
            assertThat(genericType.getActualTypeArguments()).containsExactly(REFERENCED_TYPES.get(itemReference));
            return;
        }

        Class<?> expected = referencedOrScalarType(component, property);
        assertThat(component.getType())
                .as("tipo de %s", component.getName())
                .isEqualTo(expected);
    }

    private Class<?> referencedOrScalarType(RecordComponent component, Map<String, Object> property) {
        if (property.containsKey("$ref")) {
            return REFERENCED_TYPES.get(referenceName(property));
        }
        if ("boolean".equals(property.get("type"))) {
            return boolean.class;
        }
        if ("integer".equals(property.get("type"))) {
            return component.getType() == Integer.class ? Integer.class : int.class;
        }
        if ("number".equals(property.get("type"))) {
            return BigDecimal.class;
        }
        if ("string".equals(property.get("type")) && "uuid".equals(property.get("format"))) {
            return UUID.class;
        }
        if ("string".equals(property.get("type")) && "date-time".equals(property.get("format"))) {
            return Instant.class;
        }
        if ("string".equals(property.get("type"))) {
            return String.class;
        }
        throw new AssertionError("Tipo OpenAPI não suportado no check: " + property);
    }

    private String referenceName(Map<String, Object> property) {
        return String.valueOf(property.get("$ref")).replace("#/components/schemas/", "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
