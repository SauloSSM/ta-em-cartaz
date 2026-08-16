import type { ReservationResponse } from '../api/reservationsApi';
import { useReservationTimer } from '../model/useReservationTimer';
import './ActiveReservationBanner.css';

export type ActiveReservationBannerProps = {
  reservation: ReservationResponse;
  eventTitle?: string;
  sectorName?: string;
  onContinue: () => void;
  onExpire?: () => void;
  className?: string;
};

export function ActiveReservationBanner({
  reservation,
  eventTitle,
  sectorName,
  onContinue,
  onExpire,
  className = '',
}: ActiveReservationBannerProps) {
  const { formattedTime, isExpired, state } = useReservationTimer({
    expiresAt: reservation.expiresAt,
    serverNow: reservation.serverNow,
    status: reservation.status,
    onExpire,
  });

  if (isExpired) {
    return null;
  }

  const detailsText = [
    eventTitle,
    sectorName ? `${reservation.quantity}x ${sectorName}` : `${reservation.quantity} ingressos`,
  ]
    .filter(Boolean)
    .join(' • ');

  return (
    <aside
      className={`edt-active-reservation-banner edt-active-reservation-banner--${state} ${className}`.trim()}
      role="status"
      aria-label="Reserva de ingressos em andamento"
      data-testid="active-reservation-banner"
    >
      <div className="edt-active-reservation-banner__content">
        <div className="edt-active-reservation-banner__info">
          <span className="edt-active-reservation-banner__title">
            ⏱ Reserva em andamento
          </span>
          <span className="edt-active-reservation-banner__details">
            {detailsText || 'Você possui ingressos pré-reservados.'}
          </span>
        </div>

        <div className="edt-active-reservation-banner__timer">
          <span>Tempo restante: </span>
          <strong data-testid="banner-countdown">{formattedTime}</strong>
        </div>
      </div>

      <div className="edt-active-reservation-banner__actions">
        <button
          type="button"
          className="edt-button edt-button--primary edt-active-reservation-banner__cta"
          onClick={onContinue}
          data-testid="continue-reservation-btn"
        >
          Continuar reserva →
        </button>
      </div>
    </aside>
  );
}
