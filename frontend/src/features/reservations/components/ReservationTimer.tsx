import type { ReservationStatus, ReservationResponse } from '../api/reservationsApi';
import { useReservationTimer, type ReservationTimerState } from '../model/useReservationTimer';
import './ReservationTimer.css';

export type ReservationTimerProps = {
  expiresAt: string;
  serverNow: string;
  status?: ReservationStatus;
  onExpire?: () => void;
  onReconcile?: () => Promise<ReservationResponse | null | void> | void;
  className?: string;
};

export function ReservationTimer({
  expiresAt,
  serverNow,
  status = 'HOLDING',
  onExpire,
  onReconcile,
  className = '',
}: ReservationTimerProps) {
  const {
    formattedTime,
    state,
    stateLabel,
    isExpired,
    announcement,
  } = useReservationTimer({
    expiresAt,
    serverNow,
    status,
    onExpire,
    onReconcile,
  });

  const timerClass = `edt-reservation-timer edt-reservation-timer--${state} ${className}`.trim();

  return (
    <div
      className={timerClass}
      role={isExpired ? 'alert' : 'status'}
      aria-atomic="true"
      data-testid="reservation-timer"
    >
      <div className="edt-reservation-timer__header">
        <div className="edt-reservation-timer__icon" aria-hidden="true">
          {getStateIcon(state)}
        </div>
        <div className="edt-reservation-timer__info">
          <span className="edt-reservation-timer__title">
            {isExpired ? 'Tempo de Reserva Esgotado' : 'Tempo Restante para Concluir'}
          </span>
          <span className={`edt-reservation-timer__badge edt-reservation-timer__badge--${state}`}>
            {stateLabel}
          </span>
        </div>
      </div>

      <div className="edt-reservation-timer__display">
        <span
          className="edt-reservation-timer__countdown"
          data-testid="timer-countdown"
          aria-label={isExpired ? 'Reserva expirada' : `Tempo restante: ${formattedTime}`}
        >
          {formattedTime}
        </span>
      </div>

      {/* Região ao vivo dedicada para anúncios acessíveis nos marcos canônicos (3min, 1min, expiração) */}
      <div
        className="edt-visually-hidden"
        role="status"
        aria-live="polite"
        data-testid="timer-announcement"
      >
        {announcement}
      </div>
    </div>
  );
}

function getStateIcon(state: ReservationTimerState): string {
  switch (state) {
    case 'normal':
      return '⏱';
    case 'warning':
      return '⚠️';
    case 'critical':
      return '🚨';
    case 'expired':
      return '⛔';
  }
}
