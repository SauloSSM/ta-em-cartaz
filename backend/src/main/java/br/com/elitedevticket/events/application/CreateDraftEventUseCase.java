package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDraftEventUseCase {

    private final EventRepository eventRepository;
    private final Clock clock;

    public CreateDraftEventUseCase(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public Event createDraft(UUID organizerId, String title, String externalId, String description, String imageUrl, String category) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Título do evento é obrigatório.");
        }
        Instant now = clock.instant();
        UUID eventId = UUID.randomUUID();

        String trimmedExternalId = (externalId != null && !externalId.isBlank()) ? externalId.trim() : null;
        String trimmedDescription = (description != null && !description.isBlank()) ? description.trim() : null;
        String trimmedImageUrl = (imageUrl != null && !imageUrl.isBlank()) ? imageUrl.trim() : null;
        String trimmedCategory = (category != null && !category.isBlank()) ? category.trim() : null;

        Event draftEvent = new Event(
                eventId,
                organizerId,
                trimmedExternalId,
                title.trim(),
                trimmedDescription,
                trimmedImageUrl,
                trimmedCategory,
                EventStatus.DRAFT,
                null,
                null,
                now,
                now
        );

        return eventRepository.save(draftEvent);
    }
}
