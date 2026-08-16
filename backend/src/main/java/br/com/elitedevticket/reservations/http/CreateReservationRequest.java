package br.com.elitedevticket.reservations.http;

import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;

public record CreateReservationRequest(
        Integer quantity
) {
    public void validate() {
        if (quantity == null) {
            throw new InvalidReservationQuantityException("A quantidade é obrigatória.");
        }
        if (quantity < 1 || quantity > 6) {
            throw new InvalidReservationQuantityException("A quantidade de ingressos deve ser entre 1 e 6.");
        }
    }
}
