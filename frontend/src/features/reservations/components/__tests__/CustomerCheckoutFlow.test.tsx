import { render, screen, act, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AuthenticatedSession } from '../../../auth/AuthenticatedSession';
import type { SessionUser } from '../../../../app/api/authApi';
import type { ReservationResponse } from '../../api/reservationsApi';
import { saveActiveHold, clearActiveHold } from '../../model/activeHold';

const customerUser: SessionUser = {
  id: '00000000-0000-0000-0000-000000000002',
  email: 'customer.one@demo.elitedevticket.local',
  role: 'CUSTOMER',
};

const mockEvent = {
  id: 'ev-flow-1',
  organizerId: '00000000-0000-0000-0000-000000000001',
  title: 'Festival Primavera Sound 2026',
  description: 'O maior festival indie da temporada.',
  venue: 'Parque Anhembi',
  status: 'PUBLISHED',
  startsAt: '2026-11-20T20:00:00Z',
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

const mockSectors = {
  sectors: [
    {
      id: 'sec-flow-pista',
      eventId: 'ev-flow-1',
      name: 'Pista Premium',
      capacity: 100,
      availableQuantity: 50,
      price: 250.0,
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
  ],
};

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('Story 4.5 — Customer Checkout and Authoritative Hold Flow', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';

      if (url.includes('/api/v1/catalog/ticketmaster')) {
        return Promise.resolve(jsonResponse({ events: [] }));
      }
      if (url.includes('/sectors') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockSectors));
      }
      if (url.endsWith('/events/ev-flow-1') && method === 'GET') {
        return Promise.resolve(jsonResponse(mockEvent));
      }
      if (url.includes('/events') && method === 'GET') {
        return Promise.resolve(jsonResponse({ events: [mockEvent] }));
      }
      if (url.includes('/reservations') && method === 'POST') {
        const res: ReservationResponse = {
          id: 'res-flow-100',
          customerId: customerUser.id,
          eventId: 'ev-flow-1',
          sectorId: 'sec-flow-pista',
          quantity: 2,
          unitPrice: 250.0,
          totalAmount: 500.0,
          status: 'HOLDING',
          expiresAt: new Date(Date.now() + 600000).toISOString(),
          createdAt: new Date().toISOString(),
          serverNow: new Date().toISOString(),
        };
        return Promise.resolve(jsonResponse(res, 201));
      }
      return Promise.resolve(jsonResponse({}));
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    clearActiveHold();
  });

  it('exibe checkout S04 com snapshots autoritativos (unitPrice, totalAmount, status, timer)', () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-snap-1',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 3,
      unitPrice: 250.0,
      totalAmount: 750.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
      eventVenue: 'Parque Anhembi',
    });

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    // Deve abrir diretamente na S04 (CheckoutView) pois há hold ativo no storage
    expect(screen.getByTestId('checkout-view')).toBeDefined();
    expect(screen.getByText('Checkout — Concluir Reserva')).toBeDefined();
    expect(screen.getByTestId('demo-environment-notice')).toBeDefined();

    // Valida snapshots exclusivos
    expect(screen.getByText('Festival Primavera Sound 2026')).toBeDefined();
    expect(screen.getByText('Parque Anhembi')).toBeDefined();
    expect(screen.getByText('Pista Premium')).toBeDefined();
    expect(screen.getByText('3 ingressos')).toBeDefined();
    expect(screen.getAllByText(/250,00/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/750,00/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/HOLDING/)).toBeDefined();

    // Timer inicial derivado de serverNow/expiresAt
    expect(screen.getByTestId('timer-countdown').textContent).toBe('10:00');
    expect(screen.getByText('Tempo normal')).toBeDefined();
  });

  it('acesso "Continuar reserva" permite navegar ao checkout existente sem criar novo hold', () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-snap-2',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    // Estando no checkout, clica para voltar ao catálogo
    const toCatalogBtn = screen.getByRole('button', { name: 'Voltar para o catálogo de eventos' });
    fireEvent.click(toCatalogBtn);

    // No catálogo, vê o banner "Continuar reserva"
    expect(screen.getByTestId('active-reservation-banner')).toBeDefined();
    expect(screen.getByText(/Festival Primavera Sound 2026/)).toBeDefined();

    // Clica em "Continuar reserva"
    const continueBtn = screen.getByTestId('continue-reservation-btn');
    fireEvent.click(continueBtn);

    // Retorna ao checkout existente sem chamada HTTP de novo hold
    expect(screen.getByTestId('checkout-view')).toBeDefined();
    expect(screen.getByText('Checkout — Concluir Reserva')).toBeDefined();
    expect(screen.getAllByText(/500,00/).length).toBeGreaterThanOrEqual(1);
  });

  it('quando o hold expira, remove ações de pagamento, foca a mensagem de expiração e oferece retorno ao evento', () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-snap-3',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();
    expect(screen.getByTestId('payment-section')).toBeDefined();

    // Avança o tempo até a expiração (10 min e 1 seg)
    act(() => {
      vi.advanceTimersByTime(601 * 1000);
    });

    // Alerta de expiração é exibido e focado
    expect(screen.getByTestId('checkout-expired-alert')).toBeDefined();
    const heading = screen.getByTestId('expired-alert-heading');
    expect(document.activeElement).toBe(heading);

    // Bloco de pagamento é removido
    expect(screen.queryByTestId('payment-section')).toBeNull();

    // Opção de retorno ao evento
    const returnBtn = screen.getByTestId('return-to-event-btn');
    expect(returnBtn).toBeDefined();
  });

  it('restauração após reload com tempo decorrido não reinicia o cronômetro para 10 minutos', () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-snap-reload',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    // Simula que 5 minutos se passaram desde que foi salvo no sessionStorage
    const stored = JSON.parse(sessionStorage.getItem('edt.active-hold.v1')!);
    stored.savedAtClientEpochMs = Date.now() - 300000;
    sessionStorage.setItem('edt.active-hold.v1', JSON.stringify(stored));

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();

    // Cronômetro deve exibir 05:00 e NÃO 10:00
    expect(screen.getByTestId('timer-countdown').textContent).toBe('05:00');
    expect(screen.getByText('Tempo normal')).toBeDefined();
  });

  it('aciona reconciliação autoritativa com o backend ao retornar para a aba (visibilitychange)', async () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-snap-vis',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();

    // Dispara visibilitychange para visible
    act(() => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        writable: true,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
    });

    // Reconciliação chama o endpoint de reserva
    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/reservations'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('reload com relógio civil do cliente manipulado: backend continua determinando o tempo e status autoritativos reais', async () => {
    // Hold provisório no sessionStorage com snapshot antigo
    const activeHoldRes: ReservationResponse = {
      id: 'res-reconcile-clock',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: '2026-08-16T12:10:00.000Z',
      createdAt: '2026-08-16T12:00:00.000Z',
      serverNow: '2026-08-16T12:00:00.000Z',
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    // O backend retorna o estado autoritativo real no momento do reload (restando 2m30s = 150s)
    const authoritativeBackendRes: ReservationResponse = {
      id: 'res-reconcile-clock',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: '2026-08-16T12:10:00.000Z',
      createdAt: '2026-08-16T12:00:00.000Z',
      serverNow: '2026-08-16T12:07:30.000Z', // 150 segundos restantes
    };

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';
      if (url.includes('/reservations') && method === 'POST') {
        return Promise.resolve(jsonResponse(authoritativeBackendRes, 201));
      }
      return Promise.resolve(jsonResponse({}));
    });

    // Simula relógio civil do cliente descalibrado/manipulado
    vi.setSystemTime(new Date('2026-08-16T15:00:00.000Z'));

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();

    // Aguarda a resolução assíncrona da reconciliação com o backend
    await act(async () => {
      await Promise.resolve();
    });

    // O cronômetro passa a exibir a contagem autoritativa do backend (02:30, warning)
    expect(screen.getByTestId('timer-countdown').textContent).toBe('02:30');
    expect(screen.getByText('Tempo acabando')).toBeDefined();
  });

  it('Story 5.3 — erro de rede após submissão APPROVED entra em verifying e permite reconciliar sem nova cobrança', async () => {
    const activeHoldRes: ReservationResponse = {
      id: 'res-flow-lost-resp',
      customerId: customerUser.id,
      eventId: 'ev-flow-1',
      sectorId: 'sec-flow-pista',
      quantity: 2,
      unitPrice: 250.0,
      totalAmount: 500.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 600000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    saveActiveHold({
      reservation: activeHoldRes,
      eventTitle: 'Festival Primavera Sound 2026',
      sectorName: 'Pista Premium',
    });

    let paymentCalls = 0;
    let attemptIdUsed = '';

    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation((input, init) => {
      const url = String(input);
      const method = init?.method ?? 'GET';

      if (url.includes('/payments') && method === 'POST') {
        paymentCalls++;
        const body = JSON.parse(String(init?.body));
        attemptIdUsed = body.paymentAttemptId;

        if (paymentCalls === 1) {
          // Primeira chamada: conexão falha antes da resposta
          return Promise.reject(new TypeError('Failed to fetch'));
        }

        // Segunda chamada (reconciliação): retorna APPROVED com o mesmo ID
        return Promise.resolve(jsonResponse({
          id: attemptIdUsed,
          reservationId: 'res-flow-lost-resp',
          amount: 500.0,
          currency: 'BRL',
          status: 'APPROVED',
          provider: 'FAKE',
          createdAt: new Date().toISOString(),
          processedAt: new Date().toISOString(),
        }));
      }

      return Promise.resolve(jsonResponse({}));
    });

    render(
      <AuthenticatedSession
        user={customerUser}
        busy={false}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByTestId('checkout-view')).toBeDefined();

    // 1. Customer clica para aprovar pagamento (falha na rede)
    const approveBtn = screen.getByTestId('simulate-approved-payment-btn');
    await act(async () => {
      fireEvent.click(approveBtn);
    });

    // 2. Entra em estado de verificação com aviso apropriado
    expect(screen.getByTestId('payment-verifying-alert')).toBeDefined();
    expect(screen.getByText('Verificando Situação do Pagamento')).toBeDefined();
    expect(screen.queryByTestId('simulate-approved-payment-btn')).toBeNull();

    const firstAttemptId = attemptIdUsed;

    // 3. Customer clica em "Verificar Novamente"
    const reconcileBtn = screen.getByTestId('reconcile-payment-btn');
    await act(async () => {
      fireEvent.click(reconcileBtn);
    });

    // 4. Confirmação autoritativa exibida usando o MESMO attemptId
    expect(screen.getByTestId('checkout-confirmed-alert')).toBeDefined();
    expect(attemptIdUsed).toBe(firstAttemptId);
    expect(paymentCalls).toBe(2);
  });
});
