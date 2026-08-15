package br.com.elitedevticket.catalog.application;

import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchCatalogUseCase {
    private final CatalogProvider catalogProvider;

    public SearchCatalogUseCase(CatalogProvider catalogProvider) {
        this.catalogProvider = catalogProvider;
    }

    public List<CatalogEventReference> execute(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        return catalogProvider.searchEvents(trimmed);
    }
}
