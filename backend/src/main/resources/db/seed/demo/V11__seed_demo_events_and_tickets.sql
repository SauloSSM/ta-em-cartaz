-- Seed demo events, ticket sectors, reservations, payments, tickets and validation history for evaluation (Story 8.1)
-- Preserves domain invariants: User -> Event -> Sector -> Reservation -> Payment -> Ticket

-- 1. Demo Events (PUBLISHED)
INSERT INTO events (
    id,
    organizer_id,
    external_source,
    external_id,
    title,
    description,
    image_url,
    category,
    status,
    venue_name,
    venue_address,
    starts_at,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL',
    NULL,
    'Show Acústico de Demonstração (Event A)',
    'Evento publicado de demonstração A para jornadas de compra Customer e validação Gate.',
    'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80',
    'Música',
    'PUBLISHED',
    'Auditório Ibirapuera',
    'Av. Pedro Álvares Cabral, s/n - Moema, São Paulo - SP',
    '2026-11-20 20:00:00+00',
    '2026-08-01 10:00:00+00',
    '2026-08-01 10:00:00+00'
),
(
    '00000000-0000-0000-0001-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL',
    NULL,
    'Festival Indie Brasil (Event B)',
    'Evento publicado de demonstração B para validação de contexto e teste WRONG_EVENT na portaria.',
    'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80',
    'Festival',
    'PUBLISHED',
    'Arena Anhembi',
    'Av. Olavo Fontoura, 1209 - Santana, São Paulo - SP',
    '2026-12-05 16:00:00+00',
    '2026-08-01 10:00:00+00',
    '2026-08-01 10:00:00+00'
);

-- 2. Ticket Sectors
INSERT INTO ticket_sectors (
    id,
    event_id,
    name,
    description,
    capacity,
    available_quantity,
    price,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0001-000000000001',
    'Pista Premium',
    'Acesso à área em frente ao palco',
    100,
    98,
    150.00,
    '2026-08-01 10:00:00+00',
    '2026-08-01 10:00:00+00'
),
(
    '00000000-0000-0000-0002-000000000002',
    '00000000-0000-0000-0001-000000000001',
    'Camarote VIP',
    'Área VIP elevada com vista privilegiada',
    50,
    50,
    320.00,
    '2026-08-01 10:00:00+00',
    '2026-08-01 10:00:00+00'
),
(
    '00000000-0000-0000-0002-000000000003',
    '00000000-0000-0000-0001-000000000002',
    'Pista Geral',
    'Acesso geral ao gramado',
    200,
    199,
    120.00,
    '2026-08-01 10:00:00+00',
    '2026-08-01 10:00:00+00'
);

-- 3. Reservations (CONFIRMED)
INSERT INTO reservations (
    id,
    customer_id,
    event_id,
    sector_id,
    quantity,
    unit_price,
    total_amount,
    status,
    expires_at,
    created_at,
    confirmed_at
) VALUES
(
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    2,
    150.00,
    300.00,
    'CONFIRMED',
    '2026-08-02 10:10:00+00',
    '2026-08-02 10:00:00+00',
    '2026-08-02 10:05:00+00'
),
(
    '00000000-0000-0000-0003-000000000002',
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0001-000000000002',
    '00000000-0000-0000-0002-000000000003',
    1,
    120.00,
    120.00,
    'CONFIRMED',
    '2026-08-02 11:10:00+00',
    '2026-08-02 11:00:00+00',
    '2026-08-02 11:05:00+00'
);

-- 4. Payments (APPROVED)
INSERT INTO payments (
    id,
    reservation_id,
    customer_id,
    amount,
    currency,
    status,
    provider,
    decline_reason,
    fingerprint,
    created_at,
    processed_at
) VALUES
(
    '00000000-0000-0000-0004-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0000-000000000002',
    300.00,
    'BRL',
    'APPROVED',
    'SIMULATED',
    NULL,
    'be6fefaa1d835511d29fa086d0cf568f7548a1dd30373ec8172b63cc10923f65',
    '2026-08-02 10:05:00+00',
    '2026-08-02 10:05:00+00'
),
(
    '00000000-0000-0000-0004-000000000002',
    '00000000-0000-0000-0003-000000000002',
    '00000000-0000-0000-0000-000000000003',
    120.00,
    'BRL',
    'APPROVED',
    'SIMULATED',
    NULL,
    '367faeb97e205b16e6a3c54d4d4acaccce86246e1db65fb6e4bc21b45f5d4f25',
    '2026-08-02 11:05:00+00',
    '2026-08-02 11:05:00+00'
);

-- 5. Tickets
-- Ticket A1: Event A, Customer 1, VALID
-- Ticket A2: Event A, Customer 1, USED
-- Ticket B1: Event B, Customer 2, VALID
INSERT INTO tickets (
    id,
    reservation_id,
    event_id,
    sector_id,
    customer_id,
    ordinal,
    status,
    validation_token,
    manual_code,
    share_token,
    created_at,
    used_at,
    used_by_gate_user_id
) VALUES
(
    '00000000-0000-0000-0005-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0000-000000000002',
    1,
    'VALID',
    '00000000000000000000000000000000000000000000000000000000000000a1',
    'DEM0A1C0DE',
    '10000000000000000000000000000000000000000000000000000000000000a1',
    '2026-08-02 10:05:00+00',
    NULL,
    NULL
),
(
    '00000000-0000-0000-0005-000000000002',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0000-000000000002',
    2,
    'USED',
    '00000000000000000000000000000000000000000000000000000000000000a2',
    'DEM0A2C0DE',
    '10000000000000000000000000000000000000000000000000000000000000a2',
    '2026-08-02 10:05:00+00',
    '2026-08-03 14:00:00+00',
    '00000000-0000-0000-0000-000000000004'
),
(
    '00000000-0000-0000-0005-000000000003',
    '00000000-0000-0000-0003-000000000002',
    '00000000-0000-0000-0001-000000000002',
    '00000000-0000-0000-0002-000000000003',
    '00000000-0000-0000-0000-000000000003',
    1,
    'VALID',
    '00000000000000000000000000000000000000000000000000000000000000b1',
    'DEM0B1C0DE',
    '10000000000000000000000000000000000000000000000000000000000000b1',
    '2026-08-02 11:05:00+00',
    NULL,
    NULL
);

-- 6. Validation Attempt History (for initial use of Ticket A2)
INSERT INTO validation_attempts (
    id,
    gate_user_id,
    selected_event_id,
    ticket_id,
    validation_method,
    result,
    fingerprint,
    processed_at,
    created_at
) VALUES
(
    '00000000-0000-0000-0006-000000000001',
    '00000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0005-000000000002',
    'MANUAL',
    'VALID',
    '9849546637b1b36d21100768e270fd61ff782e99e2328a25d7f8877687dab3cf',
    '2026-08-03 14:00:00+00',
    '2026-08-03 14:00:00+00'
);
