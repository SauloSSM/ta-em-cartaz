import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SessionUser } from '../../../../app/api/authApi';
import type { PublicEventResponse, TicketSectorListResponse } from '../../api/eventsApi';
import type { ReservationResponse } from '../../../reservations';
import { getPurchaseIntention } from '../../model/purchaseIntention';
import { PublicEventDetail } from '../PublicEventDetail';

const originalFetch = globalThis.fetch;

const mockPublishedEvent: PublicEventResponse = {
  id: 'ev-pub-123',
  title: 'Festival Primavera Sound 2026',
  description: 'O maior festival de música e cultura urbana.',
  imageUrl: 'https://images.example.com/banner.jpg',
  category: 'Festival',
  status: 'PUBLISHED',
  venueName: 'Autódromo de Interlagos',
  venueAddress: 'Av. Senador Teotônio Vilela, 261, São Paulo - SP',
  startsAt: '2026-11-20T20:00:00Z',
  startingPrice: 150.0,
  salesClosed: false,
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

const mockSectors: TicketSectorListResponse = {
  sectors: [
    {
      id: 'sec-pista',
      eventId: 'ev-pub-123',
      name: 'Pista Comum',
      description: 'Acesso à pista geral e praça de alimentação',
      capacity: 500,
      availableQuantity: 500,
      price: 150.0,
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
    {
      id: 'sec-vip',
      eventId: 'ev-pub-123',
      name: 'Camarote VIP',
      description: 'Área coberta com open bar e vista privilegiada',
      capacity: 100,
      availableQuantity: 4,
      price: 450.0,
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
    {
      id: 'sec-esgotado',
      eventId: 'ev-pub-123',
      name: 'Lounge Premium',
      description: 'Acesso exclusivo',
      capacity: 50,
      availableQuantity: 0,
      price: 800.0,
      createdAt: '2026-08-15T10:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
  ],
};

const customerUser: SessionUser = {
  id: 'usr-cust-1',
  email: 'customer@demo.elitedevticket.local',
  role: 'CUSTOMER',
};

const organizerUser: SessionUser = {
  id: 'usr-org-1',
  email: 'organizer@demo.elitedevticket.local',
  role: 'ORGANIZER',
};

beforeEach(() => {
  sessionStorage.clear();
});

afterEach(() => {
  globalThis.fetch = originalFetch;
  sessionStorage.clear();
});

describe('PublicEventDetail component (Superfície S02)', () => {
  it('exibe estado de loading acessível durante carregamento inicial', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockImplementation(
      () => new Promise(() => {}),
    );

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        onBackToCatalog={vi.fn()}
      />,
    );

    expect(screen.getByRole('status').textContent).toContain('Carregando detalhes do evento…');
    expect(screen.getByRole('button', { name: 'Voltar para o catálogo de eventos' })).toBeDefined();
  });

  it('exibe detalhes completos de evento publicado', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        onBackToCatalog={vi.fn()}
      />,
    );

    expect(await screen.findByRole('heading', { level: 1, name: 'Festival Primavera Sound 2026' })).toBeDefined();
    expect(screen.getByText('O maior festival de música e cultura urbana.')).toBeDefined();
    expect(screen.getByText('Autódromo de Interlagos — Av. Senador Teotônio Vilela, 261, São Paulo - SP')).toBeDefined();
    expect(screen.getByText('Festival')).toBeDefined();
  });

  it('exibe alerta e desabilita compra quando evento tiver vendas encerradas', async () => {
    const closedEvent: PublicEventResponse = {
      ...mockPublishedEvent,
      id: 'ev-closed-1',
      salesClosed: true,
    };

    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    render(
      <PublicEventDetail
        eventId="ev-closed-1"
        initialEvent={closedEvent}
        onBackToCatalog={vi.fn()}
      />,
    );

    expect(await screen.findByTestId('sales-closed-alert')).toBeDefined();
    expect(screen.getByTestId('sales-closed-alert').textContent).toContain('Vendas Encerradas');
    expect(screen.getByText('Vendas encerradas')).toBeDefined();

    expect(screen.queryByTestId('purchase-intention-box')).toBeNull();
    expect(screen.queryByRole('button', { name: /Reservar/ })).toBeNull();
  });

  it('permite selecionar setor e ajustar quantidade 1–6 limitada pelo estoque', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        onBackToCatalog={vi.fn()}
      />,
    );

    const vipSectorBtn = await screen.findByRole('button', { name: /Selecionar setor Camarote VIP/ });
    await user.click(vipSectorBtn);

    expect(screen.getAllByText('Camarote VIP').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('1');
    expect(screen.getAllByText(/450,00/).length).toBeGreaterThanOrEqual(1);

    const incBtn = screen.getByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('2');
    expect(screen.getByText(/900,00/)).toBeDefined();

    await user.click(incBtn);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('3');
    expect(screen.getByText(/1\.350,00/)).toBeDefined();

    await user.click(incBtn);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('4');
    expect(screen.getByText(/1\.800,00/)).toBeDefined();

    expect((incBtn as HTMLButtonElement).disabled).toBe(true);
  });

  it('visitante anônimo forma intenção no sessionStorage e é encaminhado para login', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    const handleLoginRedirect = vi.fn();
    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        currentUser={null}
        onBackToCatalog={vi.fn()}
        onProceedToLogin={handleLoginRedirect}
      />,
    );

    const incBtn = await screen.findByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn);

    const reserveBtn = screen.getByRole('button', { name: /Reservar 2 ingressos no setor Pista Comum/ });
    await user.click(reserveBtn);

    const savedIntention = getPurchaseIntention();
    expect(savedIntention).not.toBeNull();
    expect(savedIntention?.eventId).toBe('ev-pub-123');
    expect(savedIntention?.ticketSectorId).toBe('sec-pista');
    expect(savedIntention?.quantity).toBe(2);
    expect(savedIntention?.internalReturnPath).toBe('/events/ev-pub-123');

    expect(handleLoginRedirect).toHaveBeenCalledTimes(1);
    expect(handleLoginRedirect).toHaveBeenCalledWith(
      expect.stringContaining('acesse sua conta de Cliente (CUSTOMER)'),
    );
  });

  it('cliente autenticado (CUSTOMER) cria hold de reserva com sucesso', async () => {
    const mockReservation: ReservationResponse = {
      id: 'res-123',
      customerId: 'usr-cust-1',
      eventId: 'ev-pub-123',
      sectorId: 'sec-pista',
      quantity: 2,
      unitPrice: 150.0,
      totalAmount: 300.0,
      status: 'HOLDING',
      expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
      createdAt: new Date().toISOString(),
      serverNow: new Date().toISOString(),
    };

    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(mockSectors))
      .mockResolvedValueOnce(jsonResponse(mockReservation, 201));

    const handleReservation = vi.fn();
    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        currentUser={customerUser}
        onBackToCatalog={vi.fn()}
        onReservationCreated={handleReservation}
      />,
    );

    const incBtn = await screen.findByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn);

    const reserveBtn = screen.getByRole('button', { name: /Reservar 2 ingressos no setor Pista Comum/ });
    await user.click(reserveBtn);

    expect(await screen.findByTestId('active-hold-card')).toBeDefined();
    expect(screen.getByText('Ingressos Pré-Reservados (Hold)')).toBeDefined();
    expect(screen.getAllByText(/300,00/).length).toBeGreaterThanOrEqual(1);
    expect(handleReservation).toHaveBeenCalledWith(mockReservation);
  });

  it('exibe erro de disponibilidade insuficiente sem reduzir silenciosamente a quantidade', async () => {
    globalThis.fetch = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(mockSectors))
      .mockResolvedValueOnce(jsonResponse({
        code: 'INSUFFICIENT_AVAILABILITY',
        message: 'Quantidade indisponível.',
        traceId: 'tr-insufficient',
        timestamp: new Date().toISOString(),
      }, 422));

    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        currentUser={customerUser}
        onBackToCatalog={vi.fn()}
      />,
    );

    const incBtn = await screen.findByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn); // quantity = 2

    const reserveBtn = screen.getByRole('button', { name: /Reservar 2 ingressos no setor Pista Comum/ });
    await user.click(reserveBtn);

    expect(await screen.findByTestId('reservation-error-alert')).toBeDefined();
    expect(screen.getByTestId('reservation-error-alert').textContent).toContain(
      'Não foi possível concluir sua reserva: a quantidade solicitada não está mais disponível no setor selecionado.',
    );

    // Quantidade permanece selecionada como 2 sem redução silenciosa
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('2');
  });

  it('organizador autenticado recebe aviso de papel incompatível ao tentar reservar', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        currentUser={organizerUser}
        onBackToCatalog={vi.fn()}
      />,
    );

    const reserveBtn = await screen.findByRole('button', { name: /Reservar 1 ingresso no setor Pista Comum/ });
    await user.click(reserveBtn);

    expect(await screen.findByTestId('role-error-alert')).toBeDefined();
    expect(screen.getByTestId('role-error-alert').textContent).toContain(
      'Apenas contas com papel de Cliente (CUSTOMER) podem realizar reservas e compras de ingressos',
    );
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
