package br.com.elitedevticket.tickets;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import br.com.elitedevticket.tickets.http.MyTicketListResponse;
import br.com.elitedevticket.tickets.http.MyTicketResponse;
import br.com.elitedevticket.tickets.http.MyTicketsController;
import br.com.elitedevticket.tickets.http.PublicTicketResponse;
import br.com.elitedevticket.tickets.http.PublicTicketsController;
import br.com.elitedevticket.tickets.http.TicketApiErrorResponse;
import br.com.elitedevticket.tickets.http.TicketErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
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

class TicketsOpenApiContractTest {

    private static final Map<String, Class<?>> REFERENCED_TYPES = Map.of(
            "TicketStatus", TicketStatus.class,
            "MyTicketResponse", MyTicketResponse.class,
            "MyTicketListResponse", MyTicketListResponse.class,
            "PublicTicketResponse", PublicTicketResponse.class,
            "TicketErrorCode", TicketErrorCode.class,
            "TicketApiError", TicketApiErrorResponse.class,
            "FieldError", FieldErrorResponse.class);

    @Test
    void javaTicketsDtosAndOperationsRemainStructurallyAlignedWithTheVersionedOpenApi() throws IOException {
        assertControllerMappings();
        Map<String, Object> contract = loadContract();
        Map<String, Object> paths = map(contract.get("paths"));

        // GET /api/v1/my-tickets
        Map<String, Object> listMyTickets = assertOperation(
                paths, "/api/v1/my-tickets", "get", "listMyTickets", Set.of("200", "401", "403"));
        assertThat(listMyTickets).doesNotContainKey("requestBody");
        assertSecurity(listMyTickets, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(listMyTickets, "200", "MyTicketListResponse");
        assertResponseReference(listMyTickets, "401", "AuthUnauthenticated");
        assertResponseReference(listMyTickets, "403", "AuthForbidden");

        // GET /api/v1/my-tickets/{ticketId}
        Map<String, Object> getMyTicket = assertOperation(
                paths, "/api/v1/my-tickets/{ticketId}", "get", "getMyTicket", Set.of("200", "401", "403", "404"));
        assertThat(getMyTicket).doesNotContainKey("requestBody");
        assertSecurity(getMyTicket, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(getMyTicket, "200", "MyTicketResponse");
        assertResponseReference(getMyTicket, "401", "AuthUnauthenticated");
        assertResponseReference(getMyTicket, "403", "AuthForbidden");
        assertResponseReference(getMyTicket, "404", "TicketNotFound");

        // GET /api/v1/public/tickets/{shareToken}
        Map<String, Object> getPublicTicket = assertOperation(
                paths, "/api/v1/public/tickets/{shareToken}", "get", "getPublicTicket", Set.of("200", "404"));
        assertThat(getPublicTicket).doesNotContainKey("requestBody");
        assertSecurity(getPublicTicket, Set.of(Set.of()));
        assertResponseSchema(getPublicTicket, "200", "PublicTicketResponse");
        assertResponseReference(getPublicTicket, "404", "TicketNotFound");

        Map<String, Object> components = map(contract.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        assertEnumSchema(schemas, "TicketStatus", TicketStatus.class);
        assertRecordSchema(schemas, "MyTicketResponse", MyTicketResponse.class);
        assertRecordSchema(schemas, "MyTicketListResponse", MyTicketListResponse.class);
        assertRecordSchema(schemas, "PublicTicketResponse", PublicTicketResponse.class);
        assertEnumSchema(schemas, "TicketErrorCode", TicketErrorCode.class);
        assertRecordSchema(schemas, "TicketApiError", TicketApiErrorResponse.class);

        Map<String, Object> responses = map(components.get("responses"));
        assertThat(responses).containsKey("TicketNotFound");
        assertResponseSchemaReference(responses, "TicketNotFound", "TicketApiError");
    }

    private void assertControllerMappings() {
        assertThat(MyTicketsController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping myTicketsMapping = MyTicketsController.class.getAnnotation(RequestMapping.class);
        assertThat(myTicketsMapping).isNotNull();
        assertThat(mappingPaths(myTicketsMapping.path(), myTicketsMapping.value()))
                .containsExactly("/api/v1/my-tickets");

        List<String> myTicketsMethods = new ArrayList<>();
        Arrays.stream(MyTicketsController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                myTicketsMethods.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });
        assertThat(myTicketsMethods).containsExactlyInAnyOrder(
                "GET  -> listMyTickets",
                "GET /{ticketId} -> getMyTicket"
        );

        assertThat(PublicTicketsController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping publicMapping = PublicTicketsController.class.getAnnotation(RequestMapping.class);
        assertThat(publicMapping).isNotNull();
        assertThat(mappingPaths(publicMapping.path(), publicMapping.value()))
                .containsExactly("/api/v1/public/tickets");

        List<String> publicMethods = new ArrayList<>();
        Arrays.stream(PublicTicketsController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                publicMethods.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });
        assertThat(publicMethods).containsExactly("GET /{shareToken} -> getPublicTicket");
    }

    private List<String> mappingPaths(String[] path, String[] value) {
        if (path.length > 0) {
            return List.of(path);
        }
        if (value.length > 0) {
            return List.of(value);
        }
        return List.of("");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadContract() throws IOException {
        String source = Files.readString(Path.of("..", "openapi", "ta-em-cartaz-v1.yaml"));
        return (Map<String, Object>) new Yaml().load(source);
    }

    private Map<String, Object> assertOperation(
            Map<String, Object> paths,
            String path,
            String method,
            String operationId,
            Set<String> expectedStatuses
    ) {
        Map<String, Object> pathItem = map(paths.get(path));
        assertThat(pathItem).as("Path " + path + " deve existir").isNotNull();
        Map<String, Object> operation = map(pathItem.get(method));
        assertThat(operation).as("Método " + method + " em " + path + " deve existir").isNotNull();
        assertThat(operation.get("operationId")).isEqualTo(operationId);

        Map<String, Object> responses = map(operation.get("responses"));
        assertThat(responses.keySet()).isEqualTo(expectedStatuses);
        return operation;
    }

    private void assertSecurity(Map<String, Object> operation, Set<Set<String>> expectedRequirements) {
        List<Map<String, Object>> security = list(operation.get("security"));
        Set<Set<String>> requirements = security.stream()
                .map(req -> req.keySet().stream().map(String::valueOf).collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        assertThat(requirements).isEqualTo(expectedRequirements);
    }

    private void assertResponseSchema(Map<String, Object> operation, String statusCode, String expectedSchemaName) {
        Map<String, Object> responses = map(operation.get("responses"));
        Map<String, Object> response = map(responses.get(statusCode));
        Map<String, Object> content = map(response.get("content"));
        Map<String, Object> json = map(content.get("application/json"));
        Map<String, Object> schema = map(json.get("schema"));
        assertThat(schema.get("$ref")).isEqualTo("#/components/schemas/" + expectedSchemaName);
    }

    private void assertResponseReference(Map<String, Object> operation, String statusCode, String expectedResponseName) {
        Map<String, Object> responses = map(operation.get("responses"));
        Map<String, Object> response = map(responses.get(statusCode));
        assertThat(response.get("$ref")).isEqualTo("#/components/responses/" + expectedResponseName);
    }

    private void assertResponseSchemaReference(Map<String, Object> responses, String responseName, String expectedSchemaName) {
        Map<String, Object> response = map(responses.get(responseName));
        Map<String, Object> content = map(response.get("content"));
        Map<String, Object> json = map(content.get("application/json"));
        Map<String, Object> schema = map(json.get("schema"));
        assertThat(schema.get("$ref")).isEqualTo("#/components/schemas/" + expectedSchemaName);
    }

    private void assertRecordSchema(Map<String, Object> schemas, String schemaName, Class<?> recordClass) {
        Map<String, Object> schema = map(schemas.get(schemaName));
        assertThat(schema).as("Schema " + schemaName + " deve existir").isNotNull();
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);

        List<String> required = list(schema.get("required"));
        Map<String, Object> properties = map(schema.get("properties"));

        RecordComponent[] components = recordClass.getRecordComponents();
        assertThat(properties.keySet()).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(components).map(RecordComponent::getName).toList()
        );
        Set<String> requiredComponents = Arrays.stream(components)
                .filter(component -> {
                    JsonInclude include = component.getAccessor().getAnnotation(JsonInclude.class);
                    if (include == null) {
                        try {
                            include = recordClass.getDeclaredField(component.getName()).getAnnotation(JsonInclude.class);
                        } catch (NoSuchFieldException ignored) {}
                    }
                    return include == null || include.value() != JsonInclude.Include.NON_NULL;
                })
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertThat(required).containsExactlyInAnyOrderElementsOf(requiredComponents);

        for (RecordComponent component : components) {
            Map<String, Object> property = map(properties.get(component.getName()));
            assertPropertyType(property, component);
        }
    }

    private void assertPropertyType(Map<String, Object> property, RecordComponent component) {
        Class<?> type = component.getType();
        if (type.equals(UUID.class)) {
            assertThat(property.get("type")).isEqualTo("string");
            assertThat(property.get("format")).isEqualTo("uuid");
            return;
        }
        if (type.equals(String.class)) {
            assertThat(property.get("type")).isEqualTo("string");
            return;
        }
        if (type.equals(Instant.class)) {
            assertThat(property.get("type")).isEqualTo("string");
            assertThat(property.get("format")).isEqualTo("date-time");
            return;
        }
        if (type.equals(int.class) || type.equals(Integer.class)) {
            assertThat(property.get("type")).isEqualTo("integer");
            return;
        }
        if (type.isEnum()) {
            assertThat(property.get("$ref")).isEqualTo("#/components/schemas/" + type.getSimpleName());
            return;
        }
        if (List.class.isAssignableFrom(type)) {
            assertThat(property.get("type")).isEqualTo("array");
            Map<String, Object> items = map(property.get("items"));
            ParameterizedType genericType = (ParameterizedType) component.getGenericType();
            Class<?> itemType = (Class<?>) genericType.getActualTypeArguments()[0];
            String itemReference = referenceName(items);
            assertThat(REFERENCED_TYPES.get(itemReference)).isEqualTo(itemType);
            return;
        }
        throw new AssertionError("Tipo não mapeado no assert: " + type.getName());
    }

    private String referenceName(Map<String, Object> property) {
        return String.valueOf(property.get("$ref")).replace("#/components/schemas/", "");
    }

    private void assertEnumSchema(Map<String, Object> schemas, String schemaName, Class<? extends Enum<?>> enumClass) {
        Map<String, Object> schema = map(schemas.get(schemaName));
        assertThat(schema).as("Schema enum " + schemaName + " deve existir").isNotNull();
        assertThat(schema.get("type")).isEqualTo("string");
        List<String> values = list(schema.get("enum"));
        List<String> expectedValues = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
        assertThat(values).containsExactlyInAnyOrderElementsOf(expectedValues);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(Object value) {
        return value == null ? List.of() : (List<T>) value;
    }
}
