import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  sessionStorage.clear();
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
    render(<App initialAnonymousView="login" />);
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
    render(<App initialAnonymousView="login" />);
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
      .mockReturnValueOnce(new Promise((resolve) => { completeLogin = resolve; }))
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000002',
          email: 'customer.one@demo.elitedevticket.local',
          role: 'CUSTOMER',
        },
      }));
    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);
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
    render(<App initialAnonymousView="login" />);
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
      }))
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000002',
          email: 'customer.one@demo.elitedevticket.local',
          role: 'CUSTOMER',
        },
      }));
    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);

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
      }))
      .mockResolvedValueOnce(jsonResponse({
        authenticated: true,
        user: {
          id: '00000000-0000-0000-0000-000000000002',
          email: 'customer.one@demo.elitedevticket.local',
          role: 'CUSTOMER',
        },
      }));
    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);

    await user.type(await screen.findByLabelText('E-mail'), 'customer.one@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('customer.one@demo.elitedevticket.local')).toBeTruthy();
    expect(screen.getByText('Cliente')).toBeTruthy();
  });

  it('login success executa session bootstrap, materializa XSRF-TOKEN e primeira mutation inclui header CSRF', async () => {
    document.cookie = 'XSRF-TOKEN=initial-csrf; Path=/';
    const fetchCalls: Array<{ url: string; method?: string; headers?: Record<string, string> }> = [];

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';
      const headers = (init?.headers as Record<string, string>) || {};
      fetchCalls.push({ url, method, headers });

      if (url.includes('/api/v1/auth/session') && fetchCalls.length === 1) {
        return jsonResponse({ authenticated: false });
      }
      if (url.includes('/api/v1/auth/login')) {
        return jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000001',
            email: 'organizer@demo.elitedevticket.local',
            role: 'ORGANIZER',
          },
        });
      }
      if (url.includes('/api/v1/auth/session') && fetchCalls.length === 3) {
        // Simula a materialização do cookie XSRF-TOKEN realizada pelo GET /session no browser
        document.cookie = 'XSRF-TOKEN=materialized-after-session-bootstrap; Path=/';
        return jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000001',
            email: 'organizer@demo.elitedevticket.local',
            role: 'ORGANIZER',
          },
        });
      }
      if (url.includes('/api/v1/catalog/events')) {
        return jsonResponse({
          events: [
            {
              externalId: 'tm-rock-2026',
              title: 'Rock in Rio 2026',
              description: 'Festival de música',
              category: 'Música',
            },
          ],
        });
      }
      if (url.includes('/api/v1/events/mine')) {
        return jsonResponse({ events: [] });
      }
      if (url.includes('/api/v1/events/drafts') && method === 'POST') {
        return jsonResponse({
          id: 'ev-test-created-1',
          organizerId: '00000000-0000-0000-0000-000000000001',
          title: 'Rock in Rio 2026',
          status: 'DRAFT',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }, 201);
      }
      return jsonResponse({ events: [] });
    });

    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);

    await user.type(await screen.findByLabelText('E-mail'), 'organizer@demo.elitedevticket.local');
    await user.type(screen.getByLabelText('Senha'), 'password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    // Garante que o estado autenticado foi atingido e a tela do organizador foi carregada
    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('organizer@demo.elitedevticket.local')).toBeDefined();

    // Verifica que GET /session foi chamado após POST /login antes de qualquer mutação
    expect(fetchCalls[0].url).toContain('/api/v1/auth/session');
    expect(fetchCalls[1].url).toContain('/api/v1/auth/login');
    expect(fetchCalls[2].url).toContain('/api/v1/auth/session');

    // Executa a primeira mutation: criar rascunho a partir do catálogo
    await screen.findByRole('heading', { level: 2, name: 'Meus Eventos' });
    await user.click(screen.getByRole('button', { name: '+ Novo evento do catálogo' }));
    await screen.findByRole('heading', { level: 2, name: 'Pesquisar referências Ticketmaster' });
    const searchInput = screen.getByLabelText('Palavra-chave do evento');
    await user.type(searchInput, 'Rock');
    await user.click(screen.getByRole('button', { name: 'Buscar referências' }));
    const selectBtn = await screen.findByRole('button', { name: 'Usar Rock in Rio 2026 como referência' });
    await user.click(selectBtn);

    // Verifica que a primeira mutation já continha o header X-XSRF-TOKEN materializado
    const mutationCall = fetchCalls.find((c) => c.url.includes('/api/v1/events/drafts') && c.method === 'POST');
    expect(mutationCall).toBeDefined();
    expect(mutationCall?.headers?.['X-XSRF-TOKEN']).toBe('materialized-after-session-bootstrap');
  });

  it('encerra sessão, remove apenas a chave reservada e permite trocar de conta', async () => {
    document.cookie = 'XSRF-TOKEN=csrf; Path=/';
    sessionStorage.setItem('edt.purchase-intent.v1', '{"eventId":"future"}');
    sessionStorage.setItem('unrelated.preference', 'keep');
    localStorage.setItem('unrelated.local-preference', 'keep');
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000004',
            email: 'gate@demo.elitedevticket.local',
            role: 'GATE',
          },
        }));
      }
      if (url.includes('/api/v1/auth/logout')) {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(jsonResponse({ events: [] }));
      }
      return Promise.resolve(new Response(null, { status: 200 }));
    });
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
    let logoutAttemptCount = 0;
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000004',
            email: 'gate@demo.elitedevticket.local',
            role: 'GATE',
          },
        }));
      }
      if (url.includes('/api/v1/auth/logout')) {
        logoutAttemptCount++;
        if (logoutAttemptCount === 1) {
          return new Promise((resolve) => { completeLogout = resolve; });
        }
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(jsonResponse({ events: [] }));
      }
      return Promise.resolve(new Response(null, { status: 200 }));
    });
    const user = userEvent.setup();
    render(<App />);

    void user.click(await screen.findByRole('button', { name: 'Sair e trocar de conta' }));
    const busyButton = await screen.findByRole('button', { name: 'Saindo…' });
    expect(busyButton.closest('section')?.getAttribute('aria-busy')).toBe('true');
    completeLogout(jsonResponse({ code: 'AUTH_UNAVAILABLE' }, 503));

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Não foi possível encerrar a sessão. Tente novamente.',
    );
    expect(screen.getAllByText('gate@demo.elitedevticket.local').length).toBeGreaterThan(0);
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
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000004',
            email: 'gate@demo.elitedevticket.local',
            role: 'GATE',
          },
        }));
      }
      if (url.includes('/api/v1/auth/logout')) {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(jsonResponse({ events: [] }));
      }
      return Promise.resolve(new Response(null, { status: 200 }));
    });
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
      // 5. Load sectors on entering editor
      .mockResolvedValueOnce(jsonResponse({ sectors: [] }))
      // 6. Update draft
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
      // 7. Reload my events on returning
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

  it('visitante anônimo acessa o catálogo público por padrão e busca eventos publicados', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      // 1. Session check: unauthenticated
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      // 2. Initial public catalog load
      .mockResolvedValueOnce(jsonResponse({
        events: [
          {
            id: 'ev-pub-1',
            title: 'Festival Primavera Sound 2026',
            description: 'Festival incrível',
            imageUrl: 'https://images.example.com/primavera.jpg',
            category: 'Festival',
            status: 'PUBLISHED',
            venueName: 'Autódromo de Interlagos',
            venueAddress: 'São Paulo, SP',
            startsAt: '2026-11-20T20:00:00Z',
            startingPrice: 150.0,
            salesClosed: false,
            createdAt: '2026-08-15T10:00:00Z',
            updatedAt: '2026-08-15T12:00:00Z',
          },
        ],
      }))
      // 3. Search public events
      .mockResolvedValueOnce(jsonResponse({
        events: [
          {
            id: 'ev-pub-1',
            title: 'Festival Primavera Sound 2026',
            category: 'Festival',
            status: 'PUBLISHED',
            startingPrice: 150.0,
            salesClosed: false,
            createdAt: '2026-08-15T10:00:00Z',
            updatedAt: '2026-08-15T12:00:00Z',
          },
        ],
      }));

    const user = userEvent.setup();
    render(<App />);

    // Renders public catalog by default
    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
    expect(await screen.findByText('Festival Primavera Sound 2026')).toBeDefined();
    expect(screen.getByText(/A partir de/)).toBeDefined();

    // Type in search
    const searchInput = screen.getByPlaceholderText('Buscar eventos por título...');
    await user.type(searchInput, 'Primavera');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText(/1 evento encontrado para “Primavera”/)).toBeDefined();
  });

  it('visitante anônimo navega do catálogo para a tela de login pelo menu de navegação', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      // 1. Session check: unauthenticated
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      // 2. Initial catalog load
      .mockResolvedValueOnce(jsonResponse({ events: [] }));

    const user = userEvent.setup();
    render(<App />);

    // Starts on catalog
    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();

    // Click "Acessar conta" in top navigation
    const loginNavBtn = screen.getByRole('button', { name: 'Acessar conta' });
    await user.click(loginNavBtn);

    // Shows login form
    expect(await screen.findByRole('heading', { level: 2, name: 'Entrar com conta provisionada' })).toBeDefined();
    expect(screen.getByLabelText('E-mail')).toBeDefined();
    expect(screen.getByLabelText('Senha')).toBeDefined();

    // Switch back to catalog
    const catalogNavBtn = screen.getByRole('button', { name: 'Catálogo de Eventos' });
    await user.click(catalogNavBtn);

    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
  });

  it('visitante anônimo navega do catálogo para detalhe do evento (S02), seleciona setor/quantidade e é encaminhado ao login com intenção preservada', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      // 1. Session check: unauthenticated
      .mockResolvedValueOnce(jsonResponse({ authenticated: false }))
      // 2. Initial catalog load
      .mockResolvedValueOnce(
        jsonResponse({
          events: [
            {
              id: 'ev-flow-1',
              title: 'Lollapalooza Brasil 2026',
              description: 'Mega festival em São Paulo',
              imageUrl: 'https://images.example.com/lolla.jpg',
              category: 'Festival',
              status: 'PUBLISHED',
              venueName: 'Autódromo de Interlagos',
              venueAddress: 'São Paulo, SP',
              startsAt: '2026-11-20T20:00:00Z',
              startingPrice: 200.0,
              salesClosed: false,
              createdAt: '2026-08-15T10:00:00Z',
              updatedAt: '2026-08-15T12:00:00Z',
            },
          ],
        }),
      )
      // 3. List sectors for event
      .mockResolvedValueOnce(
        jsonResponse({
          sectors: [
            {
              id: 'sec-lolla-pista',
              eventId: 'ev-flow-1',
              name: 'Pista Geral',
              description: 'Acesso aos 4 palcos',
              capacity: 1000,
              availableQuantity: 1000,
              price: 200.0,
              createdAt: '2026-08-15T10:00:00Z',
              updatedAt: '2026-08-15T12:00:00Z',
            },
          ],
        }),
      );

    const user = userEvent.setup();
    render(<App />);

    // 1. Catálogo público é exibido
    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
    expect(await screen.findByText('Lollapalooza Brasil 2026')).toBeDefined();

    // 2. Clica no botão "Ver detalhes" do card
    const viewDetailBtn = screen.getByRole('button', { name: /Ver detalhes de Lollapalooza Brasil 2026/ });
    await user.click(viewDetailBtn);

    // 3. Superfície S02 (Detalhe) é exibida
    expect(await screen.findByRole('heading', { level: 1, name: 'Lollapalooza Brasil 2026' })).toBeDefined();
    expect(screen.getAllByText('Pista Geral').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/200,00/).length).toBeGreaterThanOrEqual(1);

    // 4. Ajusta a quantidade para 2
    const incBtn = screen.getByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('2');
    expect(screen.getAllByText(/400,00/).length).toBeGreaterThanOrEqual(1);

    // 5. Clica em "Reservar Ingressos"
    const reserveBtn = screen.getByRole('button', { name: /Reservar 2 ingressos no setor Pista Geral/ });
    await user.click(reserveBtn);

    // 6. Intenção de compra é persistida em sessionStorage
    const storedIntentionRaw = sessionStorage.getItem('edt_purchase_intention');
    expect(storedIntentionRaw).not.toBeNull();
    const storedIntention = JSON.parse(storedIntentionRaw!);
    expect(storedIntention.eventId).toBe('ev-flow-1');
    expect(storedIntention.ticketSectorId).toBe('sec-lolla-pista');
    expect(storedIntention.quantity).toBe(2);

    // 7. Usuário é encaminhado para a tela de login com banner informativo contextual
    expect(await screen.findByRole('heading', { level: 2, name: 'Entrar com conta provisionada' })).toBeDefined();
    expect(screen.getByTestId('login-notice-banner').textContent).toContain(
      'Para continuar com a compra dos seus ingressos, acesse sua conta de Cliente (CUSTOMER)',
    );
  });

  it('cliente autenticado (CUSTOMER) navega para o detalhe e cria hold de reserva com sucesso', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000002',
            email: 'customer.one@demo.elitedevticket.local',
            role: 'CUSTOMER',
          },
        }));
      }
      if (url.includes('/reservations') && method === 'POST') {
        return Promise.resolve(jsonResponse({
          id: 'res-cust-1',
          customerId: '00000000-0000-0000-0000-000000000002',
          eventId: 'ev-cust-1',
          sectorId: 'sec-mesa',
          quantity: 1,
          unitPrice: 90.0,
          totalAmount: 90.0,
          status: 'HOLDING',
          expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
          createdAt: new Date().toISOString(),
          serverNow: new Date().toISOString(),
        }, 201));
      }
      if (url.endsWith('/sectors') && method === 'GET') {
        return Promise.resolve(jsonResponse({
          sectors: [
            {
              id: 'sec-mesa',
              eventId: 'ev-cust-1',
              name: 'Mesa Central',
              capacity: 40,
              availableQuantity: 40,
              price: 90.0,
              createdAt: '2026-08-15T10:00:00Z',
              updatedAt: '2026-08-15T12:00:00Z',
            },
          ],
        }));
      }
      if (url.endsWith('/events/ev-cust-1') && method === 'GET') {
        return Promise.resolve(jsonResponse({
          id: 'ev-cust-1',
          organizerId: '00000000-0000-0000-0000-000000000001',
          title: 'Jazz & Blues Night 2026',
          category: 'Jazz',
          status: 'PUBLISHED',
          venueName: 'Blue Note SP',
          startsAt: '2026-12-05T21:00:00Z',
          createdAt: '2026-08-15T10:00:00Z',
          updatedAt: '2026-08-15T12:00:00Z',
        }));
      }
      if (url.includes('/api/v1/events') && method === 'GET') {
        return Promise.resolve(jsonResponse({
          events: [
            {
              id: 'ev-cust-1',
              title: 'Jazz & Blues Night 2026',
              category: 'Jazz',
              status: 'PUBLISHED',
              venueName: 'Blue Note SP',
              startsAt: '2026-12-05T21:00:00Z',
              startingPrice: 90.0,
              salesClosed: false,
              createdAt: '2026-08-15T10:00:00Z',
              updatedAt: '2026-08-15T12:00:00Z',
            },
          ],
        }));
      }
      return Promise.resolve(jsonResponse({}));
    });

    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByText('customer.one@demo.elitedevticket.local')).toBeDefined();
    expect(screen.getByText('Cliente')).toBeDefined();

    // Clica em "Ver detalhes"
    const viewDetailBtn = await screen.findByRole('button', { name: /Ver detalhes de Jazz & Blues Night 2026/ });
    await user.click(viewDetailBtn);

    // Detalhe S02 é exibido
    expect(await screen.findByRole('heading', { level: 1, name: 'Jazz & Blues Night 2026' })).toBeDefined();
    expect(screen.getAllByText('Mesa Central').length).toBeGreaterThanOrEqual(1);

    // Clica em "Reservar Ingressos"
    const reserveBtn = screen.getByRole('button', { name: /Reservar 1 ingresso no setor Mesa Central/ });
    await user.click(reserveBtn);

    // Active hold card é exibido com sucesso
    expect(await screen.findByTestId('active-hold-card')).toBeDefined();
    expect(screen.getByText('Ingressos Pré-Reservados (Hold)')).toBeDefined();
    expect(screen.getAllByText(/90,00/).length).toBeGreaterThanOrEqual(1);

    // Botão de voltar ao catálogo funciona
    const backBtn = screen.getByRole('button', { name: 'Voltar para o catálogo de eventos' });
    await user.click(backBtn);

    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
  });

  it('restaura intenção de compra após login CUSTOMER e cria hold válido', async () => {
    // 1. Visitante salva intenção no sessionStorage
    sessionStorage.setItem(
      'edt_purchase_intention',
      JSON.stringify({
        eventId: 'ev-cust-restore',
        ticketSectorId: 'sec-pista-restore',
        quantity: 2,
        internalReturnPath: '/events/ev-cust-restore',
        createdAt: new Date().toISOString(),
      }),
    );

    const mockEvent = {
      id: 'ev-cust-restore',
      organizerId: '00000000-0000-0000-0000-000000000001',
      title: 'Show dos Sonhos 2026',
      status: 'PUBLISHED',
      startsAt: '2026-11-20T20:00:00Z',
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };

    const mockSectors = {
      sectors: [
        {
          id: 'sec-pista-restore',
          eventId: 'ev-cust-restore',
          name: 'Pista Geral',
          capacity: 100,
          availableQuantity: 50,
          price: 100.0,
          createdAt: '2026-08-15T10:00:00Z',
          updatedAt: '2026-08-15T12:00:00Z',
        },
      ],
    };

    const mockReservation = {
      id: 'res-restored-1',
      customerId: '00000000-0000-0000-0000-000000000002',
      eventId: 'ev-cust-restore',
      sectorId: 'sec-pista-restore',
      quantity: 2,
      unitPrice: 100.0,
      totalAmount: 200.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({ authenticated: false }));
      }
      if (url.includes('/api/v1/auth/login')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000002',
            email: 'customer.one@demo.elitedevticket.local',
            role: 'CUSTOMER',
          },
        }));
      }
      if (url.includes('/reservations') && method === 'POST') {
        return Promise.resolve(jsonResponse(mockReservation, 201));
      }
      if (url.includes('/sectors') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockSectors));
      }
      if (url.endsWith('/events/ev-cust-restore') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockEvent));
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);

    // Realiza login
    const emailInput = await screen.findByLabelText('E-mail');
    const passwordInput = screen.getByLabelText('Senha');
    await user.type(emailInput, 'customer.one@demo.elitedevticket.local');
    await user.type(passwordInput, 'password');

    const submitBtn = screen.getByRole('button', { name: 'Entrar' });
    await user.click(submitBtn);

    // Intenção restaurada com sucesso e hold ativo exibido
    expect(await screen.findByTestId('active-hold-card')).toBeDefined();
    expect(screen.getByText('Ingressos Pré-Reservados (Hold)')).toBeDefined();
    expect(screen.getAllByText(/200,00/).length).toBeGreaterThanOrEqual(1);

    // Intenção foi limpa do sessionStorage
    expect(sessionStorage.getItem('edt_purchase_intention')).toBeNull();
  });

  it('restaura e trata falha de disponibilidade após login sem redução silenciosa', async () => {
    sessionStorage.setItem(
      'edt_purchase_intention',
      JSON.stringify({
        eventId: 'ev-fail-1',
        ticketSectorId: 'sec-soldout',
        quantity: 3,
        internalReturnPath: '/events/ev-fail-1',
        createdAt: new Date().toISOString(),
      }),
    );

    const mockEvent = {
      id: 'ev-fail-1',
      organizerId: '00000000-0000-0000-0000-000000000001',
      title: 'Show Esgotado 2026',
      status: 'PUBLISHED',
      startsAt: '2026-11-20T20:00:00Z',
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };

    const mockSectors = {
      sectors: [
        {
          id: 'sec-soldout',
          eventId: 'ev-fail-1',
          name: 'Área Premium',
          capacity: 50,
          availableQuantity: 0,
          price: 150.0,
          createdAt: '2026-08-15T10:00:00Z',
          updatedAt: '2026-08-15T12:00:00Z',
        },
      ],
    };

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({ authenticated: false }));
      }
      if (url.includes('/api/v1/auth/login')) {
        return Promise.resolve(jsonResponse({
          authenticated: true,
          user: {
            id: '00000000-0000-0000-0000-000000000002',
            email: 'customer.one@demo.elitedevticket.local',
            role: 'CUSTOMER',
          },
        }));
      }
      if (url.includes('/reservations') && method === 'POST') {
        return Promise.resolve(jsonResponse({
          code: 'INSUFFICIENT_AVAILABILITY',
          message: 'Quantidade solicitada indisponível.',
          traceId: 'tr-fail',
          timestamp: new Date().toISOString(),
        }, 422));
      }
      if (url.includes('/sectors') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockSectors));
      }
      if (url.endsWith('/events/ev-fail-1') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockEvent));
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    const user = userEvent.setup();
    render(<App initialAnonymousView="login" />);

    const emailInput = await screen.findByLabelText('E-mail');
    const passwordInput = screen.getByLabelText('Senha');
    await user.type(emailInput, 'customer.one@demo.elitedevticket.local');
    await user.type(passwordInput, 'password');

    const submitBtn = screen.getByRole('button', { name: 'Entrar' });
    await user.click(submitBtn);

    // Alerta de erro de restauração é exibido no detalhe do evento
    expect(await screen.findByTestId('reservation-error-alert')).toBeDefined();
    expect(screen.getByTestId('reservation-error-alert').textContent).toContain(
      'Não foi possível concluir sua reserva automaticamente: a quantidade solicitada não está mais disponível no setor selecionado.',
    );

    // Intenção foi limpa do sessionStorage
    expect(sessionStorage.getItem('edt_purchase_intention')).toBeNull();
  });

  it('visitante anônimo acessa ingresso compartilhado diretamente pelo link /t/:shareToken', async () => {
    const mockSharedTicket = {
      id: '11111111-1111-1111-1111-111111111111',
      eventId: '22222222-2222-2222-2222-222222222222',
      sectorId: '33333333-3333-3333-3333-333333333333',
      ordinal: 1,
      status: 'VALID',
      manualCode: 'AB7K92QX4M',
      shareToken: 'share-token-test-12345678901234567890123456789012',
      validationToken: 'val-token-test-12345678901234567890123456789012',
      createdAt: '2026-08-16T14:00:00Z',
    };

    const mockEvent = {
      id: '22222222-2222-2222-2222-222222222222',
      organizerId: '00000000-0000-0000-0000-000000000001',
      title: 'Festival Compartilhado',
      category: 'Música',
      description: 'Festival de música ao vivo',
      venueName: 'Estádio Municipal',
      venueAddress: 'Rua Principal, 50',
      startsAt: '2026-10-15T19:00:00Z',
      status: 'PUBLISHED',
      createdAt: '2026-08-16T12:00:00Z',
      updatedAt: '2026-08-16T12:00:00Z',
    };

    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(jsonResponse({ authenticated: false }));
      }
      if (url.includes('/api/v1/public/tickets/')) {
        return Promise.resolve(jsonResponse(mockSharedTicket));
      }
      if (url.includes('/sectors')) {
        return Promise.resolve(jsonResponse({
          sectors: [{
            id: '33333333-3333-3333-3333-333333333333',
            eventId: '22222222-2222-2222-2222-222222222222',
            name: 'Pista Geral',
            description: 'Acesso geral',
            capacity: 200,
            availableQuantity: 150,
            price: 100,
            createdAt: '2026-08-16T12:00:00Z',
            updatedAt: '2026-08-16T12:00:00Z',
          }],
        }));
      }
      if (url.includes('/api/v1/events/22222222-2222-2222-2222-222222222222')) {
        return Promise.resolve(jsonResponse(mockEvent));
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    render(
      <App
        initialAnonymousView="shared-ticket"
        initialShareToken="share-token-test-12345678901234567890123456789012"
      />
    );

    // Verifica que renderiza a página do ingresso compartilhado sem exigir login
    expect(await screen.findByRole('heading', { level: 2, name: 'Festival Compartilhado' })).toBeDefined();
    expect(screen.getByTestId('shared-ticket-status-badge').textContent).toBe('Válido');
    expect(screen.getByText('Ingresso #1')).toBeDefined();
    expect(screen.getByText('AB7K-92QX-4M')).toBeDefined();
    expect(screen.getByTestId('qrcode-panel')).toBeDefined();

    // Permite voltar ao catálogo
    const backBtn = screen.getByTestId('shared-ticket-back-to-catalog-btn');
    const user = userEvent.setup();
    await user.click(backBtn);

    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
  });

  it('usuário GATE autenticado acessa a área da Portaria, sem acesso a Meus Eventos ou Meus Ingressos', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(
          jsonResponse({
            authenticated: true,
            user: {
              id: '00000000-0000-0000-0000-000000000004',
              email: 'gate@demo.elitedevticket.local',
              role: 'GATE',
            },
          })
        );
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(
          jsonResponse({
            events: [
              {
                id: '11111111-1111-1111-1111-111111111111',
                title: 'Show da Portaria 2026',
                status: 'PUBLISHED',
                venueName: 'Espaço Unimed',
                startsAt: '2026-11-20T21:00:00Z',
                startingPrice: 120.0,
                salesClosed: false,
                createdAt: '2026-08-01T10:00:00Z',
                updatedAt: '2026-08-01T10:00:00Z',
              },
            ],
          })
        );
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    render(<App />);

    await screen.findByRole('heading', { level: 2, name: 'Sessão atual' });
    expect(screen.getByText('Portaria').textContent).toBe('Portaria');
    expect(screen.getByTestId('gate-view-root')).toBeDefined();
    expect(screen.getByText(/Controle de Portaria/i)).toBeDefined();

    // Verify Organizer / Customer surfaces are NOT rendered
    expect(screen.queryByRole('heading', { level: 2, name: 'Meus Eventos' })).toBeNull();
    expect(screen.queryByTestId('customer-nav-catalog-btn')).toBeNull();
    expect(screen.queryByTestId('customer-nav-my-tickets-btn')).toBeNull();
  });

  it('usuário GATE seleciona evento publicado e pode trocar a seleção', async () => {
    const user = userEvent.setup();
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(
          jsonResponse({
            authenticated: true,
            user: {
              id: '00000000-0000-0000-0000-000000000004',
              email: 'gate@demo.elitedevticket.local',
              role: 'GATE',
            },
          })
        );
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(
          jsonResponse({
            events: [
              {
                id: '11111111-1111-1111-1111-111111111111',
                title: 'Show da Portaria 2026',
                status: 'PUBLISHED',
                venueName: 'Espaço Unimed',
                startsAt: '2026-11-20T21:00:00Z',
                startingPrice: 120.0,
                salesClosed: false,
                createdAt: '2026-08-01T10:00:00Z',
                updatedAt: '2026-08-01T10:00:00Z',
              },
            ],
          })
        );
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    render(<App />);

    expect(await screen.findByText('Show da Portaria 2026')).toBeDefined();

    const selectBtn = screen.getByTestId('gate-select-event-btn-11111111-1111-1111-1111-111111111111');
    await user.click(selectBtn);

    // Selected event banner appears prominently
    expect(screen.getByTestId('gate-selected-event-banner')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Show da Portaria 2026' })).toBeDefined();
    expect(screen.getByTestId('gate-operational-ready')).toBeDefined();

    // Click "Trocar evento"
    const changeBtn = screen.getByTestId('gate-change-event-btn');
    await user.click(changeBtn);

    // Returns to selector
    expect(screen.queryByTestId('gate-selected-event-banner')).toBeNull();
    expect(await screen.findByText('Show da Portaria 2026')).toBeDefined();
  });

  it('usuário GATE visualiza estado vazio quando não há eventos publicados', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = String(input);
      if (url.includes('/api/v1/auth/session')) {
        return Promise.resolve(
          jsonResponse({
            authenticated: true,
            user: {
              id: '00000000-0000-0000-0000-000000000004',
              email: 'gate@demo.elitedevticket.local',
              role: 'GATE',
            },
          })
        );
      }
      if (url.includes('/api/v1/events')) {
        return Promise.resolve(
          jsonResponse({
            events: [],
          })
        );
      }
      return Promise.resolve(jsonResponse({ events: [] }));
    });

    render(<App />);

    expect(
      await screen.findByText('Nenhum evento publicado disponível para controle de portaria no momento.')
    ).toBeDefined();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
