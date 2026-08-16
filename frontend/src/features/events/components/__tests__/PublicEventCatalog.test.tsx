import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PublicEventCatalog } from '../PublicEventCatalog';
import type { PublicEventResponse } from '../../api/eventsApi';

const mockEvents: PublicEventResponse[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    title: 'Rock in Rio 2026',
    description: 'Festival de rock',
    imageUrl: 'https://images.example.com/rock.jpg',
    category: 'Rock',
    status: 'PUBLISHED',
    venueName: 'Cidade do Rock',
    venueAddress: 'Barra da Tijuca, Rio de Janeiro',
    startsAt: '2026-09-18T18:00:00Z',
    startingPrice: 300,
    salesClosed: false,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    title: 'Jazz & Blues Festival',
    description: 'Festival intimista',
    category: 'Jazz',
    status: 'PUBLISHED',
    venueName: 'Teatro Castro Alves',
    venueAddress: 'Salvador, BA',
    startsAt: '2026-10-05T20:00:00Z',
    startingPrice: 80,
    salesClosed: true,
    createdAt: '2026-08-15T10:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
];

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('PublicEventCatalog (Superfície S01)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('displays loading state and then renders the list of published events', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({ events: mockEvents }),
    );

    render(<PublicEventCatalog />);

    expect(screen.getByRole('heading', { level: 1, name: 'Catálogo de Eventos' })).toBeDefined();
    expect(screen.getByRole('search', { name: 'Buscar eventos públicos' })).toBeDefined();

    // Results rendered
    expect(await screen.findByText('Rock in Rio 2026')).toBeDefined();
    expect(screen.getByText('Jazz & Blues Festival')).toBeDefined();
    expect(screen.getByText('Vendas encerradas')).toBeDefined();
    expect(screen.getByText(/R\$\s*300,00/)).toBeDefined();
    expect(screen.getByText(/R\$\s*80,00/)).toBeDefined();
  });

  it('filters events by title when search form is submitted', async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ events: mockEvents }))
      .mockResolvedValueOnce(jsonResponse({ events: [mockEvents[0]!] }));
    globalThis.fetch = fetchMock;

    const user = userEvent.setup();
    render(<PublicEventCatalog />);

    await screen.findByText('Rock in Rio 2026');

    const searchInput = screen.getByPlaceholderText('Buscar eventos por título...');
    await user.type(searchInput, 'Rock');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenLastCalledWith(
        '/api/v1/events?search=Rock',
        expect.objectContaining({ method: 'GET' }),
      );
    });

    expect(await screen.findByText(/1 evento encontrado para “Rock”/)).toBeDefined();
    expect(screen.getByText('Rock in Rio 2026')).toBeDefined();
    expect(screen.queryByText('Jazz & Blues Festival')).toBeNull();
  });

  it('clears search filter when clicking the clear button or clear filter link', async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ events: mockEvents }))
      .mockResolvedValueOnce(jsonResponse({ events: [mockEvents[0]!] }))
      .mockResolvedValueOnce(jsonResponse({ events: mockEvents }));
    globalThis.fetch = fetchMock;

    const user = userEvent.setup();
    render(<PublicEventCatalog />);

    await screen.findByText('Rock in Rio 2026');

    const searchInput = screen.getByPlaceholderText('Buscar eventos por título...');
    await user.type(searchInput, 'Rock');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    await screen.findByText(/1 evento encontrado para “Rock”/);

    const clearFilterBtn = screen.getByRole('button', { name: 'Limpar filtro' });
    await user.click(clearFilterBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenLastCalledWith(
        '/api/v1/events',
        expect.objectContaining({ method: 'GET' }),
      );
    });
    expect(await screen.findByText('Jazz & Blues Festival')).toBeDefined();
  });

  it('displays specific empty state when search returns no matching events', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ events: mockEvents }))
      .mockResolvedValueOnce(jsonResponse({ events: [] }));

    const user = userEvent.setup();
    render(<PublicEventCatalog />);

    await screen.findByText('Rock in Rio 2026');

    const searchInput = screen.getByPlaceholderText('Buscar eventos por título...');
    await user.type(searchInput, 'Carnaval');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByRole('heading', { level: 2, name: 'Nenhum evento encontrado' })).toBeDefined();
    expect(screen.getByText(/Não encontramos eventos com o termo/)).toBeDefined();
    expect(screen.getByText('“Carnaval”')).toBeDefined();
    expect(screen.getByRole('button', { name: 'Ver todos os eventos' })).toBeDefined();
  });

  it('displays empty state when catalog has no published events at all', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({ events: [] }),
    );

    render(<PublicEventCatalog />);

    expect(await screen.findByRole('heading', { level: 2, name: 'Nenhum evento publicado' })).toBeDefined();
    expect(screen.getByText('Não há eventos disponíveis para venda no momento. Volte em breve!')).toBeDefined();
  });

  it('displays accessible error state with retry button when fetch fails', async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockRejectedValueOnce(new TypeError('Network Error'))
      .mockResolvedValueOnce(jsonResponse({ events: mockEvents }));
    globalThis.fetch = fetchMock;

    const user = userEvent.setup();
    render(<PublicEventCatalog />);

    expect(await screen.findByRole('alert')).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'Erro ao carregar eventos' })).toBeDefined();
    expect(screen.getByText(/Não foi possível carregar o catálogo de eventos/)).toBeDefined();

    const retryBtn = screen.getByRole('button', { name: 'Tentar novamente' });
    await user.click(retryBtn);

    expect(await screen.findByText('Rock in Rio 2026')).toBeDefined();
  });
});
