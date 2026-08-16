import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import {
  createReservation,
  ReservationClientError,
  type ReservationResponse,
} from './reservationsApi';

describe('reservationsApi', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    vi.restoreAllMocks();
  });

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  describe('createReservation', () => {
    it('sends POST request with CSRF token and returns holding reservation', async () => {
      document.cookie = 'XSRF-TOKEN=test-csrf-token; Path=/';

      const mockResponse: ReservationResponse = {
        id: '11111111-1111-1111-1111-111111111111',
        customerId: '22222222-2222-2222-2222-222222222222',
        eventId: '33333333-3333-3333-3333-333333333333',
        sectorId: '44444444-4444-4444-4444-444444444444',
        quantity: 2,
        unitPrice: 150.0,
        totalAmount: 300.0,
        status: 'HOLDING',
        expiresAt: '2026-08-16T14:10:00Z',
        createdAt: '2026-08-16T14:00:00Z',
        serverNow: '2026-08-16T14:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 201,
        json: () => Promise.resolve(mockResponse),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await createReservation(
        '33333333-3333-3333-3333-333333333333',
        '44444444-4444-4444-4444-444444444444',
        { quantity: 2 },
      );

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/events/33333333-3333-3333-3333-333333333333/sectors/44444444-4444-4444-4444-444444444444/reservations',
        {
          method: 'POST',
          credentials: 'same-origin',
          headers: expect.any(Headers),
          body: JSON.stringify({ quantity: 2 }),
        },
      );
      expect(result).toEqual(mockResponse);
    });

    it('throws ReservationClientError on 422 insufficient availability', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: () => Promise.resolve({
          code: 'INSUFFICIENT_AVAILABILITY',
          message: 'Quantidade solicitada indisponível.',
          traceId: 'tr-insufficient',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        createReservation('e1', 's1', { quantity: 4 }),
      ).rejects.toThrow(ReservationClientError);
    });

    it('throws ReservationClientError on 401 unauthenticated', async () => {
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

      await expect(
        createReservation('e1', 's1', { quantity: 1 }),
      ).rejects.toThrow(ReservationClientError);
    });
  });
});
