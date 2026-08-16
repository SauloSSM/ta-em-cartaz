import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TicketDetail } from './TicketDetail';
import type { MyTicketResponse } from '../api/ticketsApi';

describe('TicketDetail', () => {
  const sampleTicket: MyTicketResponse = {
    id: 't-1111',
    reservationId: 'r-1111',
    eventId: 'e-1111',
    sectorId: 's-1111',
    ordinal: 1,
    status: 'VALID',
    manualCode: 'AB7K92QX4M',
    shareToken: 'share-token-xyz-12345678901234567890',
    validationToken: 'val-token-xyz-12345678901234567890',
    createdAt: '2026-08-16T14:00:00Z',
  };

  it('renders ticket info, VALID status badge, QRCodePanel and share button', () => {
    render(
      <TicketDetail
        ticket={sampleTicket}
        eventTitle="Super Show 2026"
        eventDate="2026-08-25T21:00:00Z"
        eventVenue="Estádio Morumbi"
        sectorName="Camarote"
        onBackToList={vi.fn()}
      />
    );

    expect(screen.getByTestId('ticket-detail-title').textContent).toBe('Super Show 2026');
    expect(screen.getByText(/Camarote/i)).toBeDefined();
    expect(screen.getByText('Ingresso #1')).toBeDefined();
    expect(screen.getByTestId('ticket-detail-status-badge').textContent).toBe('Válido');
    expect(screen.queryByTestId('ticket-used-notice')).toBeNull();
    expect(screen.getByTestId('qrcode-panel')).toBeDefined();
    expect(screen.getByTestId('share-ticket-btn')).toBeDefined();
  });

  it('AC: when ticket is USED, textual state notice precedes credentials in reading order', () => {
    const usedTicket: MyTicketResponse = {
      ...sampleTicket,
      status: 'USED',
    };

    render(
      <TicketDetail
        ticket={usedTicket}
        eventTitle="Super Show 2026"
        onBackToList={vi.fn()}
      />
    );

    const usedNotice = screen.getByTestId('ticket-used-notice');
    expect(usedNotice).toBeDefined();
    expect(usedNotice.textContent).toMatch(/Ingresso Utilizado/i);
    expect(usedNotice.textContent).toMatch(/não autoriza nova entrada/i);

    const qrPanel = screen.getByTestId('qrcode-panel');
    // Ensure notice appears before the QR/credentials panel in DOM tree
    const position = usedNotice.compareDocumentPosition(qrPanel);
    expect(position & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('copies share link when share button is clicked', async () => {
    const user = userEvent.setup();
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: writeTextMock },
      writable: true,
      configurable: true,
    });

    render(
      <TicketDetail
        ticket={sampleTicket}
        eventTitle="Super Show 2026"
        onBackToList={vi.fn()}
      />
    );

    const shareBtn = screen.getByTestId('share-ticket-btn');
    await user.click(shareBtn);

    expect(writeTextMock).toHaveBeenCalledWith(
      expect.stringContaining(`/t/${sampleTicket.shareToken}`)
    );
    expect(screen.getAllByText(/Link permanente de compartilhamento copiado/i).length).toBeGreaterThanOrEqual(1);
  });

  it('calls onBackToList when back button is clicked', async () => {
    const user = userEvent.setup();
    const onBackToList = vi.fn();
    render(
      <TicketDetail
        ticket={sampleTicket}
        eventTitle="Super Show 2026"
        onBackToList={onBackToList}
      />
    );

    const backBtn = screen.getByTestId('back-to-tickets-btn');
    await user.click(backBtn);

    expect(onBackToList).toHaveBeenCalledTimes(1);
  });
});
