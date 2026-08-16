import { useState, useEffect, useCallback } from 'react';
import { getPublicTicket, type PublicTicketResponse, TicketClientError } from '../api/ticketsApi';
import { getEvent, listTicketSectors, type EventResponse } from '../../events/api/eventsApi';
import { QRCodePanel } from './QRCodePanel';

export type PublicSharedTicketProps = {
  shareToken: string;
  onBrowseCatalog?: () => void;
  onLoginClick?: () => void;
};

export function PublicSharedTicket({
  shareToken,
  onBrowseCatalog,
}: PublicSharedTicketProps) {
  const [ticket, setTicket] = useState<PublicTicketResponse | null>(null);
  const [eventData, setEventData] = useState<EventResponse | null>(null);
  const [sectorName, setSectorName] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadTicket = useCallback(async () => {
    if (!shareToken || shareToken.trim() === '') {
      setError('Ingresso não encontrado ou link inválido.');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const ticketData = await getPublicTicket(shareToken.trim());
      setTicket(ticketData);

      // Load event metadata for presentation
      try {
        const [eventRes, sectorsRes] = await Promise.all([
          getEvent(ticketData.eventId),
          listTicketSectors(ticketData.eventId),
        ]);
        setEventData(eventRes);
        const sector = sectorsRes.sectors.find((s) => s.id === ticketData.sectorId);
        if (sector) {
          setSectorName(sector.name);
        }
      } catch {
        // Event metadata load failure is non-fatal; ticket details are still displayed
      }
    } catch (err) {
      if (err instanceof TicketClientError) {
        if (err.code === 'TICKET_NOT_FOUND') {
          setError('Ingresso não encontrado ou link inválido.');
        } else {
          setError(err.message);
        }
      } else {
        setError('Ingresso não encontrado ou link inválido.');
      }
    } finally {
      setIsLoading(false);
    }
  }, [shareToken]);

  useEffect(() => {
    void loadTicket();
  }, [loadTicket]);

  const isUsed = ticket?.status === 'USED';
  const statusLabel = isUsed ? 'Utilizado' : 'Válido';

  return (
    <div className="edt-shared-ticket-page" data-testid="shared-ticket-page">
      {/* Navigation bar */}
      <div className="edt-ticket-detail__navigation">
        {onBrowseCatalog && (
          <button
            type="button"
            className="edt-button edt-button--secondary edt-ticket-detail__back-btn"
            onClick={onBrowseCatalog}
            data-testid="shared-ticket-back-to-catalog-btn"
          >
            ← Ver Eventos em Cartaz
          </button>
        )}
      </div>

      {/* Loading state */}
      {isLoading && (
        <div
          className="edt-my-tickets__loading"
          role="status"
          aria-live="polite"
          data-testid="shared-ticket-loading"
        >
          <div className="edt-spinner" aria-hidden="true" />
          <p>Carregando ingresso...</p>
        </div>
      )}

      {/* Error state */}
      {!isLoading && error && (
        <div
          className="edt-alert edt-alert--danger edt-my-tickets__error"
          role="alert"
          data-testid="shared-ticket-error"
        >
          <h3 className="edt-alert__title">Ingresso indisponível</h3>
          <p className="edt-alert__desc">{error}</p>
          <div className="edt-shared-ticket__error-actions" style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
            <button
              type="button"
              className="edt-button edt-button--secondary"
              onClick={() => void loadTicket()}
              data-testid="shared-ticket-retry-btn"
            >
              Tentar Novamente
            </button>
            {onBrowseCatalog && (
              <button
                type="button"
                className="edt-button edt-button--primary"
                onClick={onBrowseCatalog}
                data-testid="shared-ticket-browse-catalog-btn"
              >
                Explorar Catálogo de Eventos
              </button>
            )}
          </div>
        </div>
      )}

      {/* Valid or Used ticket display */}
      {!isLoading && !error && ticket && (
        <article className="edt-ticket-detail" data-testid="shared-ticket-view">
          <header className="edt-ticket-detail__header">
            <div className="edt-ticket-detail__meta-top">
              <span className="edt-ticket-detail__unit-tag">Ingresso #{ticket.ordinal}</span>
              <span
                className={`edt-status-badge ${
                  isUsed ? 'edt-status-badge--used' : 'edt-status-badge--valid'
                }`}
                data-testid="shared-ticket-status-badge"
              >
                {statusLabel}
              </span>
            </div>
            <h2 className="edt-ticket-detail__title" data-testid="shared-ticket-title">
              {eventData?.title || 'Show / Evento'}
            </h2>
            <div className="edt-ticket-detail__event-info">
              {sectorName && (
                <p className="edt-ticket-detail__info-line">
                  <strong>Setor:</strong> {sectorName}
                </p>
              )}
              {eventData?.startsAt && (
                <p className="edt-ticket-detail__info-line">
                  <strong>Data e Hora:</strong>{' '}
                  {new Date(eventData.startsAt).toLocaleDateString('pt-BR', {
                    weekday: 'long',
                    day: '2-digit',
                    month: 'long',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </p>
              )}
              {(eventData?.venueName || eventData?.venueAddress) && (
                <p className="edt-ticket-detail__info-line">
                  <strong>Local:</strong>{' '}
                  {[eventData?.venueName, eventData?.venueAddress].filter(Boolean).join(' — ')}
                </p>
              )}
            </div>
          </header>

          {/* AC: Estado textual precede a credencial quando usado */}
          {isUsed && (
            <div
              className="edt-alert edt-alert--warning edt-ticket-detail__used-notice"
              role="status"
              data-testid="shared-ticket-used-notice"
            >
              <h3 className="edt-alert__title">Ingresso Utilizado</h3>
              <p className="edt-alert__desc">
                Este ingresso já foi validado na portaria e <strong>não autoriza nova entrada</strong>. Suas credenciais permanecem visíveis para conferência histórica.
              </p>
            </div>
          )}

          <section className="edt-ticket-detail__credentials-section" aria-label="Credenciais de Acesso">
            <QRCodePanel
              validationToken={ticket.validationToken}
              manualCode={ticket.manualCode}
            />
          </section>
        </article>
      )}
    </div>
  );
}
