import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  sessionStorage.removeItem('edt.purchase-intent.v1');
  sessionStorage.removeItem('unrelated.preference');
  localStorage.removeItem('unrelated.local-preference');
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
    expect(screen.getByRole('heading', { level: 2, name: 'Meus Eventos' })).toBeDefined();
  });

  it('não exibe gestão de eventos para papéis diferentes de ORGANIZER', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      authenticated: true,
      user: {
        id: '00000000-0000-0000-0000-000000000002',
        email: 'customer@demo.elitedevticket.local',
        role: 'CUSTOMER',
      },
    }));

    render(<App />);

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('Cliente').textContent).toBe('Cliente');
    expect(screen.queryByRole('heading', { level: 2, name: 'Meus Eventos' })).toBeNull();
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

  it('revela e oculta a senha com nome, estado e foco acessíveis', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ authenticated: false }));
    const user = userEvent.setup();
    render(<App />);
    const password = await screen.findByLabelText('Senha') as HTMLInputElement;
    const reveal = screen.getByRole('button', { name: 'Mostrar senha' });

    await user.type(password, 'password');
    await user.click(reveal);

    expect(password.type).toBe('text');
    expect(reveal.getAttribute('aria-pressed')).toBe('true');
    expect(document.activeElement).toBe(reveal);

    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }));
    expect(password.type).toBe('password');
    expect(reveal.getAttribute('aria-pressed')).toBe('false');
    expect(document.activeElement).toBe(reveal);
  });

  it('expõe estado ocupado e rótulo contextual durante login', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    let completeLogin!: (response: Response) => void;
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      .mockReturnValueOnce(new Promise((resolve) => { completeLogin = resolve; }));
    const user = userEvent.setup();
    render(<App />);
    await user.type(await screen.findByLabelText('E-mail'), 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'password');

    void user.click(screen.getByRole('button', { name: 'Entrar' }));

    const busyButton = await screen.findByRole('button', { name: 'Entrando…' });
    expect(busyButton.closest('form')?.getAttribute('aria-busy')).toBe('true');
    completeLogin(jsonResponse({
      authenticated: true,
      user: {
        id: '00000000-0000-0000-0000-000000000002',
        email: 'customer.one@demo.elitedevticket.local',
        role: 'CUSTOMER',
      },
    }));
    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
  });

  it('mostra erro genérico seguro, preserva e-mail, limpa senha e encerra busy', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      .mockResolvedValueOnce(jsonResponse({ message: 'detalhe interno' }, 500));
    const user = userEvent.setup();
    render(<App />);
    const email = await screen.findByLabelText('E-mail') as HTMLInputElement;
    await user.type(email, 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect((await screen.findByRole('alert')).textContent).toBe('Não foi possível entrar. Tente novamente.');
    expect(email.value).toBe('customer.one@demo.elitedevticket.local');
    expect((screen.getByLabelText('Senha') as HTMLInputElement).value).toBe('');
    const form = screen.getByRole('button', { name: 'Entrar' }).closest('form');
    expect(form?.getAttribute('aria-busy')).toBe('false');
    expect((screen.getByRole('button', { name: 'Entrar' }) as HTMLButtonElement).disabled).toBe(false);
  });

  it('oferece retry acessível quando o bootstrap falha e permite login após emitir CSRF', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=csrf-after-retry; Path=/';
        return jsonResponse({ authenticated: false });
      })
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

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Não foi possível verificar sua sessão. Tente novamente.',
    );
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    await user.type(await screen.findByLabelText('E-mail'), 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    const loginCall = vi.mocked(globalThis.fetch).mock.calls[2];
    expect((loginCall[1]?.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('csrf-after-retry');
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

  it('preserva identidade e intenção, anuncia falha de logout e reabilita retry', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    sessionStorage.setItem('edt.purchase-intent.v1', '{"eventId":"future"}');
    let completeLogout!: (response: Response) => void;
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000004',
          email: 'gate@demo.elitedevticket.local',
          role: 'GATE',
        },
      }))
      .mockReturnValueOnce(new Promise((resolve) => { completeLogout = resolve; }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    render(<App />);

    void user.click(await screen.findByRole('button', { name: 'Sair e trocar de conta' }));
    const busyButton = await screen.findByRole('button', { name: 'Saindo…' });
    expect(busyButton.closest('section')?.getAttribute('aria-busy')).toBe('true');
    completeLogout(jsonResponse({ code: 'AUTH_UNAVAILABLE' }, 503));

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Não foi possível encerrar a sessão. Tente novamente.',
    );
    expect(screen.getByText('gate@demo.elitedevticket.local')).toBeTruthy();
    expect(sessionStorage.getItem('edt.purchase-intent.v1')).toBe('{"eventId":"future"}');
    const retry = screen.getByRole('button', { name: 'Sair e trocar de conta' }) as HTMLButtonElement;
    expect(retry.disabled).toBe(false);
    expect(retry.closest('section')?.getAttribute('aria-busy')).toBe('false');

    await user.click(retry);
    await screen.findByRole('heading', { level: 2, name: 'Entrar com conta provisionada' });
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

  it('organizador visualiza Meus Eventos, busca no catálogo, cria rascunho, edita e navega entre superfícies', async () => {
    document.cookie = 'XSRF-TOKEN=test-csrf; Path=/';
    globalThis.fetch = vi.fn<typeof fetch>()
      // 1. Initial session check
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000001',
          email: 'organizer@demo.elitedevticket.local',
          role: 'ORGANIZER',
        },
      }))
      // 2. Load my events (initial empty list)
      .mockResolvedValueOnce(jsonResponse({ events: [] }))
      // 3. Search catalog
      .mockResolvedValueOnce(jsonResponse({
        events: [
          {
            externalId: 'tm-rock',
            title: 'Rock in Rio 2026',
            description: 'Festival de música',
            category: 'Rock',
            imageUrl: 'https://images.example.com/rock.jpg',
          },
        ],
      }))
      // 4. Create draft
      .mockResolvedValueOnce(jsonResponse(
        {
          id: '99999999-9999-9999-9999-999999999999',
          organizerId: '00000000-0000-0000-0000-000000000001',
          externalId: 'tm-rock',
          title: 'Rock in Rio 2026',
          description: 'Festival de música',
          imageUrl: 'https://images.example.com/rock.jpg',
          category: 'Rock',
          status: 'DRAFT',
          createdAt: '2026-08-15T12:00:00Z',
          updatedAt: '2026-08-15T12:00:00Z',
        },
        201,
      ))
      // 5. Update draft
      .mockResolvedValueOnce(jsonResponse(
        {
          id: '99999999-9999-9999-9999-999999999999',
          organizerId: '00000000-0000-0000-0000-000000000001',
          externalId: 'tm-rock',
          title: 'Rock in Rio 2026 - Edição Atualizada',
          description: 'Festival de música',
          imageUrl: 'https://images.example.com/rock.jpg',
          category: 'Rock',
          status: 'DRAFT',
          createdAt: '2026-08-15T12:00:00Z',
          updatedAt: '2026-08-15T12:30:00Z',
        },
        200,
      ))
      // 6. Reload my events on returning
      .mockResolvedValueOnce(jsonResponse({
        events: [
          {
            id: '99999999-9999-9999-9999-999999999999',
            organizerId: '00000000-0000-0000-0000-000000000001',
            title: 'Rock in Rio 2026 - Edição Atualizada',
            category: 'Rock',
            status: 'DRAFT',
            createdAt: '2026-08-15T12:00:00Z',
            updatedAt: '2026-08-15T12:30:00Z',
          },
        ],
      }));

    const user = userEvent.setup();
    render(<App />);

    // 1. Surface S09: Meus Eventos loads empty
    await screen.findByRole('heading', { level: 2, name: 'Meus Eventos' });
    expect(await screen.findByText('Você ainda não possui eventos cadastrados.')).toBeDefined();

    // 2. Click "+ Novo evento do catálogo" -> navigates to S10
    await user.click(screen.getByRole('button', { name: '+ Novo evento do catálogo' }));

    // 3. Surface S10: Search catalog
    await screen.findByRole('heading', { level: 2, name: 'Pesquisar referências Ticketmaster' });
    const searchInput = screen.getByLabelText('Palavra-chave do evento');
    await user.type(searchInput, 'Rock');
    await user.click(screen.getByRole('button', { name: 'Buscar referências' }));

    // 4. Click "Usar como referência" to create draft
    const selectBtn = await screen.findByRole('button', { name: 'Usar Rock in Rio 2026 como referência' });
    await user.click(selectBtn);

    // 5. Click "Abrir rascunho no editor →" -> navigates to S11
    const openDraftBtn = await screen.findByRole('button', { name: 'Abrir rascunho no editor →' });
    await user.click(openDraftBtn);

    // 6. Surface S11: Editor
    await screen.findByRole('heading', { level: 2, name: 'Editor de Evento' });
    expect(screen.getByLabelText('Status do evento: DRAFT')).toBeDefined();

    // 7. Edit title and click "Salvar alterações"
    const titleInput = screen.getByLabelText(/Título do Evento/);
    await user.clear(titleInput);
    await user.type(titleInput, 'Rock in Rio 2026 - Edição Atualizada');
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByText('Alterações salvas com sucesso!')).toBeDefined();

    // 8. Click "← Voltar para Meus Eventos" -> navigates back to S09
    const backBtn = screen.getByRole('button', { name: 'Voltar para a lista de eventos' });
    await user.click(backBtn);

    // 9. Surface S09 is rendered
    expect(await screen.findByRole('heading', { level: 2, name: 'Meus Eventos' })).toBeDefined();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
