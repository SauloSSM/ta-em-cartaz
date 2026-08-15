import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { MyEventsList } from '../MyEventsList';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

describe('MyEventsList (Superfície S09)', () => {
  const mockEvents = [
    {
      id: '00000000-0000-0000-0000-000000000010',
      organizerId: '00000000-0000-0000-0000-000000000001',
      title: 'Festival de Inverno',
      category: 'Música',
      venueName: 'Campos do Jordão',
      status: 'DRAFT' as const,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
    {
      id: '00000000-0000-0000-0000-000000000020',
      organizerId: '00000000-0000-0000-0000-000000000001',
      title: 'Show de Rock',
      category: 'Rock',
      venueName: 'Allianz Parque',
      status: 'PUBLISHED' as const,
      createdAt: '2026-08-15T11:00:00Z',
      updatedAt: '2026-08-15T11:00:00Z',
    },
  ];

  it('loads and renders list of events with DRAFT and PUBLISHED status', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ events: mockEvents }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    render(<MyEventsList onNewEvent={vi.fn()} onSelectEvent={vi.fn()} />);

    expect(screen.getByRole('heading', { level: 2, name: 'Meus Eventos' })).toBeDefined();

    expect(await screen.findByRole('heading', { level: 3, name: 'Festival de Inverno' })).toBeDefined();
    expect(screen.getByLabelText('Status: DRAFT')).toBeDefined();
    expect(screen.getByRole('button', { name: 'Editar rascunho de Festival de Inverno' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Excluir rascunho de Festival de Inverno' })).toBeDefined();

    expect(screen.getByRole('heading', { level: 3, name: 'Show de Rock' })).toBeDefined();
    expect(screen.getByLabelText('Status: PUBLISHED')).toBeDefined();
    expect(screen.getByRole('button', { name: 'Ver detalhes de Show de Rock' })).toBeDefined();
    expect(screen.queryByRole('button', { name: 'Excluir rascunho de Show de Rock' })).toBeNull();
  });

  it('renders empty state with CTA when no events exist', async () => {
    const handleNewEvent = vi.fn();
    const user = userEvent.setup();

    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ events: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    render(<MyEventsList onNewEvent={handleNewEvent} onSelectEvent={vi.fn()} />);

    expect(await screen.findByText('Você ainda não possui eventos cadastrados.')).toBeDefined();

    const ctaBtn = screen.getByRole('button', { name: 'Buscar no catálogo Ticketmaster' });
    await user.click(ctaBtn);

    expect(handleNewEvent).toHaveBeenCalledTimes(1);
  });

  it('renders error state with retry button on fetch failure', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: 'AUTH_FORBIDDEN',
            message: 'Erro ao carregar eventos.',
            traceId: 'tr-err',
            timestamp: '2026-08-15T12:00:00Z',
          }),
          { status: 403, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ events: mockEvents }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    globalThis.fetch = fetchMock;

    render(<MyEventsList onNewEvent={vi.fn()} onSelectEvent={vi.fn()} />);

    expect(await screen.findByRole('alert')).toBeDefined();
    expect(screen.getByText('Erro ao carregar eventos.')).toBeDefined();

    const retryBtn = screen.getByRole('button', { name: 'Tentar novamente' });
    await user.click(retryBtn);

    expect(await screen.findByRole('heading', { level: 3, name: 'Festival de Inverno' })).toBeDefined();
  });

  it('deletes draft event from list after confirmation', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ events: [mockEvents[0]] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    globalThis.fetch = fetchMock;

    render(<MyEventsList onNewEvent={vi.fn()} onSelectEvent={vi.fn()} />);

    expect(await screen.findByRole('heading', { level: 3, name: 'Festival de Inverno' })).toBeDefined();

    await user.click(screen.getByRole('button', { name: 'Excluir rascunho de Festival de Inverno' }));

    expect(screen.getByRole('alertdialog')).toBeDefined();
    await user.click(screen.getByRole('button', { name: 'Sim, excluir rascunho' }));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/events/00000000-0000-0000-0000-000000000010',
      expect.objectContaining({ method: 'DELETE' }),
    );

    expect(await screen.findByText('Você ainda não possui eventos cadastrados.')).toBeDefined();
  });
});
