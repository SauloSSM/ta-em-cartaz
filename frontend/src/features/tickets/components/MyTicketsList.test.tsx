import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyTicketsList } from './MyTicketsList';
import * as ticketsApi from '../api/ticketsApi';
import * as eventsApi from '../../events/api/eventsApi';

describe('MyTicketsList', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially', () => {
    vi.spyOn(ticketsApi, 'listMyTickets').mockReturnValue(new Promise(() => {}));

    render(<MyTicketsList onSelectTicket={vi.fn()} />);

    expect(screen.getByTestId('my-tickets-loading')).toBeDefined();
    expect(screen.getByText(/Carregando seus ingressos/i)).toBeDefined();
  });

  it('renders empty state when customer has no tickets', async () => {
    vi.spyOn(ticketsApi, 'listMyTickets').mockResolvedValue({ tickets: [] });

    render(<MyTicketsList onSelectTicket={vi.fn()} onBrowseCatalog={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('my-tickets-empty')).toBeDefined();
    });
    expect(screen.getByText(/Nenhum ingresso ainda/i)).toBeDefined();
    expect(screen.getByTestId('browse-catalog-btn')).toBeDefined();
  });

  it('renders error state when fetch fails and allows retry', async () => {
    const user = userEvent.setup();
    const listSpy = vi.spyOn(ticketsApi, 'listMyTickets')
      .mockRejectedValueOnce(new ticketsApi.TicketClientError('TICKET_INVALID_RESPONSE', 'Falha na rede'))
      .mockResolvedValueOnce({ tickets: [] });

    render(<MyTicketsList onSelectTicket={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('my-tickets-error')).toBeDefined();
    });
    expect(screen.getByText('Falha na rede')).toBeDefined();

    const retryBtn = screen.getByTestId('my-tickets-retry-btn');
    await user.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByTestId('my-tickets-empty')).toBeDefined();
    });
    expect(listSpy).toHaveBeenCalledTimes(2);
  });

  it('renders list of ticket cards when tickets are loaded and triggers onSelectTicket', async () => {
    const user = userEvent.setup();
    const mockTicket: ticketsApi.MyTicketResponse = {
      id: 't-123',
      reservationId: 'r-123',
      eventId: 'e-123',
      sectorId: 's-123',
      ordinal: 1,
      status: 'VALID',
      manualCode: 'AB7K92QX4M',
      shareToken: 'st-123',
      validationToken: 'vt-123',
      createdAt: '2026-08-16T14:00:00Z',
    };

    vi.spyOn(ticketsApi, 'listMyTickets').mockResolvedValue({ tickets: [mockTicket] });
    vi.spyOn(eventsApi, 'listPublicEvents').mockResolvedValue({
      events: [
        {
          id: 'e-123',
          title: 'Festival da Primavera',
          startsAt: '2026-09-01T18:00:00Z',
          venueName: 'Parque Ibirapuera',
          startingPrice: 100,
          salesClosed: false,
          status: 'PUBLISHED',
          createdAt: '2026-08-01T00:00:00Z',
          updatedAt: '2026-08-01T00:00:00Z',
        },
      ],
    });
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({
      sectors: [
        {
          id: 's-123',
          eventId: 'e-123',
          name: 'Pista',
          capacity: 50,
          availableQuantity: 40,
          price: 100,
          createdAt: '2026-08-01T00:00:00Z',
          updatedAt: '2026-08-01T00:00:00Z',
        },
      ],
    });

    const onSelectTicket = vi.fn();
    render(<MyTicketsList onSelectTicket={onSelectTicket} />);

    await waitFor(() => {
      expect(screen.getByTestId('my-tickets-grid')).toBeDefined();
    });

    expect(screen.getByText('Festival da Primavera')).toBeDefined();
    expect(screen.getByText('Ingresso #1')).toBeDefined();

    const openBtn = screen.getByTestId('open-ticket-btn-t-123');
    await user.click(openBtn);

    expect(onSelectTicket).toHaveBeenCalledWith(mockTicket, expect.objectContaining({
      event: expect.objectContaining({ title: 'Festival da Primavera' }),
      sectorName: 'Pista',
    }));
  });
});
