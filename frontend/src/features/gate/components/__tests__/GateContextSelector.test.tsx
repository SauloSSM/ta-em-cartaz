import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GateContextSelector } from '../GateContextSelector';
import * as gateApi from '../../api/gateApi';
import type { GateEvent } from '../../api/gateApi';

describe('GateContextSelector component (Superfície Portaria)', () => {
  const mockPublishedEvents: GateEvent[] = [
    {
      id: 'event-uuid-1',
      title: 'Festival Rock Paulista 2026',
      status: 'PUBLISHED',
      category: 'SHOW',
      venueName: 'Allianz Parque',
      venueAddress: 'Av. Francisco Matarazzo, 1705',
      startsAt: '2026-10-20T20:00:00Z',
      startingPrice: 150.0,
      salesClosed: false,
      createdAt: '2026-08-01T10:00:00Z',
      updatedAt: '2026-08-01T10:00:00Z',
    },
    {
      id: 'event-uuid-2',
      title: 'Stand-up Comedy Night',
      status: 'PUBLISHED',
      category: 'TEATRO',
      venueName: 'Teatro Bradesco',
      venueAddress: 'Rua Palestra Itália, 500',
      startsAt: '2026-11-15T21:00:00Z',
      startingPrice: 80.0,
      salesClosed: false,
      createdAt: '2026-08-02T10:00:00Z',
      updatedAt: '2026-08-02T10:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('exibe estado de carregamento e depois lista apenas eventos publicados', async () => {
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mockPublishedEvents);
    const handleSelect = vi.fn();

    render(
      <GateContextSelector
        selectedEventId={null}
        onSelectEvent={handleSelect}
      />
    );

    expect(screen.getByRole('status').textContent).toContain(
      'Carregando eventos publicados disponíveis para a portaria…'
    );

    await waitFor(() => {
      expect(screen.getByText('Festival Rock Paulista 2026')).toBeDefined();
      expect(screen.getByText('Stand-up Comedy Night')).toBeDefined();
    });

    expect(screen.getByTestId('gate-event-item-event-uuid-1')).toBeDefined();
    expect(screen.getByTestId('gate-event-item-event-uuid-2')).toBeDefined();
  });

  it('filtra e nunca exibe eventos com status DRAFT se retornados pela API', async () => {
    const mixedEvents: GateEvent[] = [
      ...mockPublishedEvents,
      {
        id: 'event-draft-id',
        title: 'Evento em Rascunho Não Publicado',
        status: 'DRAFT' as const,
        startingPrice: 0,
        salesClosed: false,
        createdAt: '2026-08-01T10:00:00Z',
        updatedAt: '2026-08-01T10:00:00Z',
      },
    ];
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mixedEvents);

    render(
      <GateContextSelector
        selectedEventId={null}
        onSelectEvent={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Festival Rock Paulista 2026')).toBeDefined();
    });

    expect(screen.queryByText('Evento em Rascunho Não Publicado')).toBeNull();
  });

  it('exibe estado vazio claro quando não houver eventos publicados', async () => {
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue([]);

    render(
      <GateContextSelector
        selectedEventId={null}
        onSelectEvent={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(
        screen.getByText('Nenhum evento publicado disponível para controle de portaria no momento.')
      ).toBeDefined();
    });
  });

  it('exibe erro com retry acessível quando a chamada falhar', async () => {
    const listSpy = vi.spyOn(gateApi, 'listGateEvents')
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce(mockPublishedEvents);

    render(
      <GateContextSelector
        selectedEventId={null}
        onSelectEvent={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain(
        'Não foi possível carregar os eventos publicados'
      );
    });

    const retryBtn = screen.getByRole('button', { name: 'Tentar novamente' });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Festival Rock Paulista 2026')).toBeDefined();
    });

    expect(listSpy).toHaveBeenCalledTimes(2);
  });

  it('chama onSelectEvent ao clicar no botão de seleção', async () => {
    const user = userEvent.setup();
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mockPublishedEvents);
    const handleSelect = vi.fn();

    render(
      <GateContextSelector
        selectedEventId={null}
        onSelectEvent={handleSelect}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Festival Rock Paulista 2026')).toBeDefined();
    });

    const selectBtn = screen.getByTestId('gate-select-event-btn-event-uuid-1');
    await user.click(selectBtn);

    expect(handleSelect).toHaveBeenCalledWith(mockPublishedEvents[0]);
  });
});
