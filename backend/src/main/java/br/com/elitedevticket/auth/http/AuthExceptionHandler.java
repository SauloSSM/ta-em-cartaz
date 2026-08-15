package br.com.elitedevticket.auth.http;

import br.com.elitedevticket.auth.adapters.security.SessionCookieService;
import br.com.elitedevticket.auth.application.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public final class AuthExceptionHandler {
    private final Clock clock;
    private final SessionCookieService sessionCookies;

    public AuthExceptionHandler(Clock clock, SessionCookieService sessionCookies) {
        this.clock = clock;
        this.sessionCookies = sessionCookies;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiErrorResponse> invalidCredentials(HttpServletResponse response) {
        sessionCookies.clear(response);
        return error(
                HttpStatus.UNAUTHORIZED,
                AuthErrorCode.AUTH_INVALID_CREDENTIALS,
                "E-mail ou senha inválidos.",
                null);
    }

    @ExceptionHandler(InvalidAuthRequestException.class)
    ResponseEntity<ApiErrorResponse> invalidRequest(InvalidAuthRequestException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.AUTH_INVALID_REQUEST,
                "Requisição de autenticação inválida.",
                exception.fieldErrors());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiErrorResponse> unreadableRequest() {
        return error(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.AUTH_INVALID_REQUEST,
                "Requisição de autenticação inválida.",
                null);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            AuthErrorCode code,
            String message,
            List<FieldErrorResponse> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                code, message, fieldErrors, UUID.randomUUID().toString(), clock.instant());
        return ResponseEntity.status(status).body(body);
    }
}
