import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SessionUser } from '../../../../app/api/authApi';
import type { PublicEventResponse, TicketSectorListResponse } from '../../api/eventsApi';
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
      () => new Promise(() => {}), // nunca resolve
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

  it('exibe erro 404 quando o evento não existe com opção de voltar ao catálogo', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      jsonResponse(
        {
          code: 'EVENT_NOT_FOUND',
          message: 'Evento não encontrado.',
          traceId: 'trace-404',
          timestamp: '2026-08-15T12:00:00Z',
        },
        404,
      ),
    );

    const handleBack = vi.fn();
    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-inexistente"
        onBackToCatalog={handleBack}
      />,
    );

    expect(await screen.findByRole('heading', { level: 2, name: 'Evento não encontrado' })).toBeDefined();
    expect(screen.getByRole('alert').textContent).toContain('O evento solicitado não foi encontrado ou não está disponível.');

    await user.click(screen.getByRole('button', { name: '← Voltar para o Catálogo de Eventos' }));
    expect(handleBack).toHaveBeenCalledTimes(1);
  });

  it('exibe erro 403 quando um evento DRAFT é acessado publicamente', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValue(
      jsonResponse(
        {
          code: 'EVENT_FORBIDDEN',
          message: 'Acesso negado ao rascunho de evento.',
          traceId: 'trace-403',
          timestamp: '2026-08-15T12:00:00Z',
        },
        403,
      ),
    );

    render(
      <PublicEventDetail
        eventId="ev-draft-secret"
        onBackToCatalog={vi.fn()}
      />,
    );

    expect(await screen.findByRole('heading', { level: 2, name: 'Acesso Restrito' })).toBeDefined();
    expect(screen.getByRole('alert').textContent).toContain(
      'Este evento está em modo rascunho e não está disponível publicamente.',
    );
  });

  it('renderiza todos os detalhes do evento publicado com setores e preço formatado em BRL', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        onBackToCatalog={vi.fn()}
      />,
    );

    // Título principal
    expect(await screen.findByRole('heading', { level: 1, name: 'Festival Primavera Sound 2026' })).toBeDefined();

    // Metadados
    expect(screen.getByText('Autódromo de Interlagos — Av. Senador Teotônio Vilela, 261, São Paulo - SP')).toBeDefined();
    expect(screen.getByText('Festival')).toBeDefined();
    expect(screen.getByText('O maior festival de música e cultura urbana.')).toBeDefined();

    // Setores
    expect(screen.getAllByText('Pista Comum').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/150,00/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/500 ingressos disponíveis/)).toBeDefined();

    expect(screen.getByText('Camarote VIP')).toBeDefined();
    expect(screen.getAllByText(/450,00/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/4 ingressos disponíveis/)).toBeDefined();

    // Setor esgotado
    expect(screen.getByText('Lounge Premium')).toBeDefined();
    expect(screen.getByText('Esgotado')).toBeDefined();
  });

  it('comunica SALES_CLOSED claramente quando o evento já iniciou e bloqueia reserva', async () => {
    const closedEvent: PublicEventResponse = {
      ...mockPublishedEvent,
      id: 'ev-closed-1',
      title: 'Festival Já Iniciado',
      startsAt: '2026-08-10T10:00:00Z', // data no passado
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

    // Painel de compra não exibe stepper nem botão de reservar
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

    // Setor Camarote VIP tem estoque = 4
    const vipSectorBtn = await screen.findByRole('button', { name: /Selecionar setor Camarote VIP/ });
    await user.click(vipSectorBtn);

    // Resumo atualiza para Camarote VIP
    expect(screen.getAllByText('Camarote VIP').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('1');
    expect(screen.getAllByText(/450,00/).length).toBeGreaterThanOrEqual(1);

    // Incrementa até 4
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

    // Botão + desabilita no limite do estoque (4)
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

    // Incrementa para 2 ingressos
    const incBtn = await screen.findByRole('button', { name: 'Aumentar quantidade' });
    await user.click(incBtn);

    // Clica em "Reservar Ingressos"
    const reserveBtn = screen.getByRole('button', { name: /Reservar 2 ingressos no setor Pista Comum/ });
    await user.click(reserveBtn);

    // Intenção salva em sessionStorage
    const savedIntention = getPurchaseIntention();
    expect(savedIntention).not.toBeNull();
    expect(savedIntention?.eventId).toBe('ev-pub-123');
    expect(savedIntention?.ticketSectorId).toBe('sec-pista');
    expect(savedIntention?.quantity).toBe(2);
    expect(savedIntention?.internalReturnPath).toBe('/events/ev-pub-123');

    // Redireciona para o login com mensagem explicativa
    expect(handleLoginRedirect).toHaveBeenCalledTimes(1);
    expect(handleLoginRedirect).toHaveBeenCalledWith(
      expect.stringContaining('acesse sua conta de Cliente (CUSTOMER)'),
    );
  });

  it('cliente autenticado (CUSTOMER) forma intenção sem antecipar Reservation do Epic 4', async () => {
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(jsonResponse(mockSectors));

    const handleIntention = vi.fn();
    const user = userEvent.setup();

    render(
      <PublicEventDetail
        eventId="ev-pub-123"
        initialEvent={mockPublishedEvent}
        currentUser={customerUser}
        onBackToCatalog={vi.fn()}
        onIntentionFormed={handleIntention}
      />,
    );

    const reserveBtn = await screen.findByRole('button', { name: /Reservar 1 ingresso no setor Pista Comum/ });
    await user.click(reserveBtn);

    // Intenção salva no sessionStorage
    const savedIntention = getPurchaseIntention();
    expect(savedIntention).not.toBeNull();
    expect(savedIntention?.quantity).toBe(1);

    // Feedback acessível de intenção registrada
    expect(await screen.findByTestId('intention-success-alert')).toBeDefined();
    expect(screen.getByTestId('intention-success-alert').textContent).toContain(
      'Intenção de compra registrada com sucesso para o setor Pista Comum (1 ingresso)',
    );

    expect(handleIntention).toHaveBeenCalledWith(
      expect.objectContaining({
        eventId: 'ev-pub-123',
        ticketSectorId: 'sec-pista',
        quantity: 1,
      }),
    );
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
