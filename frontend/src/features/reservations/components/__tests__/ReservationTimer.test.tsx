import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ReservationTimer } from '../ReservationTimer';

describe('ReservationTimer component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders countdown and normal state badge derived from serverNow and expiresAt', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:10:00.000Z'; // 10 min

    render(
      <ReservationTimer
        serverNow={serverNow}
        expiresAt={expiresAt}
        status="HOLDING"
      />,
    );

    const timer = screen.getByTestId('reservation-timer');
    expect(timer).toBeDefined();
    expect(timer.className).toContain('edt-reservation-timer--normal');

    const countdown = screen.getByTestId('timer-countdown');
    expect(countdown.textContent).toBe('10:00');
    expect(screen.getByText('Tempo normal')).toBeDefined();
  });

  it('transitions visually and textually through warning, critical, and expired without only color', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:10:00.000Z';

    const { container } = render(
      <ReservationTimer
        serverNow={serverNow}
        expiresAt={expiresAt}
        status="HOLDING"
      />,
    );

    // 1. Normal state
    expect(screen.getByText('Tempo normal')).toBeDefined();

    // 2. Warning state (advance to remaining 2:30 = 150s)
    act(() => {
      vi.advanceTimersByTime(450 * 1000);
    });

    expect(screen.getByText('02:30')).toBeDefined();
    expect(screen.getByText('Tempo acabando')).toBeDefined();
    expect(container.querySelector('.edt-reservation-timer--warning')).not.toBeNull();

    // 3. Critical state (advance to remaining 0:45 = 45s)
    act(() => {
      vi.advanceTimersByTime(105 * 1000);
    });

    expect(screen.getByText('00:45')).toBeDefined();
    expect(screen.getByText('Tempo crítico')).toBeDefined();
    expect(container.querySelector('.edt-reservation-timer--critical')).not.toBeNull();

    // 4. Expired state (advance to remaining 0s)
    act(() => {
      vi.advanceTimersByTime(50 * 1000);
    });

    expect(screen.getByText('00:00')).toBeDefined();
    expect(screen.getByText('Reserva expirada')).toBeDefined();
    expect(container.querySelector('.edt-reservation-timer--expired')).not.toBeNull();
  });

  it('announces milestones in ARIA live region only at 3 min, 1 min, and expiration', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:05:00.000Z'; // 5 minutos (300s)

    render(
      <ReservationTimer
        serverNow={serverNow}
        expiresAt={expiresAt}
        status="HOLDING"
      />,
    );

    const announcementEl = screen.getByTestId('timer-announcement');
    expect(announcementEl.textContent).toBe('');

    // Advance 60s -> 240s remaining (no announcement yet)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(announcementEl.textContent).toBe('');

    // Advance 60s -> 180s remaining (3 minutes milestone)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(announcementEl.textContent).toBe('Restam 3 minutos para concluir sua reserva.');

    // Advance 120s -> 60s remaining (1 minute milestone)
    act(() => {
      vi.advanceTimersByTime(120 * 1000);
    });
    expect(announcementEl.textContent).toBe('Atenção: resta 1 minuto para concluir sua reserva.');

    // Advance 60s -> 0s remaining (expiration milestone)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(announcementEl.textContent).toBe('O tempo da sua reserva expirou. Seus ingressos foram liberados.');
  });
});
