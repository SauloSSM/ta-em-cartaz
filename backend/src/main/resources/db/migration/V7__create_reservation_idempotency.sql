CREATE TABLE reservation_idempotency_keys (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(128) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_reservation_idempotency_customer_key UNIQUE (customer_id, idempotency_key)
);

CREATE INDEX idx_reservation_idempotency_customer ON reservation_idempotency_keys(customer_id);
CREATE INDEX idx_reservation_idempotency_reservation ON reservation_idempotency_keys(reservation_id);
