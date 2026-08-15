package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDraftEventUseCase {

    private final EventRepository eventRepository;
    private final Clock clock;

    public UpdateDraftEventUseCase(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public Event execute(
            UUID eventId,
            UUID organizerId,
            String title,
            String description,
            String imageUrl,
            String category,
            String venueName,
            String venueAddress,
            Instant startsAt) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");

        Event existing = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        if (!existing.isOwnedBy(organizerId)) {
            throw new EventForbiddenException();
        }

        String trimmedTitle = title == null ? null : title.trim();
        if (trimmedTitle == null || trimmedTitle.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        Instant now = Instant.now(clock);

        if (existing.status() == EventStatus.DRAFT) {
            Event updated = existing.withUpdatedDraftDetails(
                    trimmedTitle,
                    trimToNull(description),
                    trimToNull(imageUrl),
                    trimToNull(category),
                    trimToNull(venueName),
                    trimToNull(venueAddress),
                    startsAt,
                    now
            );
            return eventRepository.save(updated);
        }

        if (existing.status() == EventStatus.PUBLISHED) {
            boolean titleChanged = !trimmedTitle.equals(existing.title());
            boolean venueNameChanged = !Objects.equals(trimToNull(venueName), existing.venueName());
            boolean venueAddressChanged = !Objects.equals(trimToNull(venueAddress), existing.venueAddress());
            boolean startsAtChanged = startsAt != null && !startsAt.equals(existing.startsAt());

            if (titleChanged || venueNameChanged || venueAddressChanged || startsAtChanged) {
                throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Campos estruturais de eventos publicados são imutáveis.");
            }

            Event updated = existing.withUpdatedPublishedDetails(
                    trimToNull(description),
                    trimToNull(imageUrl),
                    trimToNull(category),
                    now
            );
            return eventRepository.save(updated);
        }

        throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Apenas eventos em rascunho ou publicados podem ser modificados.");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
