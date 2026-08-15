import { useState } from 'react';
import type { CatalogEventReference } from '../api/catalogApi';

type TicketmasterResultCardProps = {
  event: CatalogEventReference;
  onSelectReference: (event: CatalogEventReference) => void;
  disabled?: boolean;
  isLoading?: boolean;
};

export function TicketmasterResultCard({
  event,
  onSelectReference,
  disabled = false,
  isLoading = false,
}: TicketmasterResultCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  const hasImage = event.imageUrl !== undefined && event.imageUrl.trim().length > 0 && !imageFailed;

  return (
    <article
      className="catalog-card"
      aria-labelledby={`event-title-${event.externalId}`}
    >
      <div className="catalog-card-media">
        {hasImage ? (
          <img
            src={event.imageUrl}
            alt={`Banner do evento ${event.title}`}
            loading="lazy"
            onError={() => setImageFailed(true)}
            className="catalog-card-image"
          />
        ) : (
          <div className="catalog-card-fallback-image" aria-hidden="true">
            <span>Sem imagem</span>
          </div>
        )}
      </div>

      <div className="catalog-card-content">
        {event.category !== undefined && event.category.trim().length > 0 ? (
          <span className="catalog-card-category">{event.category}</span>
        ) : null}

        <h3 id={`event-title-${event.externalId}`} className="catalog-card-title">
          {event.title}
        </h3>

        {event.description !== undefined && event.description.trim().length > 0 ? (
          <p className="catalog-card-description">{event.description}</p>
        ) : null}

        <div className="catalog-card-actions">
          <button
            type="button"
            className="catalog-card-select-button"
            disabled={disabled || isLoading}
            onClick={() => onSelectReference(event)}
            aria-label={`Usar ${event.title} como referência`}
          >
            {isLoading ? 'Criando rascunho…' : 'Usar como referência'}
          </button>
        </div>
      </div>
    </article>
  );
}
