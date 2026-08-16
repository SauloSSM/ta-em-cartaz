import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useReservationTimer } from '../useReservationTimer';

describe('useReservationTimer hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('calculates initial remaining time using serverNow and expiresAt', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:10:00.000Z'; // 600 segundos (10 minutos)

    const { result } = renderHook(() =>
      useReservationTimer({
        serverNow,
        expiresAt,
        status: 'HOLDING',
      }),
    );

    expect(result.current.remainingSeconds).toBe(600);
    expect(result.current.formattedTime).toBe('10:00');
    expect(result.current.state).toBe('normal');
    expect(result.current.isExpired).toBe(false);
  });

  it('progresses monotonically and transitions through normal, warning, critical, and expired states', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:10:00.000Z'; // 600s

    const { result } = renderHook(() =>
      useReservationTimer({
        serverNow,
        expiresAt,
        status: 'HOLDING',
      }),
    );

    // Initial state: normal (> 179s)
    expect(result.current.state).toBe('normal');
    expect(result.current.stateLabel).toBe('Tempo normal');

    // Avança 7 minutos e 1 segundo (421s decorridos => restam 179s => 02:59)
    act(() => {
      vi.advanceTimersByTime(421 * 1000);
    });

    expect(result.current.remainingSeconds).toBe(179);
    expect(result.current.formattedTime).toBe('02:59');
    expect(result.current.state).toBe('warning');
    expect(result.current.stateLabel).toBe('Tempo acabando');

    // Avança mais 2 minutos (120s decorridos => restam 59s => 00:59)
    act(() => {
      vi.advanceTimersByTime(120 * 1000);
    });

    expect(result.current.remainingSeconds).toBe(59);
    expect(result.current.formattedTime).toBe('00:59');
    expect(result.current.state).toBe('critical');
    expect(result.current.stateLabel).toBe('Tempo crítico');

    // Avança mais 60 segundos => expira
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });

    expect(result.current.remainingSeconds).toBe(0);
    expect(result.current.formattedTime).toBe('00:00');
    expect(result.current.state).toBe('expired');
    expect(result.current.stateLabel).toBe('Reserva expirada');
    expect(result.current.isExpired).toBe(true);
  });

  it('triggers ARIA announcements ONLY at canonical milestones (3 min, 1 min, expiration)', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:05:00.000Z'; // 300s (5 minutos)

    const { result } = renderHook(() =>
      useReservationTimer({
        serverNow,
        expiresAt,
        status: 'HOLDING',
      }),
    );

    // Inicialmente sem anúncio
    expect(result.current.announcement).toBeNull();

    // Avança 60s (restam 240s => ainda > 180s)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(result.current.announcement).toBeNull();

    // Avança mais 60s (restam 180s => marco de 3 minutos!)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(result.current.announcement).toBe('Restam 3 minutos para concluir sua reserva.');

    // Avança 30s (restam 150s => não deve anunciar novamente no segundo a segundo)
    act(() => {
      vi.advanceTimersByTime(30 * 1000);
    });
    expect(result.current.announcement).toBe('Restam 3 minutos para concluir sua reserva.');

    // Avança 90s (restam 60s => marco de 1 minuto!)
    act(() => {
      vi.advanceTimersByTime(90 * 1000);
    });
    expect(result.current.announcement).toBe('Atenção: resta 1 minuto para concluir sua reserva.');

    // Avança 60s (restam 0s => marco de expiração!)
    act(() => {
      vi.advanceTimersByTime(60 * 1000);
    });
    expect(result.current.announcement).toBe('O tempo da sua reserva expirou. Seus ingressos foram liberados.');
  });

  it('calls onExpire callback and onReconcile when countdown reaches zero', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:00:05.000Z'; // 5s
    const onExpire = vi.fn();
    const onReconcile = vi.fn();

    renderHook(() =>
      useReservationTimer({
        serverNow,
        expiresAt,
        status: 'HOLDING',
        onExpire,
        onReconcile,
      }),
    );

    act(() => {
      vi.advanceTimersByTime(6 * 1000);
    });

    expect(onExpire).toHaveBeenCalledTimes(1);
    expect(onReconcile).toHaveBeenCalled();
  });

  it('reconciles when returning to the tab (visibilitychange)', () => {
    const serverNow = '2026-08-16T12:00:00.000Z';
    const expiresAt = '2026-08-16T12:10:00.000Z';
    const onReconcile = vi.fn();

    renderHook(() =>
      useReservationTimer({
        serverNow,
        expiresAt,
        status: 'HOLDING',
        onReconcile,
      }),
    );

    expect(onReconcile).not.toHaveBeenCalled();

    // Simula evento visibilitychange para visible
    act(() => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        writable: true,
        configurable: true,
      });
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(onReconcile).toHaveBeenCalledTimes(1);
  });
});
