package br.com.elitedevticket.catalog.http;

import br.com.elitedevticket.catalog.domain.CatalogUnavailableException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CatalogController.class)
public final class CatalogExceptionHandler {
    private final Clock clock;

    public CatalogExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ResponseEntity<CatalogApiErrorResponse> catalogUnavailable(CatalogUnavailableException exception) {
        CatalogApiErrorResponse body = new CatalogApiErrorResponse(
                CatalogErrorCode.CATALOG_UNAVAILABLE,
                "Catálogo Ticketmaster temporariamente indisponível.",
                null,
                UUID.randomUUID().toString(),
                clock.instant());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
