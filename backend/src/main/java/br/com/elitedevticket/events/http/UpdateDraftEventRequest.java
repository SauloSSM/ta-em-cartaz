package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;

public record UpdateDraftEventRequest(
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String category,
        @JsonInclude(JsonInclude.Include.NON_NULL) String venue,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant startsAt
) {
    public void validate() {
        var errors = new ArrayList<FieldErrorResponse>();
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isBlank()) {
            errors.add(new FieldErrorResponse("title", "Título do evento é obrigatório."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidEventRequestException(errors);
        }
    }
}
