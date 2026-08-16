import type { ReservationResponse } from '../api/reservationsApi';
import './CheckoutSummary.css';

export type CheckoutSummaryProps = {
  reservation: ReservationResponse;
  eventTitle?: string;
  eventDate?: string;
  eventVenue?: string;
  sectorName?: string;
};

export function CheckoutSummary({
  reservation,
  eventTitle,
  eventDate,
  eventVenue,
  sectorName,
}: CheckoutSummaryProps) {
  const formattedUnitPrice = formatCurrency(reservation.unitPrice);
  const formattedTotalAmount = formatCurrency(reservation.totalAmount);

  return (
    <section
      className="edt-checkout-summary"
      aria-labelledby="checkout-summary-title"
      data-testid="checkout-summary"
    >
      <h3 id="checkout-summary-title" className="edt-checkout-summary__title">
        Resumo do Pedido (Snapshot do Hold)
      </h3>

      <div className="edt-checkout-summary__card">
        {/* Contexto do Evento */}
        {(eventTitle || eventDate || eventVenue) && (
          <div className="edt-checkout-summary__event-info">
            {eventTitle && <h4 className="edt-checkout-summary__event-title">{eventTitle}</h4>}
            {eventDate && <p className="edt-checkout-summary__event-meta">{formatEventDate(eventDate)}</p>}
            {eventVenue && <p className="edt-checkout-summary__event-meta">{eventVenue}</p>}
          </div>
        )}

        <dl className="edt-checkout-summary__list">
          <div className="edt-checkout-summary__row">
            <dt>Setor reservado:</dt>
            <dd>
              <strong>{sectorName || 'Setor padrão'}</strong>
            </dd>
          </div>

          <div className="edt-checkout-summary__row">
            <dt>Quantidade:</dt>
            <dd>
              <span>{reservation.quantity} {reservation.quantity === 1 ? 'ingresso' : 'ingressos'}</span>
            </dd>
          </div>

          <div className="edt-checkout-summary__row">
            <dt>Preço unitário (snapshot):</dt>
            <dd>
              <span>{formattedUnitPrice}</span>
            </dd>
          </div>

          <div className="edt-checkout-summary__row edt-checkout-summary__row--status">
            <dt>Status da reserva:</dt>
            <dd>
              <span className={`edt-badge edt-badge--${reservation.status.toLowerCase()}`}>
                {mapStatusLabel(reservation.status)}
              </span>
            </dd>
          </div>

          <div className="edt-checkout-summary__row edt-checkout-summary__row--total">
            <dt>Valor total garantido:</dt>
            <dd>
              <strong className="edt-checkout-summary__total-value">{formattedTotalAmount}</strong>
            </dd>
          </div>
        </dl>

        <p className="edt-checkout-summary__disclaimer">
          Os valores e quantidades acima foram fixados pelo servidor no momento da reserva (snapshot autoritativo) e não serão recalculados durante o hold.
        </p>
      </div>
    </section>
  );
}

function mapStatusLabel(status: string): string {
  switch (status) {
    case 'HOLDING':
      return 'Reserva Ativa (HOLDING)';
    case 'CONFIRMED':
      return 'Confirmada (CONFIRMED)';
    case 'EXPIRED':
      return 'Expirada (EXPIRED)';
    default:
      return status;
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

function formatEventDate(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
      return isoString;
    }
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'America/Sao_Paulo',
    }).format(date);
  } catch {
    return isoString;
  }
}
