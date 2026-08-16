import type { ReservationResponse } from '../api/reservationsApi';
import { useReservationTimer } from '../model/useReservationTimer';

export type ActiveHoldCardProps = {
  reservation: ReservationResponse;
  sectorName?: string;
  eventTitle?: string;
  onNavigateCheckout?: () => void;
  onExpire?: () => void;
};

export function ActiveHoldCard({
  reservation,
  sectorName,
  eventTitle,
  onNavigateCheckout,
  onExpire,
}: ActiveHoldCardProps) {
  const {
    formattedTime,
    isExpired,
    announcement,
  } = useReservationTimer({
    expiresAt: reservation.expiresAt,
    serverNow: reservation.serverNow,
    status: reservation.status,
    onExpire,
  });

  return (
    <div
      className={`edt-alert ${isExpired ? 'edt-alert--danger' : 'edt-alert--success'}`}
      role={isExpired ? 'alert' : 'status'}
      aria-atomic="true"
      data-testid="active-hold-card"
    >
      <div className="edt-active-hold__header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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
            <dl
              className="edt-hold-details"
              style={{
                marginTop: '0.75rem',
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                gap: '0.5rem',
              }}
            >
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

            {onNavigateCheckout && (
              <div style={{ marginTop: '1rem' }}>
                <button
                  type="button"
                  className="edt-button edt-button--primary"
                  onClick={onNavigateCheckout}
                  data-testid="go-to-checkout-btn"
                >
                  Concluir no Checkout →
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      <div
        className="edt-visually-hidden"
        role="status"
        aria-live="polite"
        data-testid="hold-card-announcement"
      >
        {announcement}
      </div>
    </div>
  );
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}
