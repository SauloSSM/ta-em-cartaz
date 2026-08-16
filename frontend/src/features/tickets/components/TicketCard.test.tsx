import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TicketCard } from './TicketCard';
import type { MyTicketResponse } from '../api/ticketsApi';

describe('TicketCard', () => {
  const sampleTicket: MyTicketResponse = {
    id: 't-1111',
    reservationId: 'r-1111',
    eventId: 'e-1111',
    sectorId: 's-1111',
    ordinal: 1,
    status: 'VALID',
    manualCode: 'AB7K92QX4M',
    shareToken: 'st-1111',
    validationToken: 'vt-1111',
    createdAt: '2026-08-16T14:00:00Z',
  };

  it('renders event title, unit ordinal, sector, date, venue, formatted code and status badge', () => {
    const onOpenDetail = vi.fn();
    render(
      <TicketCard
        ticket={sampleTicket}
        eventTitle="Festival de Música 2026"
        eventDate="2026-08-20T20:00:00Z"
        eventVenue="Allianz Parque"
        sectorName="Pista Premium"
        onOpenDetail={onOpenDetail}
      />
    );

    expect(screen.getByText('Festival de Música 2026')).toBeDefined();
    expect(screen.getByText('Ingresso #1')).toBeDefined();
    expect(screen.getByText('Pista Premium')).toBeDefined();
    expect(screen.getByText('Allianz Parque')).toBeDefined();
    expect(screen.getByText('AB7K-92QX-4M')).toBeDefined();
    expect(screen.getByText('Válido')).toBeDefined();
  });

  it('renders USED status badge when ticket status is USED', () => {
    const usedTicket: MyTicketResponse = {
      ...sampleTicket,
      status: 'USED',
    };
    render(
      <TicketCard
        ticket={usedTicket}
        eventTitle="Show Rock"
        onOpenDetail={vi.fn()}
      />
    );

    expect(screen.getByText('Utilizado')).toBeDefined();
  });

  it('calls onOpenDetail when action button is clicked', async () => {
    const user = userEvent.setup();
    const onOpenDetail = vi.fn();
    render(
      <TicketCard
        ticket={sampleTicket}
        eventTitle="Show Rock"
        onOpenDetail={onOpenDetail}
      />
    );

    const btn = screen.getByTestId('open-ticket-btn-t-1111');
    await user.click(btn);

    expect(onOpenDetail).toHaveBeenCalledWith(sampleTicket);
  });
});
