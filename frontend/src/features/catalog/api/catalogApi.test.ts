import { afterEach, describe, expect, it, vi } from 'vitest';
import { searchCatalogEvents, CatalogClientError } from './catalogApi';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

describe('catalogApi', () => {
  it('realiza busca com palavra-chave e retorna lista tipada com sucesso', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({
        events: [
          {
            externalId: 'tm-101',
            title: 'Rock in Rio 2026',
            description: 'Festival de música',
            imageUrl: 'https://images.example.com/live.jpg',
            category: 'Rock',
          },
        ],
      }),
    );
    globalThis.fetch = fetchMock;

    const response = await searchCatalogEvents('Rock in Rio');

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/v1/catalog/events?keyword=Rock%20in%20Rio');
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({
      method: 'GET',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
      },
    });
    expect(response.events).toHaveLength(1);
    expect(response.events[0]).toEqual({
      externalId: 'tm-101',
      title: 'Rock in Rio 2026',
      description: 'Festival de música',
      imageUrl: 'https://images.example.com/live.jpg',
      category: 'Rock',
    });
  });

  it('realiza busca sem keyword e chama endpoint sem query param', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({ events: [] }),
    );
    globalThis.fetch = fetchMock;

    const response = await searchCatalogEvents('');

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/v1/catalog/events');
    expect(response.events).toEqual([]);
  });

  it('mapeia resposta de erro 503 CATALOG_UNAVAILABLE para CatalogClientError', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      jsonResponse(
        {
          code: 'CATALOG_UNAVAILABLE',
          message: 'Catálogo Ticketmaster temporariamente indisponível.',
          traceId: '12345',
          timestamp: '2026-08-15T12:00:00Z',
        },
        503,
      ),
    );

    await expect(searchCatalogEvents('Rock')).rejects.toThrow(CatalogClientError);
    await expect(searchCatalogEvents('Rock')).rejects.toMatchObject({
      code: 'CATALOG_UNAVAILABLE',
      message: 'Catálogo Ticketmaster temporariamente indisponível.',
    });
  });

  it('mapeia respostas 401 e 403 com códigos apropriados', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse(
        {
          code: 'AUTH_UNAUTHENTICATED',
          message: 'Sessão expirada.',
          traceId: '12345',
          timestamp: '2026-08-15T12:00:00Z',
        },
        401,
      ),
    );

    await expect(searchCatalogEvents('Rock')).rejects.toMatchObject({
      code: 'AUTH_UNAUTHENTICATED',
    });
  });

  it('recusa respostas com estrutura divergente do OpenAPI', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({
        events: [
          {
            invalidField: 'something',
          },
        ],
      }),
    );

    await expect(searchCatalogEvents('Rock')).rejects.toMatchObject({
      code: 'CATALOG_INVALID_RESPONSE',
    });
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
