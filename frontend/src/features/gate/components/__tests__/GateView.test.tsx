import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GateView } from '../GateView';
import * as gateApi from '../../api/gateApi';
import type { GateEvent } from '../../api/gateApi';
import type { SessionUser } from '../../../../app/api/authApi';

describe('GateView component (Área Operacional da Portaria)', () => {
  const gateUser: SessionUser = {
    id: 'gate-user-1',
    email: 'gate@demo.elitedevticket.local',
    role: 'GATE',
  };

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

  it('renderiza cabeçalho operacional com badge de Portaria e e-mail do operador', async () => {
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mockPublishedEvents);

    render(<GateView user={gateUser} />);

    expect(screen.getByText(/Controle de Portaria/i)).toBeDefined();
    expect(screen.getByText('gate@demo.elitedevticket.local')).toBeDefined();
    expect(screen.getByText('Validação de Ingressos')).toBeDefined();
  });

  it('permite selecionar um evento publicado e exibe o banner explícito com contexto ativo', async () => {
    const user = userEvent.setup();
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mockPublishedEvents);
    const onEventChange = vi.fn();

    render(<GateView user={gateUser} onEventChange={onEventChange} />);

    await waitFor(() => {
      expect(screen.getByText('Festival Rock Paulista 2026')).toBeDefined();
    });

    const selectBtn = screen.getByTestId('gate-select-event-btn-event-uuid-1');
    await user.click(selectBtn);

    // Selected event banner appears prominently
    expect(screen.getByTestId('gate-selected-event-banner')).toBeDefined();
    expect(screen.getByText('Evento em Operação')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Festival Rock Paulista 2026' })).toBeDefined();
    expect(screen.getByText(/Allianz Parque/i)).toBeDefined();

    // Operational ready placeholder
    expect(screen.getByTestId('gate-operational-ready')).toBeDefined();
    expect(screen.getByText(/Contexto operacional ativo \(event-uuid-1\)/i)).toBeDefined();

    expect(onEventChange).toHaveBeenCalledWith(mockPublishedEvents[0]);
  });

  it('permite trocar o evento selecionado antes de iniciar validação', async () => {
    const user = userEvent.setup();
    vi.spyOn(gateApi, 'listGateEvents').mockResolvedValue(mockPublishedEvents);
    const onEventChange = vi.fn();

    render(
      <GateView
        user={gateUser}
        initialSelectedEvent={mockPublishedEvents[0]}
        onEventChange={onEventChange}
      />
    );

    // Starts with event 1 selected
    expect(screen.getByTestId('gate-selected-event-banner')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Festival Rock Paulista 2026' })).toBeDefined();

    // Click "Trocar evento"
    const changeBtn = screen.getByTestId('gate-change-event-btn');
    await user.click(changeBtn);

    // Returns to selection list
    expect(screen.queryByTestId('gate-selected-event-banner')).toBeNull();
    await waitFor(() => {
      expect(screen.getByText('Stand-up Comedy Night')).toBeDefined();
    });

    // Select event 2
    const selectSecondBtn = screen.getByTestId('gate-select-event-btn-event-uuid-2');
    await user.click(selectSecondBtn);

    expect(screen.getByTestId('gate-selected-event-banner')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Stand-up Comedy Night' })).toBeDefined();
    expect(onEventChange).toHaveBeenCalledWith(mockPublishedEvents[1]);
  });
});
