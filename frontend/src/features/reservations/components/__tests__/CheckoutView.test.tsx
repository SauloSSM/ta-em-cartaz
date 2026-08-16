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

  it('handles general payment errors and renders error alert', async () => {
    vi.spyOn(paymentsApi, 'processPayment').mockRejectedValue(
      new Error('Erro de conexão com o servidor.'),
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
    expect(screen.getByText('Erro de conexão com o servidor.')).toBeDefined();
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
});
