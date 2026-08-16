package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import java.math.BigDecimal;

public record PublicEventItem(
        Event event,
        BigDecimal startingPrice,
        boolean salesClosed
) {
}
