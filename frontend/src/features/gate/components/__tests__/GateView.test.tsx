import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GateView } from '../GateView';
import * as gateApi from '../../api/gateApi';
import type { GateEvent, ValidateTicketResponse } from '../../api/gateApi';
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

    // Operational ready container with manual validation form
    expect(screen.getByTestId('gate-operational-ready')).toBeDefined();
    expect(screen.getByTestId('gate-manual-section')).toBeDefined();
    expect(screen.getByTestId('gate-manual-code-input')).toBeDefined();

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

  describe('Validação Manual (Story 7.2)', () => {
    it('executa validação com código manual e exibe resultado VALID com opção de validar próximo', async () => {
      const user = userEvent.setup();
      const mockResponse: ValidateTicketResponse = {
        result: 'VALID',
        validationAttemptId: 'attempt-uuid-1',
        selectedEventId: 'event-uuid-1',
        ticketId: 'ticket-uuid-1',
        method: 'MANUAL',
        processedAt: '2026-08-16T21:30:00Z',
      };
      const validateSpy = vi.spyOn(gateApi, 'validateTicket').mockResolvedValue(mockResponse);

      render(
        <GateView
          user={gateUser}
          initialSelectedEvent={mockPublishedEvents[0]}
        />
      );

      const input = screen.getByTestId('gate-manual-code-input');
      const submitBtn = screen.getByTestId('gate-validate-btn');

      // Botão desabilitado enquanto input vazio
      expect(submitBtn.getAttribute('disabled')).toBeDefined();

      await user.type(input, 'ABCD-1234-EF');
      expect(submitBtn.hasAttribute('disabled')).toBe(false);

      await user.click(submitBtn);

      expect(validateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          selectedEventId: 'event-uuid-1',
          manualCode: 'ABCD-1234-EF',
          method: 'MANUAL',
        })
      );

      // Result card VALID
      const resultCard = await screen.findByTestId('gate-result-valid');
      expect(resultCard).toBeDefined();
      expect(screen.getByText('LIBERADO')).toBeDefined();
      expect(screen.getByText('Ingresso Válido')).toBeDefined();
      expect(screen.getByText(/Entrada autorizada/i)).toBeDefined();

      // Evento selecionado permanece explícito
      expect(screen.getByTestId('gate-selected-event-banner')).toBeDefined();

      // Clica em "Validar próximo ingresso"
      const nextBtn = screen.getByTestId('gate-next-validation-btn');
      await user.click(nextBtn);

      // Retorna ao formulário limpo
      expect(screen.queryByTestId('gate-result-valid')).toBeNull();
      expect(screen.getByTestId('gate-manual-code-input')).toBeDefined();
      expect((screen.getByTestId('gate-manual-code-input') as HTMLInputElement).value).toBe('');
    });

    it('exibe resultado INVALID quando o código não é reconhecido', async () => {
      const user = userEvent.setup();
      const mockResponse: ValidateTicketResponse = {
        result: 'INVALID',
        validationAttemptId: 'attempt-uuid-2',
        selectedEventId: 'event-uuid-1',
        method: 'MANUAL',
        processedAt: '2026-08-16T21:31:00Z',
      };
      vi.spyOn(gateApi, 'validateTicket').mockResolvedValue(mockResponse);

      render(
        <GateView
          user={gateUser}
          initialSelectedEvent={mockPublishedEvents[0]}
        />
      );

      await user.type(screen.getByTestId('gate-manual-code-input'), 'INVALID-CODE');
      await user.click(screen.getByTestId('gate-validate-btn'));

      const resultCard = await screen.findByTestId('gate-result-invalid');
      expect(resultCard).toBeDefined();
      expect(screen.getByText('RECUSADO')).toBeDefined();
      expect(screen.getByText('Ingresso Inválido')).toBeDefined();
      expect(screen.getByText(/Entrada não autorizada/i)).toBeDefined();
    });

    it('exibe resultado ALREADY_USED quando o ingresso já foi validado anteriormente', async () => {
      const user = userEvent.setup();
      const mockResponse: ValidateTicketResponse = {
        result: 'ALREADY_USED',
        validationAttemptId: 'attempt-uuid-3',
        selectedEventId: 'event-uuid-1',
        ticketId: 'ticket-uuid-3',
        method: 'MANUAL',
        processedAt: '2026-08-16T21:32:00Z',
      };
      vi.spyOn(gateApi, 'validateTicket').mockResolvedValue(mockResponse);

      render(
        <GateView
          user={gateUser}
          initialSelectedEvent={mockPublishedEvents[0]}
        />
      );

      await user.type(screen.getByTestId('gate-manual-code-input'), 'USED-1234');
      await user.click(screen.getByTestId('gate-validate-btn'));

      const resultCard = await screen.findByTestId('gate-result-already_used');
      expect(resultCard).toBeDefined();
      expect(screen.getByText('JÁ UTILIZADO')).toBeDefined();
      expect(screen.getByText('Ingresso Já Utilizado')).toBeDefined();
      expect(screen.getByText(/já foi validado e consumido/i)).toBeDefined();
    });

    it('exibe resultado WRONG_EVENT quando o ingresso pertence a outro evento', async () => {
      const user = userEvent.setup();
      const mockResponse: ValidateTicketResponse = {
        result: 'WRONG_EVENT',
        validationAttemptId: 'attempt-uuid-4',
        selectedEventId: 'event-uuid-1',
        ticketId: 'ticket-uuid-4',
        method: 'MANUAL',
        processedAt: '2026-08-16T21:33:00Z',
      };
      vi.spyOn(gateApi, 'validateTicket').mockResolvedValue(mockResponse);

      render(
        <GateView
          user={gateUser}
          initialSelectedEvent={mockPublishedEvents[0]}
        />
      );

      await user.type(screen.getByTestId('gate-manual-code-input'), 'OTHER-EVENT-CODE');
      await user.click(screen.getByTestId('gate-validate-btn'));

      const resultCard = await screen.findByTestId('gate-result-wrong_event');
      expect(resultCard).toBeDefined();
      expect(within(resultCard).getByText('OUTRO EVENTO')).toBeDefined();
      expect(within(resultCard).getByText('Evento Incorreto')).toBeDefined();
      expect(within(resultCard).getByText(/pertence a outro evento/i)).toBeDefined();
    });

    it('exibe banner de erro de rede permitindo retry sem consumir ingresso', async () => {
      const user = userEvent.setup();
      vi.spyOn(gateApi, 'validateTicket').mockRejectedValue(
        new gateApi.GateClientError('GATE_INVALID_RESPONSE', 'Falha na conexão com o servidor.')
      );

      render(
        <GateView
          user={gateUser}
          initialSelectedEvent={mockPublishedEvents[0]}
        />
      );

      await user.type(screen.getByTestId('gate-manual-code-input'), 'ABCD-1234-EF');
      await user.click(screen.getByTestId('gate-validate-btn'));

      const errorBanner = await screen.findByTestId('gate-error-banner');
      expect(errorBanner).toBeDefined();
      expect(within(errorBanner).getByText(/Falha na conexão com o servidor/i)).toBeDefined();
      expect(within(errorBanner).getByText(/Nenhum ingresso foi consumido/i)).toBeDefined();
    });
  });
});
