import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
});

describe('App session flow', () => {
  it('mostra identidade e papel ao restaurar sessão autenticada', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      authenticated: true,
      user: {
        id: '00000000-0000-0000-0000-000000000001',
        email: 'organizer@demo.elitedevticket.local',
        role: 'ORGANIZER',
      },
    }));

    render(<App />);

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('organizer@demo.elitedevticket.local').textContent).toBe(
      'organizer@demo.elitedevticket.local',
    );
    expect(screen.getByText('Organizador').textContent).toBe('Organizador');
  });

  it('preserva o e-mail e move o foco para o erro genérico de credenciais', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      .mockResolvedValueOnce(jsonResponse({
        code: 'AUTH_INVALID_CREDENTIALS',
        message: 'E-mail ou senha inválidos.',
        traceId: 'trace',
        timestamp: '2026-08-14T12:00:00Z',
      }, 401));
    const user = userEvent.setup();
    render(<App />);
    const email = await screen.findByLabelText('E-mail');

    await user.type(email, 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    const error = await screen.findByRole('alert');
    expect(error.textContent).toBe('E-mail ou senha inválidos.');
    expect(document.activeElement).toBe(error);
    expect((screen.getByLabelText('E-mail') as HTMLInputElement).value).toBe(
      'customer.one@demo.elitedevticket.local',
    );
    expect((screen.getByLabelText('Senha') as HTMLInputElement).value).toBe('');
  });

  it('autentica credenciais válidas e mostra identidade e papel', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000002',
          email: 'customer.one@demo.elitedevticket.local',
          role: 'CUSTOMER',
        },
      }));
    const user = userEvent.setup();
    render(<App />);

    await user.type(await screen.findByLabelText('E-mail'), 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('customer.one@demo.elitedevticket.local')).toBeTruthy();
    expect(screen.getByText('Cliente')).toBeTruthy();
  });

  it('encerra sessão, remove apenas a chave reservada e permite trocar de conta', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    sessionStorage.setItem('edt.purchase-intent.v1', '{"eventId":"future"}');
    sessionStorage.setItem('unrelated.preference', 'keep');
    localStorage.setItem('unrelated.local-preference', 'keep');
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000004',
          email: 'gate@demo.elitedevticket.local',
          role: 'GATE',
        },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: 'Sair e trocar de conta' }));
    await screen.findByRole('heading', { level: 2, name: 'Entrar com conta provisionada' });

    expect(sessionStorage.getItem('edt.purchase-intent.v1')).toBeNull();
    expect(sessionStorage.getItem('unrelated.preference')).toBe('keep');
    expect(sessionStorage.length).toBe(1);
    expect(localStorage.getItem('unrelated.local-preference')).toBe('keep');
    await waitFor(() => expect((screen.getByLabelText('E-mail') as HTMLInputElement).disabled).toBe(false));
  });

  it('fica anônima mesmo quando a remoção da intenção falha após logout 204', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    sessionStorage.setItem('edt.purchase-intent.v1', '{"eventId":"future"}');
    sessionStorage.setItem('unrelated.preference', 'keep');
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000004',
          email: 'gate@demo.elitedevticket.local',
          role: 'GATE',
        },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const originalRemoveItem = Storage.prototype.removeItem;
    const removeItem = vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(function (this: Storage, key) {
      if (this === sessionStorage && key === 'edt.purchase-intent.v1') {
        throw new DOMException('storage indisponível');
      }
      originalRemoveItem.call(this, key);
    });

    try {
      const user = userEvent.setup();
      render(<App />);
      await user.click(await screen.findByRole('button', { name: 'Sair e trocar de conta' }));

      await screen.findByRole('heading', { level: 2, name: 'Entrar com conta provisionada' });
      expect(sessionStorage.getItem('edt.purchase-intent.v1')).toBe('{"eventId":"future"}');
      expect(sessionStorage.getItem('unrelated.preference')).toBe('keep');
    } finally {
      removeItem.mockRestore();
    }
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
