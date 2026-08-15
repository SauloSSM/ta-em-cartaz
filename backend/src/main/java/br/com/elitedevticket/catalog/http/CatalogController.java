package br.com.elitedevticket.catalog.http;

import br.com.elitedevticket.catalog.application.SearchCatalogUseCase;
import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final SearchCatalogUseCase searchCatalogUseCase;

    public CatalogController(SearchCatalogUseCase searchCatalogUseCase) {
        this.searchCatalogUseCase = searchCatalogUseCase;
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public CatalogSearchResponse searchCatalogEvents(
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<CatalogEventReference> references = searchCatalogUseCase.execute(keyword);
        List<CatalogEventReferenceResponse> responses = references.stream()
                .map(ref -> new CatalogEventReferenceResponse(
                        ref.externalId(),
                        ref.title(),
                        ref.description(),
                        ref.imageUrl(),
                        ref.category()))
                .toList();
        return new CatalogSearchResponse(responses);
    }
}
