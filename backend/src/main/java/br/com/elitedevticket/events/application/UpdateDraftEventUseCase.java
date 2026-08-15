package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
