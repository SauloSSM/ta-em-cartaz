package br.com.elitedevticket.catalog.http;

import java.util.List;

public record CatalogSearchResponse(
        List<CatalogEventReferenceResponse> events) {
}
