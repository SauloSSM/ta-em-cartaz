package br.com.elitedevticket.payments;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import br.com.elitedevticket.payments.domain.PaymentStatus;
import br.com.elitedevticket.payments.http.PaymentResponse;
import br.com.elitedevticket.payments.http.PaymentsController;
import br.com.elitedevticket.payments.http.ProcessPaymentRequest;
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

class PaymentsOpenApiContractTest {

    private static final Map<String, Class<?>> REFERENCED_TYPES = Map.ofEntries(
            Map.entry("PaymentSimulatedOutcome", PaymentSimulatedOutcome.class),
            Map.entry("PaymentStatus", PaymentStatus.class),
            Map.entry("ProcessPaymentRequest", ProcessPaymentRequest.class),
            Map.entry("PaymentResponse", PaymentResponse.class)
    );

    @Test
    void javaPaymentsDtosAndOperationsRemainStructurallyAlignedWithTheVersionedOpenApi() throws IOException {
        assertControllerMappings();
        Map<String, Object> contract = loadContract();
        Map<String, Object> paths = map(contract.get("paths"));

        // POST /api/v1/reservations/{reservationId}/payments
        Map<String, Object> processPayment = assertOperation(
                paths,
                "/api/v1/reservations/{reservationId}/payments",
                "post",
                "processPayment",
                Set.of("200", "400", "401", "403", "404", "409", "422")
        );
        assertRequestSchema(processPayment, "ProcessPaymentRequest");
        assertSecurity(processPayment, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertResponseSchema(processPayment, "200", "PaymentResponse");
        assertResponseReference(processPayment, "400", "AuthInvalidRequest");
        assertResponseReference(processPayment, "401", "AuthUnauthenticated");
        assertResponseReference(processPayment, "403", "AuthForbidden");
        assertResponseReference(processPayment, "404", "ReservationNotFound");
        assertResponseReference(processPayment, "409", "ReservationConflict");
        assertResponseReference(processPayment, "422", "ReservationUnprocessable");

        Map<String, Object> components = map(contract.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        assertEnumSchema(schemas, "PaymentSimulatedOutcome", PaymentSimulatedOutcome.class);
        assertEnumSchema(schemas, "PaymentStatus", PaymentStatus.class);
        assertRecordSchema(schemas, "ProcessPaymentRequest", ProcessPaymentRequest.class);
        assertRecordSchema(schemas, "PaymentResponse", PaymentResponse.class);
    }

    private void assertControllerMappings() {
        assertThat(PaymentsController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping baseMapping = PaymentsController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping).isNotNull();
        assertThat(mappingPaths(baseMapping.path(), baseMapping.value()))
                .containsExactly("/api/v1/reservations");

        List<String> actual = new ArrayList<>();
        Arrays.stream(PaymentsController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                actual.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });

        assertThat(actual).containsExactlyInAnyOrder(
                "POST /{reservationId}/payments -> processPayment"
        );
    }

    private List<String> mappingPaths(String[] path, String[] value) {
        String[] candidate = path.length > 0 ? path : value;
        return candidate.length == 0 ? List.of("") : List.of(candidate);
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
        assertThat(response.get("$ref")).isEqualTo("#/components/responses/" + responseName);
    }

    private void assertEnumSchema(Map<String, Object> schemas, String schemaName, Class<? extends Enum<?>> enumClass) {
        Map<String, Object> schema = map(schemas.get(schemaName));
        assertThat(schema).containsEntry("type", "string");
        Set<String> actual = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> expected = list(schema.get("enum")).stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private void assertRecordSchema(Map<String, Object> schemas, String schemaName, Class<?> recordClass) {
        assertThat(recordClass.isRecord()).isTrue();
        Map<String, Object> schema = map(schemas.get(schemaName));
        assertThat(schema).containsEntry("type", "object");
        assertThat(schema).containsEntry("additionalProperties", false);

        Map<String, Object> properties = map(schema.get("properties"));
        Set<String> actualPropertyNames = Arrays.stream(recordClass.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertThat(properties.keySet()).containsExactlyInAnyOrderElementsOf(actualPropertyNames);

        List<String> required = list(schema.get("required")).stream()
                .map(String::valueOf)
                .toList();

        for (RecordComponent component : recordClass.getRecordComponents()) {
            Map<String, Object> property = map(properties.get(component.getName()));
            assertTypeAlignment(component, property);
        }
    }

    private void assertTypeAlignment(RecordComponent component, Map<String, Object> property) {
        Class<?> type = component.getType();
        if (type.equals(UUID.class)) {
            assertThat(property).containsEntry("type", "string");
            assertThat(property).containsEntry("format", "uuid");
        } else if (type.equals(Instant.class)) {
            assertThat(property).containsEntry("type", "string");
            assertThat(property).containsEntry("format", "date-time");
        } else if (type.equals(String.class)) {
            assertThat(property).containsEntry("type", "string");
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            assertThat(property).containsEntry("type", "integer");
        } else if (type.equals(BigDecimal.class) || type.equals(double.class) || type.equals(Double.class)) {
            assertThat(property).containsEntry("type", "number");
        } else if (type.isEnum()) {
            assertThat(referenceName(property)).isEqualTo(type.getSimpleName());
        } else if (type.equals(List.class)) {
            assertThat(property).containsEntry("type", "array");
            Map<String, Object> items = map(property.get("items"));
            ParameterizedType genericType = (ParameterizedType) component.getGenericType();
            Class<?> itemType = (Class<?>) genericType.getActualTypeArguments()[0];
            assertThat(referenceName(items)).isEqualTo(itemType.getSimpleName());
        } else if (REFERENCED_TYPES.containsValue(type)) {
            assertThat(referenceName(property)).isEqualTo(type.getSimpleName());
        }
    }

    private String referenceName(Map<String, Object> schema) {
        String ref = String.valueOf(schema.get("$ref"));
        assertThat(ref).startsWith("#/components/schemas/");
        return ref.substring("#/components/schemas/".length());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }
}
