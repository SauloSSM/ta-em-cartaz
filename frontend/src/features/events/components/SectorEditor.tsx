import { useState, useEffect, useRef, type FormEvent } from 'react';
import {
  createTicketSector,
  updateTicketSector,
  type TicketSectorResponse,
  type CreateTicketSectorRequest,
  type UpdateTicketSectorRequest,
} from '../api/eventsApi';

type SectorEditorProps = {
  eventId: string;
  sector?: TicketSectorResponse | null;
  isOpen: boolean;
  onSaved: (savedSector: TicketSectorResponse) => void;
  onCancel: () => void;
};

export function SectorEditor({
  eventId,
  sector,
  isOpen,
  onSaved,
  onCancel,
}: SectorEditorProps) {
  const isEditing = sector !== null && sector !== undefined;

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [capacity, setCapacity] = useState<string>('');
  const [price, setPrice] = useState<string>('');
  const [busy, setBusy] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const nameInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      if (sector) {
        setName(sector.name);
        setDescription(sector.description ?? '');
        setCapacity(String(sector.capacity));
        setPrice(String(sector.price));
      } else {
        setName('');
        setDescription('');
        setCapacity('');
        setPrice('');
      }
      setErrorMessage(null);
      setFieldErrors({});
      setTimeout(() => nameInputRef.current?.focus(), 50);

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Escape' && !busy) {
          onCancel();
        }
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, sector, busy, onCancel]);

  if (!isOpen) {
    return null;
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage(null);
    setFieldErrors({});

    const errors: Record<string, string> = {};
    const trimmedName = name.trim();
    if (!trimmedName) {
      errors.name = 'Nome do setor é obrigatório.';
    }

    const parsedCapacity = parseInt(capacity, 10);
    if (isNaN(parsedCapacity) || parsedCapacity <= 0) {
      errors.capacity = 'Capacidade deve ser um número inteiro maior que zero.';
    }

    const parsedPrice = parseFloat(price.replace(',', '.'));
    if (isNaN(parsedPrice) || parsedPrice < 0) {
      errors.price = 'Preço deve ser maior ou igual a zero.';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setBusy(true);
    try {
      if (isEditing) {
        const payload: UpdateTicketSectorRequest = {
          name: trimmedName,
          description: description.trim() ? description : undefined,
          capacity: parsedCapacity,
          price: parsedPrice,
        };
        const updated = await updateTicketSector(eventId, sector.id, payload);
        onSaved(updated);
      } else {
        const payload: CreateTicketSectorRequest = {
          name: trimmedName,
          description: description.trim() ? description : undefined,
          capacity: parsedCapacity,
          price: parsedPrice,
        };
        const created = await createTicketSector(eventId, payload);
        onSaved(created);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar setor de ingressos.';
      setErrorMessage(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="dialog-backdrop" role="presentation">
      <div
        className="dialog-box sector-editor-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="sector-editor-title"
      >
        <h3 id="sector-editor-title">
          {isEditing ? `Editar Setor: ${sector.name}` : 'Adicionar Novo Setor'}
        </h3>

        {errorMessage !== null ? (
          <div className="dialog-error" role="alert">
            <p>{errorMessage}</p>
          </div>
        ) : null}

        <form onSubmit={handleSubmit} className="sector-editor-form" noValidate>
          <div className="form-group">
            <label htmlFor="sector-name-input">
              Nome do Setor <span aria-hidden="true">*</span>
            </label>
            <input
              ref={nameInputRef}
              id="sector-name-input"
              type="text"
              required
              disabled={busy}
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (fieldErrors.name) {
                  setFieldErrors((prev) => ({ ...prev, name: '' }));
                }
              }}
              placeholder="Ex.: Pista Premium, Camarote VIP"
              aria-invalid={fieldErrors.name ? 'true' : 'false'}
              aria-describedby={fieldErrors.name ? 'sector-name-error' : undefined}
            />
            {fieldErrors.name ? (
              <span id="sector-name-error" className="field-error" role="alert">
                {fieldErrors.name}
              </span>
            ) : null}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="sector-capacity-input">
                Capacidade Total <span aria-hidden="true">*</span>
              </label>
              <input
                id="sector-capacity-input"
                type="number"
                min="1"
                step="1"
                required
                disabled={busy}
                value={capacity}
                onChange={(e) => {
                  setCapacity(e.target.value);
                  if (fieldErrors.capacity) {
                    setFieldErrors((prev) => ({ ...prev, capacity: '' }));
                  }
                }}
                placeholder="Ex.: 500"
                aria-invalid={fieldErrors.capacity ? 'true' : 'false'}
                aria-describedby={fieldErrors.capacity ? 'sector-capacity-error' : undefined}
              />
              {fieldErrors.capacity ? (
                <span id="sector-capacity-error" className="field-error" role="alert">
                  {fieldErrors.capacity}
                </span>
              ) : null}
            </div>

            <div className="form-group">
              <label htmlFor="sector-price-input">
                Preço Unitário (R$) <span aria-hidden="true">*</span>
              </label>
              <input
                id="sector-price-input"
                type="number"
                min="0"
                step="0.01"
                required
                disabled={busy}
                value={price}
                onChange={(e) => {
                  setPrice(e.target.value);
                  if (fieldErrors.price) {
                    setFieldErrors((prev) => ({ ...prev, price: '' }));
                  }
                }}
                placeholder="0.00"
                aria-invalid={fieldErrors.price ? 'true' : 'false'}
                aria-describedby={fieldErrors.price ? 'sector-price-error' : undefined}
              />
              {fieldErrors.price ? (
                <span id="sector-price-error" className="field-error" role="alert">
                  {fieldErrors.price}
                </span>
              ) : null}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="sector-description-input">Descrição (Opcional)</label>
            <textarea
              id="sector-description-input"
              rows={3}
              disabled={busy}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Informações adicionais sobre o setor (benefícios, localização, etc.)"
            />
          </div>

          <div className="dialog-actions">
            <button
              type="button"
              className="dialog-cancel-btn"
              disabled={busy}
              onClick={onCancel}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="dialog-save-btn"
              disabled={busy}
            >
              {busy ? 'Salvando…' : isEditing ? 'Salvar Alterações' : 'Criar Setor'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
