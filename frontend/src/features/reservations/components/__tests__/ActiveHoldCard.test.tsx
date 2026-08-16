import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { ReservationResponse } from '../../api/reservationsApi';
import { ActiveHoldCard } from '../ActiveHoldCard';

const mockReservation: ReservationResponse = {
  id: 'res-123',
  customerId: 'cust-1',
  eventId: 'ev-1',
  sectorId: 'sec-1',
  quantity: 3,
  unitPrice: 120.0,
  totalAmount: 360.0,
  status: 'HOLDING',
  expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
  createdAt: new Date().toISOString(),
  serverNow: new Date().toISOString(),
};

describe('ActiveHoldCard component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders active hold details with 10-minute countdown timer', () => {
    render(
      <ActiveHoldCard
        reservation={mockReservation}
        sectorName="Pista VIP"
        eventTitle="Festival Primavera 2026"
      />,
    );

    expect(screen.getByTestId('active-hold-card')).toBeDefined();
    expect(screen.getByText('Ingressos Pré-Reservados (Hold)')).toBeDefined();
    expect(screen.getByTestId('hold-countdown').textContent).toMatch(/Tempo restante: (09:59|10:00)/);
    expect(screen.getByText('Festival Primavera 2026')).toBeDefined();
    expect(screen.getByText('Pista VIP')).toBeDefined();
    expect(screen.getByText('3 ingressos')).toBeDefined();
    expect(screen.getByText(/360,00/)).toBeDefined();
  });

  it('updates countdown and switches to expired state when time reaches zero', () => {
    render(
      <ActiveHoldCard
        reservation={mockReservation}
        sectorName="Pista VIP"
      />,
    );

    expect(screen.getByText('Ingressos Pré-Reservados (Hold)')).toBeDefined();

    // Avança 11 minutos
    act(() => {
      vi.advanceTimersByTime(11 * 60 * 1000);
    });

    expect(screen.getByText('Reserva Expirada')).toBeDefined();
    expect(screen.getByText('Expirado')).toBeDefined();
    expect(screen.getByText(/O tempo de 10 minutos para garantia dos seus ingressos expirou/)).toBeDefined();
  });
});
