package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;

public record CreateDraftEventRequest(
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalSource,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalId,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String category
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
