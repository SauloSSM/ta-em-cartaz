import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { createDraftEvent, getEvent, EventClientError } from './eventsApi';

describe('eventsApi', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    vi.restoreAllMocks();
  });

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  describe('createDraftEvent', () => {
    it('sends POST request with CSRF headers and parses created draft', async () => {
      document.cookie = 'XSRF-TOKEN=test-csrf-token; Path=/';

      const mockResponse = {
        id: '123e4567-e89b-12d3-a456-426614174000',
        organizerId: '00000000-0000-0000-0000-000000000001',
        externalId: 'tm-100',
        title: 'Rock in Rio',
        description: 'Festival',
        imageUrl: 'https://images.example.com/rock.jpg',
        category: 'Music',
        status: 'DRAFT',
        createdAt: '2026-08-15T12:00:00Z',
        updatedAt: '2026-08-15T12:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 201,
        json: () => Promise.resolve(mockResponse),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await createDraftEvent(
        'Rock in Rio',
        'tm-100',
        'Festival',
        'https://images.example.com/rock.jpg',
        'Music',
      );

      expect(fetchMock).toHaveBeenCalledWith('/api/v1/events/drafts', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': 'test-csrf-token',
        },
        body: JSON.stringify({
          title: 'Rock in Rio',
          externalId: 'tm-100',
          description: 'Festival',
          imageUrl: 'https://images.example.com/rock.jpg',
          category: 'Music',
        }),
      });

      expect(result).toEqual(mockResponse);
    });

    it('throws EventClientError on 400 bad request', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: () => Promise.resolve({
          code: 'AUTH_INVALID_REQUEST',
          message: 'Título do evento é obrigatório.',
          traceId: 'tr-1',
          timestamp: '2026-08-15T12:00:00Z',
        }),
      }));

      await expect(createDraftEvent('')).rejects.toThrow(EventClientError);
    });

    it('throws EventClientError on 403 forbidden', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: () => Promise.resolve({
          code: 'AUTH_FORBIDDEN',
          message: 'Acesso negado.',
          traceId: 'tr-2',
          timestamp: '2026-08-15T12:00:00Z',
        }),
      }));

      await expect(createDraftEvent('Show')).rejects.toThrow(EventClientError);
    });

    it('throws EventClientError on malformed response body', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: true,
        status: 201,
        json: () => Promise.resolve({ unexpected: 'shape' }),
      }));

      await expect(createDraftEvent('Show')).rejects.toThrow(
        'Resposta do servidor de eventos inválida.',
      );
    });
  });

  describe('getEvent', () => {
    it('sends GET request and parses event response', async () => {
      const mockEvent = {
        id: '123e4567-e89b-12d3-a456-426614174000',
        organizerId: '00000000-0000-0000-0000-000000000001',
        title: 'Festival de Inverno',
        status: 'DRAFT',
        createdAt: '2026-08-15T12:00:00Z',
        updatedAt: '2026-08-15T12:00:00Z',
      };

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockEvent),
      });
      vi.stubGlobal('fetch', fetchMock);

      const result = await getEvent('123e4567-e89b-12d3-a456-426614174000');

      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/events/123e4567-e89b-12d3-a456-426614174000',
        {
          method: 'GET',
          credentials: 'same-origin',
          headers: {
            Accept: 'application/json',
          },
        },
      );

      expect(result).toEqual(mockEvent);
    });

    it('throws EventClientError on 404 not found', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: () => Promise.resolve({
          code: 'EVENT_NOT_FOUND',
          message: 'Evento não encontrado.',
          traceId: 'tr-3',
          timestamp: '2026-08-15T12:00:00Z',
        }),
      }));

      await expect(getEvent('unknown-id')).rejects.toThrow(EventClientError);
    });
  });
});
