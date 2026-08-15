package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.ArrayList;

public record CreateTicketSectorRequest(
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        Integer capacity,
        BigDecimal price
) {
    public void validate() {
        var errors = new ArrayList<FieldErrorResponse>();
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            errors.add(new FieldErrorResponse("name", "Nome do setor é obrigatório."));
        } else if (normalizedName.length() > 255) {
            errors.add(new FieldErrorResponse("name", "Nome do setor deve ter no máximo 255 caracteres."));
        }
        if (capacity == null) {
            errors.add(new FieldErrorResponse("capacity", "Capacidade é obrigatória."));
        } else if (capacity <= 0) {
            errors.add(new FieldErrorResponse("capacity", "Capacidade deve ser maior que zero."));
        }
        if (price == null) {
            errors.add(new FieldErrorResponse("price", "Preço é obrigatório."));
        } else if (price.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldErrorResponse("price", "Preço deve ser maior ou igual a zero."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidEventRequestException(errors);
        }
    }
}
