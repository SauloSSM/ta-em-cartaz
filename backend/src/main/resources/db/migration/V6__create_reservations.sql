CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    sector_id UUID NOT NULL REFERENCES ticket_sectors(id),
    quantity INT NOT NULL CHECK (quantity >= 1 AND quantity <= 6),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('HOLDING', 'CONFIRMED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_reservations_customer_id ON reservations(customer_id);
CREATE INDEX idx_reservations_event_id ON reservations(event_id);
CREATE INDEX idx_reservations_sector_id ON reservations(sector_id);
CREATE INDEX idx_reservations_customer_event ON reservations(customer_id, event_id);
CREATE INDEX idx_reservations_status_expires_at ON reservations(status, expires_at);
