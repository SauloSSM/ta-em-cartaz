import { afterEach, describe, expect, it, vi } from 'vitest';
import { getSession, login, logout } from './authApi';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
});

describe('authApi', () => {
  it('usa mesma origem e envia o CSRF sem expor sessão no JavaScript', async () => {
    document.cookie = 'XSRF-TOKEN=csrf%20token; Path=/';
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: { id: '00000000-0000-0000-0000-000000000002', email: 'customer@example.com', role: 'CUSTOMER' },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    globalThis.fetch = fetchMock;

    await getSession();
    await login('customer@example.com', 'secret');
    await logout();

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/v1/auth/session');
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: 'same-origin' });
    expect(fetchMock.mock.calls[1]?.[1]).toMatchObject({
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': 'csrf token',
      },
      body: '{"email":"customer@example.com","password":"secret"}',
    });
    expect(fetchMock.mock.calls[1]?.[0]).toBe('/api/v1/auth/login');
    expect(fetchMock.mock.calls[2]?.[1]).toMatchObject({
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'X-XSRF-TOKEN': 'csrf token' },
    });
  });

  it('recusa payload de sessão que diverge do OpenAPI', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      authenticated: false,
      user: { id: 'unexpected' },
    }));

    await expect(getSession()).rejects.toMatchObject({ code: 'AUTH_INVALID_RESPONSE' });
  });

  it('recusa resposta anônima no endpoint de login', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ authenticated: false }));

    await expect(login('customer@example.com', 'secret')).rejects.toMatchObject({
      code: 'AUTH_INVALID_RESPONSE',
    });
  });

  it('considera logout bem-sucedido somente com status 204', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ authenticated: false }, 200));

    await expect(logout()).rejects.toMatchObject({ code: 'AUTH_UNAVAILABLE' });
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
