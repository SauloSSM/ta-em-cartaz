import type { MyTicketResponse } from '../api/ticketsApi';
import { formatManualCode } from '../lib/qrCode';

export type TicketCardProps = {
  ticket: MyTicketResponse;
  eventTitle?: string;
  eventDate?: string;
  eventVenue?: string;
  sectorName?: string;
  onOpenDetail: (ticket: MyTicketResponse) => void;
};

export function TicketCard({
  ticket,
  eventTitle,
  eventDate,
  eventVenue,
  sectorName,
  onOpenDetail,
}: TicketCardProps) {
  const isUsed = ticket.status === 'USED';
  const statusLabel = isUsed ? 'Utilizado' : 'Válido';
  const formattedCode = formatManualCode(ticket.manualCode);

  return (
    <article
      className={`edt-ticket-card ${isUsed ? 'edt-ticket-card--used' : 'edt-ticket-card--valid'}`}
      data-testid={`ticket-card-${ticket.id}`}
      aria-labelledby={`ticket-title-${ticket.id}`}
    >
      <div className="edt-ticket-card__header">
        <div className="edt-ticket-card__title-group">
          <span className="edt-ticket-card__unit">Ingresso #{ticket.ordinal}</span>
          <h3 id={`ticket-title-${ticket.id}`} className="edt-ticket-card__title">
            {eventTitle || 'Show / Evento'}
          </h3>
        </div>
        <div className="edt-ticket-card__badge-wrapper">
          <span
            className={`edt-status-badge ${
              isUsed ? 'edt-status-badge--used' : 'edt-status-badge--valid'
            }`}
            data-testid={`ticket-status-${ticket.id}`}
          >
            {statusLabel}
          </span>
        </div>
      </div>

      <div className="edt-ticket-card__details">
        {sectorName && (
          <div className="edt-ticket-card__meta-item">
            <span className="edt-ticket-card__meta-label">Setor:</span>
            <span className="edt-ticket-card__meta-value">{sectorName}</span>
          </div>
        )}
        {eventDate && (
          <div className="edt-ticket-card__meta-item">
            <span className="edt-ticket-card__meta-label">Data:</span>
            <span className="edt-ticket-card__meta-value">
              {new Date(eventDate).toLocaleDateString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          </div>
        )}
        {eventVenue && (
          <div className="edt-ticket-card__meta-item">
            <span className="edt-ticket-card__meta-label">Local:</span>
            <span className="edt-ticket-card__meta-value">{eventVenue}</span>
          </div>
        )}
        <div className="edt-ticket-card__meta-item">
          <span className="edt-ticket-card__meta-label">Código:</span>
          <code className="edt-ticket-card__code-preview">{formattedCode}</code>
        </div>
      </div>

      <div className="edt-ticket-card__actions">
        <button
          type="button"
          className="edt-button edt-button--primary edt-button--full-width"
          onClick={() => onOpenDetail(ticket)}
          data-testid={`open-ticket-btn-${ticket.id}`}
          aria-label={`Ver detalhes do ingresso #${ticket.ordinal} de ${eventTitle || 'evento'}`}
        >
          Ver Ingresso (QR & Código) →
        </button>
      </div>
    </article>
  );
}
