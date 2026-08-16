CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    event_id UUID NOT NULL REFERENCES events(id),
    sector_id UUID NOT NULL REFERENCES ticket_sectors(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    ordinal INT NOT NULL CHECK (ordinal >= 1 AND ordinal <= 6),
    status VARCHAR(32) NOT NULL CHECK (status IN ('VALID', 'USED')),
    validation_token VARCHAR(128) NOT NULL UNIQUE,
    manual_code VARCHAR(32) NOT NULL UNIQUE,
    share_token VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    used_by_gate_user_id UUID REFERENCES users(id),
    CONSTRAINT uq_tickets_reservation_ordinal UNIQUE (reservation_id, ordinal)
);

CREATE INDEX idx_tickets_reservation_id ON tickets(reservation_id);
CREATE INDEX idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX idx_tickets_event_id ON tickets(event_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_validation_token ON tickets(validation_token);
CREATE INDEX idx_tickets_manual_code ON tickets(manual_code);
CREATE INDEX idx_tickets_share_token ON tickets(share_token);
