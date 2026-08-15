package br.com.elitedevticket.catalog.application;

import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import java.util.List;

public interface CatalogProvider {
    List<CatalogEventReference> searchEvents(String keyword);
}
