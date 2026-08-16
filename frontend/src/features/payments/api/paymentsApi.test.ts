import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import {
  processPayment,
  generatePaymentAttemptId,
  PaymentClientError,
  type PaymentResponse,
} from './paymentsApi';

describe('paymentsApi', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    vi.restoreAllMocks();
  });

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  describe('generatePaymentAttemptId', () => {
    it('generates a valid UUID string', () => {
      const id = generatePaymentAttemptId();
      expect(typeof id).toBe('string');
      expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
    });
  });

  describe('processPayment', () => {
    it('sends POST request with CSRF token and returns DECLINED payment response', async () => {
      document.cookie = 'XSRF-TOKEN=test-csrf-token; Path=/';

      const mockResponse: PaymentResponse = {
        id: '11111111-1111-1111-1111-111111111111',
        reservationId: '22222222-2222-2222-2222-222222222222',
        amount: 300.0,
        currency: 'BRL',
        status: 'DECLINED',
        provider: 'FAKE',
        declineReason: 'SIMULATED_DECLINE',
        createdAt: '2026-08-16T14:00:00Z',
        processedAt: '2026-08-16T14:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponse),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await processPayment(
        '22222222-2222-2222-2222-222222222222',
        {
          paymentAttemptId: '11111111-1111-1111-1111-111111111111',
          simulatedOutcome: 'DECLINED',
        },
      );

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/reservations/22222222-2222-2222-2222-222222222222/payments',
        {
          method: 'POST',
          credentials: 'same-origin',
          headers: expect.any(Headers),
          body: JSON.stringify({
            paymentAttemptId: '11111111-1111-1111-1111-111111111111',
            simulatedOutcome: 'DECLINED',
          }),
        },
      );
      expect(result).toEqual(mockResponse);
    });

    it('throws PaymentClientError on 422 reservation expired', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: () => Promise.resolve({
          code: 'RESERVATION_EXPIRED',
          message: 'A reserva expirou.',
          traceId: 'tr-expired',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        processPayment('r1', { paymentAttemptId: 'p1', simulatedOutcome: 'DECLINED' }),
      ).rejects.toThrow(PaymentClientError);
    });

    it('throws PaymentClientError on 409 idempotency conflict', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 409,
        json: () => Promise.resolve({
          code: 'IDEMPOTENCY_CONFLICT',
          message: 'Chave de idempotência reutilizada com dados conflitantes.',
          traceId: 'tr-conflict',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        processPayment('r1', { paymentAttemptId: 'p1', simulatedOutcome: 'DECLINED' }),
      ).rejects.toThrow(PaymentClientError);
    });

    it('throws PaymentClientError on 401 unauthenticated', async () => {
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
        processPayment('r1', { paymentAttemptId: 'p1', simulatedOutcome: 'DECLINED' }),
      ).rejects.toThrow(PaymentClientError);
    });

    it('throws PaymentClientError on 403 forbidden', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: () => Promise.resolve({
          code: 'AUTH_FORBIDDEN',
          message: 'Acesso restrito a clientes proprietários da reserva.',
          traceId: 'tr-forbidden',
          timestamp: '2026-08-16T14:00:00Z',
        }),
      }));

      await expect(
        processPayment('r1', { paymentAttemptId: 'p1', simulatedOutcome: 'DECLINED' }),
      ).rejects.toThrow(PaymentClientError);
    });
  });
});
