package br.com.elitedevticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.auth.domain.Role;
import br.com.elitedevticket.auth.http.AnonymousSessionResponse;
import br.com.elitedevticket.auth.http.ApiErrorResponse;
import br.com.elitedevticket.auth.http.AuthErrorCode;
import br.com.elitedevticket.auth.http.AuthenticatedSessionResponse;
import br.com.elitedevticket.auth.http.AuthController;
import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.auth.http.LoginRequest;
import br.com.elitedevticket.auth.http.SessionResponse;
import br.com.elitedevticket.auth.http.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
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

class OpenApiContractTest {
    private static final Map<String, Class<?>> REFERENCED_TYPES = Map.of(
            "Role", Role.class,
            "SessionUser", UserResponse.class,
            "AuthErrorCode", AuthErrorCode.class,
            "FieldError", FieldErrorResponse.class);

    @Test
    void javaAuthDtosAndOperationsRemainStructurallyAlignedWithTheVersionedOpenApi() throws IOException {
        assertControllerMappings();
        Map<String, Object> contract = loadContract();
        Map<String, Object> paths = map(contract.get("paths"));
        Map<String, Object> session = assertOperation(
                paths, "/api/v1/auth/session", "get", "getAuthSession", Set.of("200"));
        assertThat(session).doesNotContainKey("requestBody");
        assertSecurity(session, Set.of(Set.of("SessionCookie"), Set.of()));
        assertResponseSchema(session, "200", "SessionResponse");

        Map<String, Object> login = assertOperation(
                paths, "/api/v1/auth/login", "post", "login", Set.of("200", "400", "401", "403"));
        assertSecurity(login, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertRequestSchema(login, "LoginRequest");
        assertResponseSchema(login, "200", "AuthenticatedSessionResponse");
        assertResponseReference(login, "400", "AuthInvalidRequest");
        assertResponseReference(login, "401", "AuthInvalidCredentials");
        assertResponseReference(login, "403", "AuthInvalidCsrf");

        Map<String, Object> logout = assertOperation(
                paths, "/api/v1/auth/logout", "post", "logout", Set.of("204", "403"));
        assertThat(logout).doesNotContainKey("requestBody");
        assertSecurity(logout, Set.of(Set.of("CsrfCookie", "CsrfHeader")));
        assertThat(map(map(logout.get("responses")).get("204"))).doesNotContainKey("content");
        assertResponseReference(logout, "403", "AuthInvalidCsrf");

        Map<String, Object> components = map(contract.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        assertEnumSchema(schemas, "Role", Role.class);
        assertEnumSchema(schemas, "AuthErrorCode", AuthErrorCode.class);
        assertRecordSchema(schemas, "LoginRequest", LoginRequest.class);
        assertRecordSchema(schemas, "SessionUser", UserResponse.class);
        assertRecordSchema(schemas, "AnonymousSessionResponse", AnonymousSessionResponse.class);
        assertRecordSchema(schemas, "AuthenticatedSessionResponse", AuthenticatedSessionResponse.class);
        assertRecordSchema(schemas, "FieldError", FieldErrorResponse.class);
        assertRecordSchema(schemas, "ApiError", ApiErrorResponse.class);
        assertSessionUnion(schemas);

        Map<String, Object> schemes = map(components.get("securitySchemes"));
        assertSecurityScheme(schemes, "SessionCookie", "cookie", "EDT_SESSION");
        assertSecurityScheme(schemes, "CsrfCookie", "cookie", "XSRF-TOKEN");
        assertThat(map(schemes.get("CsrfHeader")))
                .containsEntry("type", "apiKey")
                .containsEntry("in", "header")
                .containsEntry("name", "X-XSRF-TOKEN");

        Map<String, Object> responses = map(components.get("responses"));
        assertThat(map(map(responses.get("AuthInvalidCredentials")).get("headers")))
                .containsKey("Set-Cookie");
        assertThat(responses).containsKeys("AuthUnauthenticated", "AuthForbidden");
        assertResponseSchemaReference(responses, "AuthUnauthenticated", "ApiError");
        assertResponseSchemaReference(responses, "AuthForbidden", "ApiError");
    }

    private void assertControllerMappings() {
        assertThat(AuthController.class.isAnnotationPresent(RestController.class)).isTrue();
        RequestMapping baseMapping = AuthController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping).isNotNull();
        assertThat(mappingPaths(baseMapping.path(), baseMapping.value()))
                .containsExactly("/api/v1/auth");

        List<String> actual = new ArrayList<>();
        Arrays.stream(AuthController.class.getDeclaredMethods()).forEach(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                List<String> paths = mappingPaths(mapping.path(), mapping.value());
                assertThat(paths).hasSize(1);
                assertThat(mapping.method()).hasSize(1);
                actual.add(mapping.method()[0].name() + " " + paths.getFirst() + " -> " + method.getName());
            }
        });

        assertThat(actual).containsExactlyInAnyOrder(
                "GET /session -> session",
                "POST /login -> login",
                "POST /logout -> logout");
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

    private void assertRequestSchema(Map<String, Object> operation, String schemaName) {
        Map<String, Object> requestBody = map(operation.get("requestBody"));
        assertThat(requestBody).containsEntry("required", true);
        Map<String, Object> mediaType = map(map(requestBody.get("content")).get("application/json"));
        assertThat(referenceName(map(mediaType.get("schema")))).isEqualTo(schemaName);
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

    private void assertSecurityScheme(
            Map<String, Object> schemes, String schemeName, String location, String parameterName) {
        assertThat(map(schemes.get(schemeName)))
                .containsEntry("type", "apiKey")
                .containsEntry("in", location)
                .containsEntry("name", parameterName);
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

    private void assertSessionUnion(Map<String, Object> schemas) {
        Map<String, Object> session = map(schemas.get("SessionResponse"));
        assertThat(session).doesNotContainKey("discriminator");
        Set<String> openApiMembers = list(session.get("oneOf")).stream()
                .map(this::map)
                .map(this::referenceName)
                .collect(Collectors.toSet());
        Set<String> javaMembers = Arrays.stream(SessionResponse.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertThat(SessionResponse.class.isSealed()).isTrue();
        assertThat(javaMembers).containsExactlyInAnyOrderElementsOf(openApiMembers);
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
