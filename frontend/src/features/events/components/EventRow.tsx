import { getEventTheme } from '../../../shared/components/EventArtwork/BrandedImageFallback';
import type { PublicEventResponse } from '../api/eventsApi';
import './EventRow.css';

export type EventRowProps = {
  event: PublicEventResponse;
  onClick?: (event: PublicEventResponse) => void;
};

export function EventRow({ event, onClick }: EventRowProps) {
  const theme = getEventTheme(event.id);
  const { day, month, weekdayTime } = parseEventDate(event.startsAt);
  const formattedPrice = formatCurrency(event.startingPrice);

  return (
    <article
      className="tc-event-row"
      data-testid={`event-row-${event.id}`}
      aria-labelledby={`event-row-title-${event.id}`}
    >
      {/* Date block */}
      <div className="tc-event-row__date-block">
        <div className={`tc-event-row__date-badge tc-event-row__date-badge--${theme}`}>
          <span className="tc-event-row__date-day">{day}</span>
          <span className="tc-event-row__date-month">{month}</span>
        </div>
        {weekdayTime && (
          <span className="tc-event-row__weekday-time">{weekdayTime}</span>
        )}
      </div>

      {/* Title and Category */}
      <div className="tc-event-row__info">
        <h3 id={`event-row-title-${event.id}`} className="tc-event-row__title">
          {event.title}
        </h3>
        <div className="tc-event-row__tags">
          {event.category ? (
            <span className={`tc-event-row__category tc-event-row__category--${theme}`}>{event.category}</span>
          ) : null}
          {event.salesClosed ? (
            <span className="tc-event-row__status-badge" role="status">
              Vendas encerradas
            </span>
          ) : null}
        </div>
      </div>

      {/* Venue & Location */}
      <div className="tc-event-row__venue">
        {event.venueAddress ? (
          <div>{event.venueAddress}</div>
        ) : null}
        {event.venueName ? (
          <div className="tc-event-row__venue-name">{event.venueName}</div>
        ) : null}
      </div>

      {/* Price & Action */}
      <div className="tc-event-row__action-block">
        <div className="tc-event-row__price-box">
          <span className="tc-event-row__price-label">A partir de</span>
          <span className="tc-event-row__price-value">{formattedPrice}</span>
        </div>

        {onClick && (
          <button
            type="button"
            className="tc-event-row__btn"
            onClick={() => onClick(event)}
            aria-label={`Ver detalhes de ${event.title}`}
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              aria-hidden="true"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </button>
        )}
      </div>
    </article>
  );
}

function parseEventDate(isoString?: string | null): {
  day: string;
  month: string;
  weekdayTime: string;
} {
  if (!isoString) {
    return { day: '--', month: 'TBD', weekdayTime: 'Data a confirmar' };
  }
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
      return { day: '--', month: 'TBD', weekdayTime: 'Data a confirmar' };
    }
    const day = new Intl.DateTimeFormat('pt-BR', { day: '2-digit' }).format(date);
    const month = new Intl.DateTimeFormat('pt-BR', { month: 'short' })
      .format(date)
      .replace('.', '')
      .toUpperCase();
    const weekday = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })
      .format(date)
      .replace('.', '')
      .toUpperCase();
    const hour = new Intl.DateTimeFormat('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);

    return {
      day,
      month,
      weekdayTime: `${weekday} ${hour}`,
    };
  } catch {
    return { day: '--', month: 'TBD', weekdayTime: 'Data a confirmar' };
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(amount);
}
