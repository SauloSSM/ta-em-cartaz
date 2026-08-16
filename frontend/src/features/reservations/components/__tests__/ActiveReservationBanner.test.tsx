import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ActiveReservationBanner } from '../ActiveReservationBanner';
import type { ReservationResponse } from '../../api/reservationsApi';

const sampleReservation: ReservationResponse = {
  id: 'res-banner',
  customerId: 'cust-1',
  eventId: 'evt-1',
  sectorId: 'sec-1',
  quantity: 2,
  unitPrice: 100.0,
  totalAmount: 200.0,
  status: 'HOLDING',
  expiresAt: '2026-08-16T12:10:00.000Z',
  createdAt: '2026-08-16T12:00:00.000Z',
  serverNow: '2026-08-16T12:00:00.000Z',
};

describe('ActiveReservationBanner component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders active reservation info with timer and continues to checkout on click', () => {
    const onContinue = vi.fn();

    render(
      <ActiveReservationBanner
        reservation={sampleReservation}
        eventTitle="Festival de Verão"
        sectorName="Área VIP"
        onContinue={onContinue}
      />,
    );

    expect(screen.getByTestId('active-reservation-banner')).toBeDefined();
    expect(screen.getByText(/Festival de Verão/)).toBeDefined();
    expect(screen.getByText(/2x Área VIP/)).toBeDefined();
    expect(screen.getByTestId('banner-countdown').textContent).toBe('10:00');

    const button = screen.getByTestId('continue-reservation-btn');
    fireEvent.click(button);

    expect(onContinue).toHaveBeenCalledTimes(1);
  });

  it('hides the banner when reservation is expired', () => {
    const onContinue = vi.fn();

    const { container } = render(
      <ActiveReservationBanner
        reservation={{
          ...sampleReservation,
          status: 'EXPIRED',
        }}
        eventTitle="Festival de Verão"
        onContinue={onContinue}
      />,
    );

    expect(container.firstChild).toBeNull();
  });
});
