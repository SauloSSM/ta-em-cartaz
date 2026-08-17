package br.com.elitedevticket.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.catalog.http.CatalogApiErrorResponse;
import br.com.elitedevticket.catalog.http.CatalogController;
import br.com.elitedevticket.catalog.http.CatalogErrorCode;
import br.com.elitedevticket.catalog.http.CatalogEventReferenceResponse;
import br.com.elitedevticket.catalog.http.CatalogSearchResponse;
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

class CatalogOpenApiContractTest {

    private static final Map<String, Class<?>> REFERENCED_TYPES = Map.of(
            "CatalogErrorCode", CatalogErrorCode.class,
            "CatalogEventReference", CatalogEventReferenceResponse.class,
            "CatalogSearchResponse", CatalogSearchResponse.class,
            "CatalogApiError", CatalogApiErrorResponse.class,
            "FieldError", FieldErrorResponse.class);

    @Test
    void javaCatalogDtosAndOperationsRemainStructurallyAlignedWithTheVersionedOpenApi() throws IOException {
        assertControllerMappings();
        Map<String, Object> contract = loadContract();
        Map<String, Object> paths = map(contract.get("paths"));

        Map<String, Object> search = assertOperation(
                paths, "/api/v1/catalog/events", "get", "searchCatalogEvents", Set.of("200", "400", "401", "403", "503"));
        assertThat(search).doesNotContainKey("requestBody");
        assertSecurity(search, Set.of(Set.of("SessionCookie")));
        assertResponseSchema(search, "200", "CatalogSearchResponse");
        assertResponseReference(search, "400", "AuthInvalidRequest");
        assertResponseReference(search, "401", "AuthUnauthenticated");
        assertResponseReference(search, "403", "AuthForbidden");
        assertResponseReference(search, "503", "CatalogUnavailable");

        Map<String, Object> components = map(contract.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        assertEnumSchema(schemas, "CatalogErrorCode", CatalogErrorCode.class);
        assertRecordSchema(schemas, "CatalogEventReference", CatalogEventReferenceResponse.class);
        assertRecordSchema(schemas, "CatalogSearchResponse", CatalogSearchResponse.class);
        assertRecordSchema(schemas, "CatalogApiError", CatalogApiErrorResponse.class);

        Map<String, Object> responses = map(components.get("responses"));
        assertThat(responses).containsKey("CatalogUnavailable");
        assertResponseSchemaReference(responses, "CatalogUnavailable", "CatalogApiError");
    }

    private void assertControllerMappings() {
        assertThat(CatalogController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping baseMapping = CatalogController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping).isNotNull();
        assertThat(mappingPaths(baseMapping.path(), baseMapping.value()))
                .containsExactly("/api/v1/catalog");

        List<String> actual = new ArrayList<>();
        Arrays.stream(CatalogController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                actual.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });

        assertThat(actual).containsExactly("GET /events -> searchCatalogEvents");
    }

    private List<String> mappingPaths(String[] path, String[] value) {
        return List.of(path.length == 0 ? value : path);
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
        assertThat(pathItem).containsOnlyKeys(method);
        Map<String, Object> operation = map(pathItem.get(method));
        assertThat(operation).containsEntry("operationId", operationId);
        assertThat(map(operation.get("responses")).keySet())
                .extracting(String::valueOf)
                .containsExactlyInAnyOrderElementsOf(expectedResponses);
        return operation;
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

        Class<?> expected = referencedOrScalarType(property);
        assertThat(component.getType())
                .as("tipo de %s", component.getName())
                .isEqualTo(expected);
    }

    private Class<?> referencedOrScalarType(Map<String, Object> property) {
        if (property.containsKey("$ref")) {
            return REFERENCED_TYPES.get(referenceName(property));
        }
        if ("boolean".equals(property.get("type"))) {
            return boolean.class;
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
