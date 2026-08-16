import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PublicSharedTicket } from './PublicSharedTicket';
import type { PublicTicketResponse } from '../api/ticketsApi';
import type { EventResponse, TicketSectorListResponse } from '../../events/api/eventsApi';

describe('PublicSharedTicket (Superfície S08)', () => {
  const sampleTicket: PublicTicketResponse = {
    id: '11111111-1111-1111-1111-111111111111',
    eventId: '22222222-2222-2222-2222-222222222222',
    sectorId: '33333333-3333-3333-3333-333333333333',
    ordinal: 1,
    status: 'VALID',
    manualCode: 'AB7K92QX4M',
    shareToken: 'share-token-valid-12345678901234567890123456789012',
    validationToken: 'val-token-valid-12345678901234567890123456789012',
    createdAt: '2026-08-16T14:00:00Z',
  };

  const sampleEvent: EventResponse = {
    id: '22222222-2222-2222-2222-222222222222',
    organizerId: '00000000-0000-0000-0000-000000000001',
    title: 'Rock in Rio 2026',
    category: 'Música',
    description: 'Maior festival de música do mundo',
    venueName: 'Cidade do Rock',
    venueAddress: 'Av. Salvador Allende, 6500',
    startsAt: '2026-09-20T21:00:00Z',
    status: 'PUBLISHED',
    createdAt: '2026-08-16T12:00:00Z',
    updatedAt: '2026-08-16T12:00:00Z',
  };

  const sampleSectors: TicketSectorListResponse = {
    sectors: [
      {
        id: '33333333-3333-3333-3333-333333333333',
        eventId: '22222222-2222-2222-2222-222222222222',
        name: 'Pista Premium',
        description: 'Em frente ao Palco Mundo',
        capacity: 500,
        availableQuantity: 400,
        price: 350,
        createdAt: '2026-08-16T12:00:00Z',
        updatedAt: '2026-08-16T12:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('exibe detalhes do ingresso público válido com QR, código manual, evento e setor sem PII', async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/v1/public/tickets/')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleTicket),
        });
      }
      if (url.endsWith('/sectors')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleSectors),
        });
      }
      if (url.includes('/api/v1/events/')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleEvent),
        });
      }
      return Promise.reject(new Error('Unexpected URL: ' + url));
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<PublicSharedTicket shareToken="share-token-valid-12345678901234567890123456789012" />);

    // Header & event info
    expect(await screen.findByRole('heading', { level: 2, name: 'Rock in Rio 2026' })).toBeDefined();
    expect(screen.getByText('Ingresso #1')).toBeDefined();
    expect(screen.getByTestId('shared-ticket-status-badge').textContent).toBe('Válido');
    expect(screen.getByText(/Pista Premium/)).toBeDefined();
    expect(screen.getByText(/Cidade do Rock — Av. Salvador Allende, 6500/)).toBeDefined();

    // QR & manual code
    expect(screen.getByTestId('qrcode-panel')).toBeDefined();
    expect(screen.getByText('AB7K-92QX-4M')).toBeDefined();

    // Zero PII
    expect(screen.queryByText(/customer/i)).toBeNull();
    expect(screen.queryByText(/reservation/i)).toBeNull();
    expect(screen.queryByTestId('shared-ticket-used-notice')).toBeNull();
  });

  it('exibe alerta de ingresso utilizado quando status é USED antes das credenciais', async () => {
    const usedTicket: PublicTicketResponse = {
      ...sampleTicket,
      status: 'USED',
    };

    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/v1/public/tickets/')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(usedTicket),
        });
      }
      if (url.endsWith('/sectors')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleSectors),
        });
      }
      if (url.includes('/api/v1/events/')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleEvent),
        });
      }
      return Promise.reject(new Error('Unexpected URL: ' + url));
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<PublicSharedTicket shareToken="share-token-used" />);

    expect(await screen.findByRole('heading', { level: 2, name: 'Rock in Rio 2026' })).toBeDefined();
    expect(screen.getByTestId('shared-ticket-status-badge').textContent).toBe('Utilizado');

    // Alerta textual precede a credencial
    const usedNotice = screen.getByTestId('shared-ticket-used-notice');
    expect(usedNotice).toBeDefined();
    expect(usedNotice.textContent).toContain('Ingresso Utilizado');
    expect(usedNotice.textContent).toContain('não autoriza nova entrada');

    // Credenciais ainda visíveis para conferência histórica
    expect(screen.getByTestId('qrcode-panel')).toBeDefined();
    expect(screen.getByText('AB7K-92QX-4M')).toBeDefined();
  });

  it('exibe mensagem neutra e botão de tentar novamente em caso de link inválido/inexistente', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({
        code: 'TICKET_NOT_FOUND',
        message: 'Ingresso não encontrado.',
        traceId: 'tr-404',
        timestamp: '2026-08-16T14:00:00Z',
      }),
    }));

    const onBrowseCatalog = vi.fn();
    render(
      <PublicSharedTicket
        shareToken="invalid-token-123"
        onBrowseCatalog={onBrowseCatalog}
      />
    );

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert.textContent).toContain('Ingresso não encontrado ou link inválido.');
    // Não vaza o token bruto nem dados internos
    expect(errorAlert.textContent).not.toContain('invalid-token-123');

    // Botões de ação
    expect(screen.getByTestId('shared-ticket-retry-btn')).toBeDefined();
    const browseBtn = screen.getByTestId('shared-ticket-browse-catalog-btn');
    expect(browseBtn).toBeDefined();

    const user = userEvent.setup();
    await user.click(browseBtn);
    expect(onBrowseCatalog).toHaveBeenCalledTimes(1);
  });

  it('permite navegar para o catálogo a partir do link de navegação', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/v1/public/tickets/')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(sampleTicket),
        });
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ sectors: [], events: [] }),
      });
    }));

    const onBrowseCatalog = vi.fn();
    render(
      <PublicSharedTicket
        shareToken="share-token-valid-12345678901234567890123456789012"
        onBrowseCatalog={onBrowseCatalog}
      />
    );

    const backBtn = await screen.findByTestId('shared-ticket-back-to-catalog-btn');
    const user = userEvent.setup();
    await user.click(backBtn);
    expect(onBrowseCatalog).toHaveBeenCalledTimes(1);
  });
});
