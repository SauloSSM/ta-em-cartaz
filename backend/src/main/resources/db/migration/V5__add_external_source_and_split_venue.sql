ALTER TABLE events ADD COLUMN external_source VARCHAR(64);
ALTER TABLE events ADD COLUMN venue_name VARCHAR(255);
ALTER TABLE events ADD COLUMN venue_address VARCHAR(500);

UPDATE events SET external_source = 'TICKETMASTER' WHERE external_id IS NOT NULL;
UPDATE events SET venue_name = venue, venue_address = venue WHERE venue IS NOT NULL;

ALTER TABLE events DROP COLUMN venue;
