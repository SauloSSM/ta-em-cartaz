import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { CheckoutSummary } from '../CheckoutSummary';
import type { ReservationResponse } from '../../api/reservationsApi';

const sampleReservation: ReservationResponse = {
  id: 'res-888',
  customerId: 'cust-999',
  eventId: 'evt-111',
  sectorId: 'sec-222',
  quantity: 4,
  unitPrice: 85.5,
  totalAmount: 342.0,
  status: 'HOLDING',
  expiresAt: '2026-08-16T12:10:00.000Z',
  createdAt: '2026-08-16T12:00:00.000Z',
  serverNow: '2026-08-16T12:00:00.000Z',
};

describe('CheckoutSummary component', () => {
  it('renders snapshot details with exact unitPrice, totalAmount, quantity and status', () => {
    render(
      <CheckoutSummary
        reservation={sampleReservation}
        eventTitle="Noite Eletrônica 2026"
        eventVenue="Pavilhão de Exposições"
        sectorName="Pista Premium"
      />,
    );

    expect(screen.getByTestId('checkout-summary')).toBeDefined();
    expect(screen.getByText('Noite Eletrônica 2026')).toBeDefined();
    expect(screen.getByText('Pavilhão de Exposições')).toBeDefined();
    expect(screen.getByText('Pista Premium')).toBeDefined();
    expect(screen.getByText('4 ingressos')).toBeDefined();
    expect(screen.getByText(/85,50/)).toBeDefined();
    expect(screen.getByText(/342,00/)).toBeDefined();
    expect(screen.getByText(/HOLDING/)).toBeDefined();
    expect(screen.getByText(/snapshot autoritativo/)).toBeDefined();
  });
});
