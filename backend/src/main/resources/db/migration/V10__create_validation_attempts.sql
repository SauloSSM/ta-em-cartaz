CREATE TABLE validation_attempts (
    id UUID PRIMARY KEY,
    gate_user_id UUID NOT NULL REFERENCES users(id),
    selected_event_id UUID NOT NULL REFERENCES events(id),
    ticket_id UUID REFERENCES tickets(id),
    validation_method VARCHAR(16) NOT NULL CHECK (validation_method IN ('MANUAL', 'QR')),
    result VARCHAR(32) NOT NULL CHECK (result IN ('VALID', 'INVALID', 'ALREADY_USED', 'WRONG_EVENT')),
    fingerprint VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_validation_attempts_gate_user_id ON validation_attempts(gate_user_id);
CREATE INDEX idx_validation_attempts_selected_event_id ON validation_attempts(selected_event_id);
CREATE INDEX idx_validation_attempts_ticket_id ON validation_attempts(ticket_id);
CREATE INDEX idx_validation_attempts_result ON validation_attempts(result);
