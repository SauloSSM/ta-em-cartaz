import { useState, useEffect } from 'react';
import type { ReservationResponse } from '../api/reservationsApi';

export type ActiveHoldCardProps = {
  reservation: ReservationResponse;
  sectorName?: string;
  eventTitle?: string;
};

export function ActiveHoldCard({ reservation, sectorName, eventTitle }: ActiveHoldCardProps) {
  const [remainingSeconds, setRemainingSeconds] = useState<number>(() =>
    calculateRemainingSeconds(reservation.expiresAt),
  );

  useEffect(() => {
    const timer = setInterval(() => {
      const remaining = calculateRemainingSeconds(reservation.expiresAt);
      setRemainingSeconds(remaining);
    }, 1000);
    return () => clearInterval(timer);
  }, [reservation.expiresAt]);

  const isExpired = remainingSeconds <= 0;
  const minutes = Math.floor(Math.max(0, remainingSeconds) / 60);
  const seconds = Math.max(0, remainingSeconds) % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  return (
    <div
      className={`edt-alert ${isExpired ? 'edt-alert--danger' : 'edt-alert--success'}`}
      role={isExpired ? 'alert' : 'status'}
      aria-live="polite"
      data-testid="active-hold-card"
    >
      <div className="edt-active-hold__header">
        <h3 className="edt-alert__title">
          {isExpired ? 'Reserva Expirada' : 'Ingressos Pré-Reservados (Hold)'}
        </h3>
        <span
          className={`edt-badge ${isExpired ? 'edt-badge--danger' : 'edt-badge--success'}`}
          data-testid="hold-countdown"
        >
          {isExpired ? 'Expirado' : `Tempo restante: ${formattedTime}`}
        </span>
      </div>

      <div className="edt-alert__desc">
        {isExpired ? (
          <p>
            O tempo de 10 minutos para garantia dos seus ingressos expirou. Seus ingressos foram devolvidos ao estoque.
            Por favor, selecione novamente para iniciar uma nova reserva.
          </p>
        ) : (
          <div>
            <p>
              Seus ingressos estão garantidos temporariamente por 10 minutos durante o processo de compra.
            </p>
            <dl className="edt-hold-details" style={{ marginTop: '0.75rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '0.5rem' }}>
              {eventTitle && (
                <div>
                  <dt><strong>Evento:</strong></dt>
                  <dd>{eventTitle}</dd>
                </div>
              )}
              {sectorName && (
                <div>
                  <dt><strong>Setor:</strong></dt>
                  <dd>{sectorName}</dd>
                </div>
              )}
              <div>
                <dt><strong>Quantidade:</strong></dt>
                <dd>{reservation.quantity} {reservation.quantity === 1 ? 'ingresso' : 'ingressos'}</dd>
              </div>
              <div>
                <dt><strong>Preço Unitário:</strong></dt>
                <dd>{formatCurrency(reservation.unitPrice)}</dd>
              </div>
              <div>
                <dt><strong>Valor Total:</strong></dt>
                <dd><strong>{formatCurrency(reservation.totalAmount)}</strong></dd>
              </div>
            </dl>
          </div>
        )}
      </div>
    </div>
  );
}

function calculateRemainingSeconds(expiresAtIso: string): number {
  try {
    const expiresMs = new Date(expiresAtIso).getTime();
    if (Number.isNaN(expiresMs)) return 0;
    const diffMs = expiresMs - Date.now();
    return Math.max(0, Math.floor(diffMs / 1000));
  } catch {
    return 0;
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}
