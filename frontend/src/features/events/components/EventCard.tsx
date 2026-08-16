import { useState } from 'react';
import type { PublicEventResponse } from '../api/eventsApi';

export type EventCardProps = {
  event: PublicEventResponse;
  onClick?: (event: PublicEventResponse) => void;
};

export function EventCard({ event, onClick }: EventCardProps) {
  const [imageError, setImageError] = useState(false);

  const formattedDate = event.startsAt
    ? formatEventDate(event.startsAt)
    : 'Data a confirmar';

  const formattedPrice = formatCurrency(event.startingPrice);

  const hasImage = Boolean(event.imageUrl) && !imageError;

  return (
    <article
      className="edt-event-card"
      data-testid={`event-card-${event.id}`}
      aria-labelledby={`event-title-${event.id}`}
    >
      <div className="edt-event-card__image-container">
        {hasImage ? (
          <img
            src={event.imageUrl}
            alt={`Banner do evento ${event.title}`}
            className="edt-event-card__image"
            onError={() => setImageError(true)}
            loading="lazy"
          />
        ) : (
          <div className="edt-event-card__image-fallback" role="img" aria-label="Imagem padrão do evento">
            <svg
              className="edt-event-card__fallback-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <rect x="3" y="4" width="18" height="16" rx="2" />
              <path d="M7 8h10M7 12h10M7 16h6" />
              <circle cx="16.5" cy="16.5" r="1.5" />
            </svg>
            <span className="edt-event-card__fallback-text">EliteDevTicket</span>
          </div>
        )}

        <div className="edt-event-card__badges">
          {event.category ? (
            <span className="edt-event-card__category-badge">{event.category}</span>
          ) : null}
          {event.salesClosed ? (
            <span className="edt-event-card__sales-closed-badge" role="status">
              Vendas encerradas
            </span>
          ) : null}
        </div>
      </div>

      <div className="edt-event-card__body">
        <h3 id={`event-title-${event.id}`} className="edt-event-card__title">
          {event.title}
        </h3>

        <div className="edt-event-card__meta">
          <div className="edt-event-card__meta-item" title="Data do evento">
            <svg
              className="edt-event-card__meta-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            <span>{formattedDate}</span>
          </div>

          {(event.venueName || event.venueAddress) && (
            <div className="edt-event-card__meta-item" title="Local do evento">
              <svg
                className="edt-event-card__meta-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              <span>
                {event.venueName}
                {event.venueName && event.venueAddress ? ' — ' : ''}
                {event.venueAddress}
              </span>
            </div>
          )}
        </div>

        <div className="edt-event-card__footer">
          <div className="edt-event-card__price-section">
            <span className="edt-event-card__price-label">A partir de</span>
            <span className="edt-event-card__price-value">{formattedPrice}</span>
          </div>

          {onClick && (
            <button
              type="button"
              className="edt-button edt-button--primary edt-event-card__action-btn"
              onClick={() => onClick(event)}
              aria-label={`Ver detalhes de ${event.title}`}
            >
              Ver detalhes
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

function formatEventDate(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
      return 'Data a confirmar';
    }
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch {
    return 'Data a confirmar';
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(amount);
}
