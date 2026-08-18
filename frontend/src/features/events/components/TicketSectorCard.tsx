import type { ReactNode } from 'react';
import type { TicketSectorResponse } from '../api/eventsApi';

export type TicketSectorCardProps = {
  sector: TicketSectorResponse;
  selected: boolean;
  disabled?: boolean;
  index?: number;
  control?: ReactNode;
  onSelect: (sector: TicketSectorResponse) => void;
};

export function TicketSectorCard({
  sector,
  selected,
  disabled = false,
  index = 1,
  control,
  onSelect,
}: TicketSectorCardProps) {
  const isSoldOut = sector.availableQuantity <= 0;
  const isSelectable = !disabled && !isSoldOut;
  const formattedPrice = formatCurrency(sector.price);
  const accentIndex = ((Math.max(1, index) - 1) % 4) + 1;

  return (
    <div
      className={`edt-ticket-sector-card edt-ticket-sector-card--accent-${accentIndex} ${selected ? 'edt-ticket-sector-card--selected' : ''} ${isSoldOut ? 'edt-ticket-sector-card--sold-out' : ''} ${disabled ? 'edt-ticket-sector-card--disabled' : ''}`}
      data-testid={`sector-card-${sector.id}`}
      aria-disabled={!isSelectable}
    >
      <div className="edt-ticket-sector-card__tab" aria-hidden="true">
        <span>SETOR</span>
        <strong>{String(index).padStart(2, '0')}</strong>
      </div>

      <div className="edt-ticket-sector-card__content">
        <div className="edt-ticket-sector-card__selection">
          <input
            type="radio"
            id={`sector-radio-${sector.id}`}
            name="ticket-sector-selection"
            checked={selected}
            disabled={!isSelectable}
            onChange={() => {
              if (isSelectable) {
                onSelect(sector);
              }
            }}
            className="edt-ticket-sector-card__radio"
            aria-labelledby={`sector-name-${sector.id} sector-price-${sector.id}`}
            aria-describedby={`sector-avail-${sector.id} ${sector.description ? `sector-desc-${sector.id}` : ''}`.trim()}
          />
          <label htmlFor={`sector-radio-${sector.id}`} className="edt-ticket-sector-card__name" id={`sector-name-${sector.id}`}>
            {sector.name}
          </label>
        </div>

        {sector.description ? (
          <p className="edt-ticket-sector-card__desc" id={`sector-desc-${sector.id}`}>
            {sector.description}
          </p>
        ) : null}

        <div className="edt-ticket-sector-card__availability" id={`sector-avail-${sector.id}`}>
          {isSoldOut ? (
            <span className="edt-ticket-sector-card__sold-out-badge" role="status">
              Esgotado
            </span>
          ) : (
            <span className="edt-ticket-sector-card__available-text" role="status">
              Disponíveis · {sector.availableQuantity}
            </span>
          )}
        </div>
      </div>

      <div className="edt-ticket-sector-card__price-block" id={`sector-price-${sector.id}`}>
        <span>A PARTIR DE</span>
        <strong>{formattedPrice}</strong>
        <small>POR INGRESSO</small>
      </div>

      <div className="edt-ticket-sector-card__control">
        {control ?? (
          isSelectable ? (
            <button
              type="button"
              className={`edt-ticket-sector-card__select-btn ${selected ? 'edt-ticket-sector-card__select-btn--selected' : ''}`}
              onClick={() => onSelect(sector)}
              aria-label={`Selecionar setor ${sector.name} por ${formattedPrice}`}
            >
              {selected ? 'Selecionado' : '+'}
            </button>
          ) : null
        )}
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
