import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { DraftEventEditor } from '../DraftEventEditor';
import type { EventResponse } from '../../api/eventsApi';

const originalFetch = globalThis.fetch;

describe('DraftEventEditor', () => {
  const mockDraftEvent: EventResponse = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    organizerId: '00000000-0000-0000-0000-000000000001',
    externalSource: 'TICKETMASTER',
    externalId: 'tm-rock-2026',
    title: 'Rock in Rio 2026',
    description: 'Grande festival de música',
    imageUrl: 'https://images.example.com/banner.jpg',
    category: 'Rock',
    status: 'DRAFT',
    venueName: 'Cidade do Rock',
    venueAddress: 'Av. Salvador Allende, 6500, Rio de Janeiro - RJ',
    startsAt: '2026-09-20T18:00:00Z',
    createdAt: '2026-08-15T12:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  };

  const mockPublishedEvent: EventResponse = {
    ...mockDraftEvent,
    status: 'PUBLISHED',
  };

  beforeEach(() => {
    globalThis.fetch = vi.fn().mockImplementation((url: string | URL | Request) => {
      const urlString = typeof url === 'string' ? url : url.toString();
      if (urlString.includes('/sectors')) {
        return Promise.resolve(
          new Response(JSON.stringify({ sectors: [] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(mockDraftEvent), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it('renders draft event in editable form with fields populated and sector manager', () => {
    render(<DraftEventEditor event={mockDraftEvent} />);

    expect(screen.getByRole('heading', { name: 'Editor de Evento', level: 2 })).toBeDefined();
    expect(screen.getByLabelText('Status do evento: DRAFT')).toBeDefined();

    const titleInput = screen.getByLabelText(/Título do Evento/) as HTMLInputElement;
    expect(titleInput.value).toBe('Rock in Rio 2026');
    expect(titleInput.disabled).toBe(false);

    const descInput = screen.getByLabelText('Descrição do Evento') as HTMLTextAreaElement;
    expect(descInput.value).toBe('Grande festival de música');

    expect(screen.getByRole('heading', { name: 'Setores de Ingressos' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Salvar alterações' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Excluir rascunho de Rock in Rio 2026' })).toBeDefined();
  });

  it('edits fields and submits update successfully', async () => {
    const user = userEvent.setup();
    const handleUpdate = vi.fn();

    const fetchMock = vi.fn<typeof fetch>().mockImplementation((url: string | URL | Request, init?: RequestInit) => {
      const urlString = typeof url === 'string' ? url : url.toString();
      if (urlString.includes('/sectors')) {
        return Promise.resolve(
          new Response(JSON.stringify({ sectors: [] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }
      if (init?.method === 'PUT') {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              ...mockDraftEvent,
              title: 'Rock in Rio 2026 - Edição Especial',
              updatedAt: '2026-08-15T12:30:00Z',
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(mockDraftEvent), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });
    globalThis.fetch = fetchMock;

    render(<DraftEventEditor event={mockDraftEvent} onEventUpdated={handleUpdate} />);

    const titleInput = screen.getByLabelText(/Título do Evento/);
    await user.clear(titleInput);
    await user.type(titleInput, 'Rock in Rio 2026 - Edição Especial');

    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/events/123e4567-e89b-12d3-a456-426614174000',
      expect.objectContaining({
        method: 'PUT',
      }),
    );

    expect(await screen.findByText('Alterações salvas com sucesso!')).toBeDefined();
    expect(handleUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Rock in Rio 2026 - Edição Especial',
      }),
    );
  });

  it('opens confirmation dialog and deletes draft', async () => {
    const user = userEvent.setup();
    const handleDelete = vi.fn();

    const fetchMock = vi.fn<typeof fetch>().mockImplementation((url: string | URL | Request, init?: RequestInit) => {
      const urlString = typeof url === 'string' ? url : url.toString();
      if (urlString.includes('/sectors')) {
        return Promise.resolve(
          new Response(JSON.stringify({ sectors: [] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }
      if (init?.method === 'DELETE') {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      return Promise.resolve(
        new Response(JSON.stringify(mockDraftEvent), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });
    globalThis.fetch = fetchMock;

    render(<DraftEventEditor event={mockDraftEvent} onEventDeleted={handleDelete} />);

    await user.click(screen.getByRole('button', { name: 'Excluir rascunho de Rock in Rio 2026' }));

    expect(screen.getByRole('alertdialog')).toBeDefined();
    await user.click(screen.getByRole('button', { name: 'Sim, excluir rascunho' }));

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/events/123e4567-e89b-12d3-a456-426614174000',
      expect.objectContaining({
        method: 'DELETE',
      }),
    );

    expect(handleDelete).toHaveBeenCalledWith('123e4567-e89b-12d3-a456-426614174000');
  });

  it('renders published event with locked structural fields, editable non-structural fields and save button', () => {
    render(<DraftEventEditor event={mockPublishedEvent} />);

    expect(screen.getByLabelText('Status do evento: PUBLISHED')).toBeDefined();

    // Structural fields are disabled
    const titleInput = screen.getByLabelText(/Título do Evento/) as HTMLInputElement;
    expect(titleInput.disabled).toBe(true);

    const venueNameInput = screen.getByLabelText('Nome do Local / Venue') as HTMLInputElement;
    expect(venueNameInput.disabled).toBe(true);

    const venueAddressInput = screen.getByLabelText('Endereço do Local') as HTMLInputElement;
    expect(venueAddressInput.disabled).toBe(true);

    const startsAtInput = screen.getByLabelText('Data e Hora de Início (ISO)') as HTMLInputElement;
    expect(startsAtInput.disabled).toBe(true);

    // Structural lock explanations are visible
    const lockNotes = screen.getAllByText('Campo estrutural protegido: não pode ser alterado após a publicação.');
    expect(lockNotes.length).toBe(4);

    // Non-structural fields are editable
    const descInput = screen.getByLabelText('Descrição do Evento') as HTMLTextAreaElement;
    expect(descInput.disabled).toBe(false);

    const categoryInput = screen.getByLabelText('Categoria') as HTMLInputElement;
    expect(categoryInput.disabled).toBe(false);

    const imageInput = screen.getByLabelText('URL do Banner / Imagem') as HTMLInputElement;
    expect(imageInput.disabled).toBe(false);

    // Save button is available
    expect(screen.getByRole('button', { name: 'Salvar alterações' })).toBeDefined();

    // Delete button is not present
    expect(screen.queryByRole('button', { name: /Excluir rascunho/ })).toBeNull();
    expect(
      screen.getByText('Eventos publicados possuem dados estruturais protegidos e não podem ser excluídos.'),
    ).toBeDefined();
  });

  it('edits non-structural fields of published event and submits update successfully', async () => {
    const user = userEvent.setup();
    const handleUpdate = vi.fn();

    const fetchMock = vi.fn<typeof fetch>().mockImplementation((url: string | URL | Request, init?: RequestInit) => {
      const urlString = typeof url === 'string' ? url : url.toString();
      if (urlString.includes('/sectors')) {
        return Promise.resolve(
          new Response(JSON.stringify({ sectors: [] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }
      if (init?.method === 'PUT') {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              ...mockPublishedEvent,
              description: 'Nova descrição enriquecida',
              category: 'Rock Festival',
              updatedAt: '2026-08-15T15:00:00Z',
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(mockPublishedEvent), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });
    globalThis.fetch = fetchMock;

    render(<DraftEventEditor event={mockPublishedEvent} onEventUpdated={handleUpdate} />);

    const descInput = screen.getByLabelText('Descrição do Evento');
    await user.clear(descInput);
    await user.type(descInput, 'Nova descrição enriquecida');

    const categoryInput = screen.getByLabelText('Categoria');
    await user.clear(categoryInput);
    await user.type(categoryInput, 'Rock Festival');

    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/v1/events/${mockPublishedEvent.id}`,
      expect.objectContaining({
        method: 'PUT',
      }),
    );

    expect(await screen.findByText('Alterações salvas com sucesso!')).toBeDefined();
    expect(handleUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        description: 'Nova descrição enriquecida',
        category: 'Rock Festival',
      }),
    );
  });

  it('calls onBack when clicking back button', async () => {
    const handleBack = vi.fn();
    const user = userEvent.setup();

    render(<DraftEventEditor event={mockDraftEvent} onBack={handleBack} />);

    await user.click(screen.getByRole('button', { name: 'Voltar para a lista de eventos' }));

    expect(handleBack).toHaveBeenCalledTimes(1);
  });

  it('publishes draft event when ready and updates UI to published state', async () => {
    const user = userEvent.setup();
    const handleUpdate = vi.fn();

    const mockSectors = [
      {
        id: 'sec-1',
        eventId: mockDraftEvent.id,
        name: 'Pista',
        description: 'Pista geral',
        capacity: 500,
        availableQuantity: 500,
        price: 150.0,
        createdAt: '2026-08-15T12:00:00Z',
        updatedAt: '2026-08-15T12:00:00Z',
      },
    ];

    const fetchMock = vi.fn<typeof fetch>().mockImplementation((url: string | URL | Request, init?: RequestInit) => {
      const urlString = typeof url === 'string' ? url : url.toString();
      if (urlString.includes('/sectors')) {
        return Promise.resolve(
          new Response(JSON.stringify({ sectors: mockSectors }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }
      if (urlString.includes('/publish') && init?.method === 'POST') {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              ...mockDraftEvent,
              status: 'PUBLISHED',
              updatedAt: '2026-08-15T16:00:00Z',
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        );
      }
      return Promise.resolve(
        new Response(JSON.stringify(mockDraftEvent), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });
    globalThis.fetch = fetchMock;

    render(<DraftEventEditor event={mockDraftEvent} onEventUpdated={handleUpdate} />);

    // Wait for sectors to load and checklist to become ready
    expect(await screen.findByText(/Todas as condições obrigatórias foram atendidas/i)).toBeDefined();

    const publishButton = screen.getByRole('button', { name: 'Publicar Evento' }) as HTMLButtonElement;
    expect(publishButton.disabled).toBe(false);

    await user.click(publishButton);

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/v1/events/${mockDraftEvent.id}/publish`,
      expect.objectContaining({
        method: 'POST',
      }),
    );

    expect(await screen.findByText(/Evento Publicado!/i)).toBeDefined();
    expect(handleUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 'PUBLISHED',
      }),
    );
  });
});
