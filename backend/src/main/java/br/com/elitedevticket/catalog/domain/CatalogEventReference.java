package br.com.elitedevticket.catalog.domain;

import java.util.Objects;

public record CatalogEventReference(
        String externalId,
        String title,
        String description,
        String imageUrl,
        String category) {

    public CatalogEventReference {
        Objects.requireNonNull(externalId, "externalId não pode ser nulo");
        Objects.requireNonNull(title, "title não pode ser nulo");
    }
}
