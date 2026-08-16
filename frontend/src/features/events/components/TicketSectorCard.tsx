import type { TicketSectorResponse } from '../api/eventsApi';

export type TicketSectorCardProps = {
  sector: TicketSectorResponse;
  selected: boolean;
  disabled?: boolean;
  onSelect: (sector: TicketSectorResponse) => void;
};

export function TicketSectorCard({
  sector,
  selected,
  disabled = false,
  onSelect,
}: TicketSectorCardProps) {
  const isSoldOut = sector.availableQuantity <= 0;
  const isSelectable = !disabled && !isSoldOut;

  const formattedPrice = formatCurrency(sector.price);

  return (
    <div
      className={`edt-ticket-sector-card ${selected ? 'edt-ticket-sector-card--selected' : ''} ${isSoldOut ? 'edt-ticket-sector-card--sold-out' : ''} ${disabled ? 'edt-ticket-sector-card--disabled' : ''}`}
      data-testid={`sector-card-${sector.id}`}
      aria-disabled={!isSelectable}
    >
      <div className="edt-ticket-sector-card__header">
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

        <span className="edt-ticket-sector-card__price" id={`sector-price-${sector.id}`}>
          {formattedPrice}
        </span>
      </div>

      {sector.description ? (
        <p className="edt-ticket-sector-card__desc" id={`sector-desc-${sector.id}`}>
          {sector.description}
        </p>
      ) : null}

      <div className="edt-ticket-sector-card__footer">
        <div className="edt-ticket-sector-card__availability" id={`sector-avail-${sector.id}`}>
          {isSoldOut ? (
            <span className="edt-ticket-sector-card__sold-out-badge" role="status">
              Esgotado
            </span>
          ) : (
            <span className="edt-ticket-sector-card__available-text" role="status">
              {sector.availableQuantity} {sector.availableQuantity === 1 ? 'ingresso disponível' : 'ingressos disponíveis'}
            </span>
          )}
        </div>

        {isSelectable && (
          <button
            type="button"
            className={`edt-button edt-button--small ${selected ? 'edt-button--primary' : 'edt-button--secondary'}`}
            onClick={() => onSelect(sector)}
            aria-label={`Selecionar setor ${sector.name} por ${formattedPrice}`}
          >
            {selected ? 'Selecionado' : 'Selecionar'}
          </button>
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
