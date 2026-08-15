CREATE TABLE events (
    id UUID PRIMARY KEY,
    organizer_id UUID NOT NULL REFERENCES users(id),
    external_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(2048),
    category VARCHAR(100),
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    venue VARCHAR(255),
    starts_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_events_organizer_id ON events(organizer_id);
