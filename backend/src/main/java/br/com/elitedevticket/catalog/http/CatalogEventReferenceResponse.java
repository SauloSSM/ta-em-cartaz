package br.com.elitedevticket.catalog.http;

import com.fasterxml.jackson.annotation.JsonInclude;

public record CatalogEventReferenceResponse(
        String externalId,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String category) {
}
