import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  listMyTickets,
  getMyTicket,
  getPublicTicket,
  TicketClientError,
  type MyTicketResponse,
  type MyTicketListResponse,
} from './ticketsApi';

describe('ticketsApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('listMyTickets', () => {
    it('sends GET request to /api/v1/my-tickets and returns tickets list', async () => {
      const mockTicket: MyTicketResponse = {
        id: '11111111-1111-1111-1111-111111111111',
        reservationId: '22222222-2222-2222-2222-222222222222',
        eventId: '33333333-3333-3333-3333-333333333333',
        sectorId: '44444444-4444-4444-4444-444444444444',
        ordinal: 1,
        status: 'VALID',
        manualCode: 'AB7K92QX4M',
        shareToken: 'share-token-12345678901234567890123456789012',
        validationToken: 'val-token-12345678901234567890123456789012',
        createdAt: '2026-08-16T14:00:00Z',
      };
      const mockResponse: MyTicketListResponse = {
        tickets: [mockTicket],
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponse),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await listMyTickets();

      expect(fetchMock).toHaveBeenCalledWith('/api/v1/my-tickets', {
        method: 'GET',
        credentials: 'same-origin',
        headers: expect.any(Headers),
      });
      expect(result).toEqual(mockResponse);
    });

    it('throws TicketClientError on 401 unauthenticated', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        json: () => Promise.resolve({
          code: 'AUTH_UNAUTHENTICATED',
          message: 'Sessão expirada ou não autenticada.',
          traceId: 'tr-auth',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(listMyTickets()).rejects.toThrow(TicketClientError);
    });

    it('throws TicketClientError on 403 forbidden', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: () => Promise.resolve({
          code: 'AUTH_FORBIDDEN',
          message: 'Acesso restrito a clientes.',
          traceId: 'tr-forbidden',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(listMyTickets()).rejects.toThrow(TicketClientError);
    });
  });

  describe('getMyTicket', () => {
    it('sends GET request to /api/v1/my-tickets/:ticketId and returns ticket detail', async () => {
      const mockTicket: MyTicketResponse = {
        id: '11111111-1111-1111-1111-111111111111',
        reservationId: '22222222-2222-2222-2222-222222222222',
        eventId: '33333333-3333-3333-3333-333333333333',
        sectorId: '44444444-4444-4444-4444-444444444444',
        ordinal: 1,
        status: 'VALID',
        manualCode: 'AB7K92QX4M',
        shareToken: 'share-token-123',
        validationToken: 'val-token-123',
        createdAt: '2026-08-16T14:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockTicket),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await getMyTicket('11111111-1111-1111-1111-111111111111');

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/my-tickets/11111111-1111-1111-1111-111111111111',
        {
          method: 'GET',
          credentials: 'same-origin',
          headers: expect.any(Headers),
        },
      );
      expect(result).toEqual(mockTicket);
    });

    it('throws TicketClientError with TICKET_NOT_FOUND on 404', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: () => Promise.resolve({
          code: 'TICKET_NOT_FOUND',
          message: 'Ingresso não encontrado.',
          traceId: 'tr-notfound',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        getMyTicket('non-existent-or-not-owned-id'),
      ).rejects.toThrow(TicketClientError);
    });
  });

  describe('getPublicTicket', () => {
    it('sends GET request to /api/v1/public/tickets/:shareToken and returns public ticket', async () => {
      const mockPublicTicket = {
        id: '11111111-1111-1111-1111-111111111111',
        eventId: '33333333-3333-3333-3333-333333333333',
        sectorId: '44444444-4444-4444-4444-444444444444',
        ordinal: 1,
        status: 'VALID' as const,
        manualCode: 'AB7K92QX4M',
        shareToken: 'share-token-12345678901234567890123456789012',
        validationToken: 'val-token-12345678901234567890123456789012',
        createdAt: '2026-08-16T14:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockPublicTicket),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await getPublicTicket('share-token-12345678901234567890123456789012');

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/public/tickets/share-token-12345678901234567890123456789012',
        {
          method: 'GET',
          credentials: 'same-origin',
          headers: expect.any(Headers),
        },
      );
      expect(result).toEqual(mockPublicTicket);
    });

    it('throws TicketClientError on 404 for invalid shareToken', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: () => Promise.resolve({
          code: 'TICKET_NOT_FOUND',
          message: 'Ingresso não encontrado.',
          traceId: 'tr-notfound',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        getPublicTicket('invalid-token'),
      ).rejects.toThrow(TicketClientError);
    });
  });
});
