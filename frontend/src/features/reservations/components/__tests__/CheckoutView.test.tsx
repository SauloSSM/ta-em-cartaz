import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CheckoutView } from '../CheckoutView';
import type { ReservationResponse } from '../../api/reservationsApi';
import * as paymentsApi from '../../../payments/api/paymentsApi';

const sampleReservation: ReservationResponse = {
  id: 'res-s04',
  customerId: 'cust-10',
  eventId: 'evt-20',
  sectorId: 'sec-30',
  quantity: 2,
  unitPrice: 125.0,
  totalAmount: 250.0,
  status: 'HOLDING',
  expiresAt: '2026-08-16T12:10:00.000Z',
  createdAt: '2026-08-16T12:00:00.000Z',
  serverNow: '2026-08-16T12:00:00.000Z',
};

describe('CheckoutView (Superfície S04)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders S04 checkout with snapshot details, timer, and demo payment section', () => {
    const onBackToEvent = vi.fn();
    const onBackToCatalog = vi.fn();

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        eventDate="2026-11-20T20:00:00.000Z"
        eventVenue="Auditório Ibirapuera"
        sectorName="Plateia Central"
        onBackToEvent={onBackToEvent}
        onBackToCatalog={onBackToCatalog}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();
    expect(screen.getByText('Checkout — Concluir Reserva')).toBeDefined();
    expect(screen.getByTestId('demo-environment-notice')).toBeDefined();
    expect(screen.getByTestId('reservation-timer')).toBeDefined();
    expect(screen.getByTestId('checkout-summary')).toBeDefined();
    expect(screen.getByText('Festival de MPB')).toBeDefined();
    expect(screen.getByText('Plateia Central')).toBeDefined();
    expect(screen.getByText('2 ingressos')).toBeDefined();
    expect(screen.getByText(/125,00/)).toBeDefined();
    expect(screen.getByText(/250,00/)).toBeDefined();
    expect(screen.getByTestId('payment-section')).toBeDefined();
    expect(screen.getByTestId('simulate-declined-payment-btn')).toBeDefined();
  });

  it('processes simulated DECLINED payment, displays clear decline feedback and allows retry', async () => {
    const processPaymentSpy = vi.spyOn(paymentsApi, 'processPayment').mockResolvedValue({
      id: 'pay-attempt-1',
      reservationId: 'res-s04',
      amount: 250.0,
      currency: 'BRL',
      status: 'DECLINED',
      provider: 'FAKE',
      declineReason: 'SIMULATED_DECLINE',
      createdAt: '2026-08-16T12:01:00.000Z',
      processedAt: '2026-08-16T12:01:00.000Z',
    });

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        sectorName="Plateia Central"
        onBackToEvent={vi.fn()}
        onBackToCatalog={vi.fn()}
      />,
    );

    const payBtn = screen.getByTestId('simulate-declined-payment-btn');
    fireEvent.click(payBtn);

    await waitFor(() => {
      expect(processPaymentSpy).toHaveBeenCalledWith('res-s04', {
        paymentAttemptId: expect.any(String),
        simulatedOutcome: 'DECLINED',
      });
    });

    // Feedback específico de recusa
    const declinedAlert = await screen.findByTestId('payment-declined-alert');
    expect(declinedAlert).toBeDefined();
    expect(screen.getByText('Tentativa de Pagamento Recusada')).toBeDefined();
    expect(screen.getByText(/SIMULATED_DECLINE/)).toBeDefined();
    expect(screen.getByText(/pay-attempt-1/)).toBeDefined();

    // Botão muda para permitir nova tentativa
    expect(screen.getByText('Tentar Novamente (Simular Recusa)')).toBeDefined();

    // Reserva continua HOLDING (não vai para tela de sucesso)
    expect(screen.getByTestId('checkout-view')).toBeDefined();
    expect(screen.queryByTestId('checkout-expired-alert')).toBeNull();
  });

  it('handles payment error when reservation expired during checkout (422)', async () => {
    vi.spyOn(paymentsApi, 'processPayment').mockRejectedValue(
      new paymentsApi.PaymentClientError('RESERVATION_EXPIRED', 'A reserva expirou.'),
    );

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        sectorName="Plateia Central"
        onBackToEvent={vi.fn()}
        onBackToCatalog={vi.fn()}
      />,
    );

    const payBtn = screen.getByTestId('simulate-declined-payment-btn');
    fireEvent.click(payBtn);

    const expiredAlert = await screen.findByTestId('checkout-expired-alert');
    expect(expiredAlert).toBeDefined();
    expect(screen.queryByTestId('payment-section')).toBeNull();
  });

  it('handles deterministic payment errors and renders error alert', async () => {
    vi.spyOn(paymentsApi, 'processPayment').mockRejectedValue(
      new paymentsApi.PaymentClientError('AUTH_INVALID_REQUEST', 'Requisição de pagamento inválida.'),
    );

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        sectorName="Plateia Central"
        onBackToEvent={vi.fn()}
        onBackToCatalog={vi.fn()}
      />,
    );

    const payBtn = screen.getByTestId('simulate-declined-payment-btn');
    fireEvent.click(payBtn);

    const errorAlert = await screen.findByTestId('payment-error-alert');
    expect(errorAlert).toBeDefined();
    expect(screen.getByText('Requisição de pagamento inválida.')).toBeDefined();
  });

  it('switches to expired state when timer reaches zero, focuses alert and hides payment', () => {
    vi.useFakeTimers();
    const onBackToEvent = vi.fn();
    const onBackToCatalog = vi.fn();

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        sectorName="Plateia Central"
        onBackToEvent={onBackToEvent}
        onBackToCatalog={onBackToCatalog}
      />,
    );

    expect(screen.queryByTestId('checkout-expired-alert')).toBeNull();

    // Advance 10 minutes and 1 second to expire the hold
    act(() => {
      vi.advanceTimersByTime(601 * 1000);
    });

    const expiredAlert = screen.getByTestId('checkout-expired-alert');
    expect(expiredAlert).toBeDefined();
    expect(screen.getByText('O tempo da sua reserva expirou')).toBeDefined();

    // Heading must have focus for accessibility
    const alertHeading = screen.getByTestId('expired-alert-heading');
    expect(document.activeElement).toBe(alertHeading);

    // Payment section must be hidden
    expect(screen.queryByTestId('payment-section')).toBeNull();

    // Return to event CTA
    const returnBtn = screen.getByTestId('return-to-event-btn');
    fireEvent.click(returnBtn);
    expect(onBackToEvent).toHaveBeenCalledTimes(1);

    vi.useRealTimers();
  });

  it('renders expired state immediately if initial reservation status is EXPIRED', () => {
    const onBackToEvent = vi.fn();
    const onBackToCatalog = vi.fn();

    render(
      <CheckoutView
        reservation={{
          ...sampleReservation,
          status: 'EXPIRED',
        }}
        eventTitle="Festival de MPB"
        onBackToEvent={onBackToEvent}
        onBackToCatalog={onBackToCatalog}
      />,
    );

    expect(screen.getByTestId('checkout-expired-alert')).toBeDefined();
    expect(screen.queryByTestId('payment-section')).toBeNull();
  });

  it('processes simulated APPROVED payment, transitions to confirmed state, disables timer and focuses confirmation alert', async () => {
    const processPaymentSpy = vi.spyOn(paymentsApi, 'processPayment').mockResolvedValue({
      id: 'pay-attempt-approved',
      reservationId: 'res-s04',
      amount: 250.0,
      currency: 'BRL',
      status: 'APPROVED',
      provider: 'FAKE',
      createdAt: '2026-08-16T12:01:00.000Z',
      processedAt: '2026-08-16T12:01:00.000Z',
    });

    const onReconcile = vi.fn();

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        sectorName="Plateia Central"
        onBackToEvent={vi.fn()}
        onBackToCatalog={vi.fn()}
        onReconcile={onReconcile}
      />,
    );

    const approveBtn = screen.getByTestId('simulate-approved-payment-btn');
    fireEvent.click(approveBtn);

    await waitFor(() => {
      expect(processPaymentSpy).toHaveBeenCalledWith('res-s04', {
        paymentAttemptId: expect.any(String),
        simulatedOutcome: 'APPROVED',
      });
    });

    // Feedback de confirmação autoritativa
    const confirmedAlert = await screen.findByTestId('checkout-confirmed-alert');
    expect(confirmedAlert).toBeDefined();
    expect(screen.getByText('Pagamento Aprovado e Reserva Confirmada!')).toBeDefined();
    expect(screen.getByText(/pay-attempt-approved/)).toBeDefined();

    // Seção de simulação de pagamento é ocultada
    expect(screen.queryByTestId('payment-section')).toBeNull();

    // Timer é desabilitado/ocultado após confirmação
    expect(screen.queryByTestId('reservation-timer')).toBeNull();

    // Título da página atualiza para Reserva Confirmada
    expect(screen.getByText('Reserva Confirmada')).toBeDefined();
  });

  it('renders confirmed state immediately if initial reservation status is CONFIRMED', () => {
    render(
      <CheckoutView
        reservation={{
          ...sampleReservation,
          status: 'CONFIRMED',
        }}
        eventTitle="Festival de MPB"
        onBackToEvent={vi.fn()}
        onBackToCatalog={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-confirmed-alert')).toBeDefined();
    expect(screen.queryByTestId('payment-section')).toBeNull();
    expect(screen.queryByTestId('reservation-timer')).toBeNull();
  });

  it('navigates back to event and catalog using nav links', () => {
    const onBackToEvent = vi.fn();
    const onBackToCatalog = vi.fn();

    render(
      <CheckoutView
        reservation={sampleReservation}
        eventTitle="Festival de MPB"
        onBackToEvent={onBackToEvent}
        onBackToCatalog={onBackToCatalog}
      />,
    );

    fireEvent.click(screen.getByText('← Voltar para o Evento'));
    expect(onBackToEvent).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByText('Ir para o Catálogo'));
    expect(onBackToCatalog).toHaveBeenCalledTimes(1);
  });

  describe('Story 5.3 — Reconciliação de resposta perdida sem nova cobrança', () => {
    beforeEach(() => {
      sessionStorage.clear();
    });

    it('entra em estado de verificação (verifying) após erro de rede / resposta perdida, mantendo o mesmo paymentAttemptId e sem exibir recusa', async () => {
      // Simula falha de conexão na primeira tentativa
      vi.spyOn(paymentsApi, 'processPayment').mockRejectedValueOnce(
        new TypeError('Failed to fetch'),
      );

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      const payBtn = screen.getByTestId('simulate-approved-payment-btn');
      fireEvent.click(payBtn);

      // Entra em verifying alert
      const verifyingAlert = await screen.findByTestId('payment-verifying-alert');
      expect(verifyingAlert).toBeDefined();
      expect(screen.getByTestId('verifying-alert-heading')).toBeDefined();
      expect(screen.getByText('Verificando Situação do Pagamento')).toBeDefined();

      // Heading deve ter foco para acessibilidade
      const alertHeading = screen.getByTestId('verifying-alert-heading');
      expect(document.activeElement).toBe(alertHeading);

      // Não mostra "pagamento recusado"
      expect(screen.queryByTestId('payment-declined-alert')).toBeNull();

      // Botões normais de aprovar/recusar devem estar ocultos para não permitir nova cobrança acidental
      expect(screen.queryByTestId('simulate-approved-payment-btn')).toBeNull();
      expect(screen.queryByTestId('simulate-declined-payment-btn')).toBeNull();

      // Botão de reconciliação presente
      expect(screen.getByTestId('reconcile-payment-btn')).toBeDefined();
      expect(screen.getByText('Verificar Novamente')).toBeDefined();

      // Tentativa está armazenada no sessionStorage
      const rawStored = sessionStorage.getItem(`edt.uncertain-payment.v1:${sampleReservation.id}`);
      expect(rawStored).not.toBeNull();
      const parsed = JSON.parse(rawStored!);
      expect(parsed.reservationId).toBe(sampleReservation.id);
      expect(parsed.simulatedOutcome).toBe('APPROVED');
      expect(parsed.paymentAttemptId).toBeDefined();
    });

    it('reconcilia tentativa APPROVED com sucesso ao clicar em "Verificar Novamente", reutilizando o mesmo paymentAttemptId e confirmando a compra', async () => {
      let capturedAttemptId = '';
      const processPaymentSpy = vi.spyOn(paymentsApi, 'processPayment')
        .mockImplementationOnce(async (_resId, req) => {
          capturedAttemptId = req.paymentAttemptId;
          throw new TypeError('Network connection lost');
        })
        .mockImplementationOnce(async (_resId, req) => {
          expect(req.paymentAttemptId).toBe(capturedAttemptId);
          expect(req.simulatedOutcome).toBe('APPROVED');
          return {
            id: req.paymentAttemptId,
            reservationId: 'res-s04',
            amount: 250.0,
            currency: 'BRL',
            status: 'APPROVED',
            provider: 'FAKE',
            createdAt: '2026-08-16T12:01:00.000Z',
            processedAt: '2026-08-16T12:01:00.000Z',
          };
        });

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      // 1. Dispara submissão inicial que falha na rede
      fireEvent.click(screen.getByTestId('simulate-approved-payment-btn'));

      await screen.findByTestId('payment-verifying-alert');
      expect(processPaymentSpy).toHaveBeenCalledTimes(1);

      // 2. Clica em "Verificar Novamente"
      const reconcileBtn = screen.getByTestId('reconcile-payment-btn');
      fireEvent.click(reconcileBtn);

      // 3. Deve resolver para CONFIRMED e limpar o estado incerto do sessionStorage
      await screen.findByTestId('checkout-confirmed-alert');
      expect(processPaymentSpy).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Pagamento Aprovado e Reserva Confirmada!')).toBeDefined();

      expect(sessionStorage.getItem(`edt.uncertain-payment.v1:${sampleReservation.id}`)).toBeNull();
    });

    it('reconcilia tentativa DECLINED com sucesso, exibe alerta de recusa e gera novo attemptId para a próxima tentativa deliberada', async () => {
      let capturedAttemptId = '';
      const processPaymentSpy = vi.spyOn(paymentsApi, 'processPayment')
        .mockImplementationOnce(async (_resId, req) => {
          capturedAttemptId = req.paymentAttemptId;
          throw new TypeError('Network timeout');
        })
        .mockImplementationOnce(async (_resId, req) => {
          expect(req.paymentAttemptId).toBe(capturedAttemptId);
          expect(req.simulatedOutcome).toBe('DECLINED');
          return {
            id: req.paymentAttemptId,
            reservationId: 'res-s04',
            amount: 250.0,
            currency: 'BRL',
            status: 'DECLINED',
            provider: 'FAKE',
            declineReason: 'SIMULATED_DECLINE',
            createdAt: '2026-08-16T12:01:00.000Z',
            processedAt: '2026-08-16T12:01:00.000Z',
          };
        });

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      fireEvent.click(screen.getByTestId('simulate-declined-payment-btn'));

      await screen.findByTestId('payment-verifying-alert');

      fireEvent.click(screen.getByTestId('reconcile-payment-btn'));

      // Resolve para DECLINED
      await screen.findByTestId('payment-declined-alert');
      expect(processPaymentSpy).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Tentativa de Pagamento Recusada')).toBeDefined();
      expect(sessionStorage.getItem(`edt.uncertain-payment.v1:${sampleReservation.id}`)).toBeNull();

      // Botões de pagamento voltam a estar disponíveis com novo ID
      expect(screen.getByTestId('simulate-declined-payment-btn')).toBeDefined();
      expect(screen.getByTestId('simulate-approved-payment-btn')).toBeDefined();
    });

    it('restaura estado de verificação a partir do sessionStorage após reload e reconcilia com o mesmo ID', async () => {
      const storedAttemptId = '00000000-1111-2222-3333-444444444444';
      sessionStorage.setItem(
        `edt.uncertain-payment.v1:${sampleReservation.id}`,
        JSON.stringify({
          reservationId: sampleReservation.id,
          paymentAttemptId: storedAttemptId,
          simulatedOutcome: 'APPROVED',
          timestamp: Date.now(),
        }),
      );

      const processPaymentSpy = vi.spyOn(paymentsApi, 'processPayment').mockResolvedValue({
        id: storedAttemptId,
        reservationId: 'res-s04',
        amount: 250.0,
        currency: 'BRL',
        status: 'APPROVED',
        provider: 'FAKE',
        createdAt: '2026-08-16T12:01:00.000Z',
        processedAt: '2026-08-16T12:01:00.000Z',
      });

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      // Renderiza imediatamente em verifying alert com o ID persistido
      expect(screen.getByTestId('payment-verifying-alert')).toBeDefined();
      expect(screen.getByText(storedAttemptId)).toBeDefined();

      // Clica em verificar novamente
      fireEvent.click(screen.getByTestId('reconcile-payment-btn'));

      await waitFor(() => {
        expect(processPaymentSpy).toHaveBeenCalledWith('res-s04', {
          paymentAttemptId: storedAttemptId,
          simulatedOutcome: 'APPROVED',
        });
      });

      await screen.findByTestId('checkout-confirmed-alert');
      expect(sessionStorage.getItem(`edt.uncertain-payment.v1:${sampleReservation.id}`)).toBeNull();
    });

    it('trata falha contínua de rede durante reconciliação permanecendo no estado de verificação com opção de retry', async () => {
      vi.spyOn(paymentsApi, 'processPayment')
        .mockRejectedValueOnce(new TypeError('Network offline'))
        .mockRejectedValueOnce(new TypeError('Still offline'));

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      fireEvent.click(screen.getByTestId('simulate-approved-payment-btn'));

      await screen.findByTestId('payment-verifying-alert');

      fireEvent.click(screen.getByTestId('reconcile-payment-btn'));

      // Permanece em verifying alert e exibe mensagem de erro na verificação
      const verifyingAlert = await screen.findByTestId('payment-verifying-alert');
      expect(verifyingAlert).toBeDefined();
      expect(screen.getByText(/Não foi possível conectar ao servidor/)).toBeDefined();
      expect(screen.getByTestId('reconcile-payment-btn')).toBeDefined();
    });

    it('trata expiração (422) retornada durante a reconciliação', async () => {
      vi.spyOn(paymentsApi, 'processPayment')
        .mockRejectedValueOnce(new TypeError('Network lost'))
        .mockRejectedValueOnce(
          new paymentsApi.PaymentClientError('RESERVATION_EXPIRED', 'A reserva expirou.'),
        );

      render(
        <CheckoutView
          reservation={sampleReservation}
          eventTitle="Festival de MPB"
          sectorName="Plateia Central"
          onBackToEvent={vi.fn()}
          onBackToCatalog={vi.fn()}
        />,
      );

      fireEvent.click(screen.getByTestId('simulate-approved-payment-btn'));

      await screen.findByTestId('payment-verifying-alert');

      fireEvent.click(screen.getByTestId('reconcile-payment-btn'));

      // Transiciona para o alerta de expiração
      await screen.findByTestId('checkout-expired-alert');
      expect(sessionStorage.getItem(`edt.uncertain-payment.v1:${sampleReservation.id}`)).toBeNull();
      expect(screen.queryByTestId('payment-section')).toBeNull();
    });
  });
});

