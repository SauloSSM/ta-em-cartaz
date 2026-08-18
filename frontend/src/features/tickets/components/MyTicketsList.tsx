import { useEffect, useState, useCallback } from 'react';
import { listMyTickets, type MyTicketResponse, TicketClientError } from '../api/ticketsApi';
import { listPublicEvents, listTicketSectors, type PublicEventResponse } from '../../events/api/eventsApi';
import { TicketCard } from './TicketCard';

export type MyTicketsListProps = {
  onSelectTicket: (ticket: MyTicketResponse, meta?: { event?: PublicEventResponse; sectorName?: string }) => void;
  onBrowseCatalog?: () => void;
};

export function MyTicketsList({ onSelectTicket, onBrowseCatalog }: MyTicketsListProps) {
  const [tickets, setTickets] = useState<MyTicketResponse[]>([]);
  const [eventsMap, setEventsMap] = useState<Record<string, PublicEventResponse>>({});
  const [sectorsMap, setSectorsMap] = useState<Record<string, string>>({}); // sectorId -> sectorName
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      // 1. Load tickets
      const response = await listMyTickets();
      setTickets(response.tickets);

      // 2. Load public events to enrich cards with title/date/venue
      if (response.tickets.length > 0) {
        try {
          const eventsResponse = await listPublicEvents();
          const eMap: Record<string, PublicEventResponse> = {};
          for (const ev of eventsResponse.events) {
            eMap[ev.id] = ev;
          }
          setEventsMap(eMap);

          // Unique event IDs to fetch sector names
          const uniqueEventIds = Array.from(new Set(response.tickets.map((t) => t.eventId)));
          const secMap: Record<string, string> = {};
          await Promise.all(
            uniqueEventIds.map(async (eventId) => {
              try {
                const sectorList = await listTicketSectors(eventId);
                for (const s of sectorList.sectors) {
                  secMap[s.id] = s.name;
                }
              } catch {
                // Ignore sector load failure and use fallback
              }
            })
          );
          setSectorsMap(secMap);
        } catch {
          // Failure to load event metadata is non-fatal; cards display fallback IDs
        }
      }
    } catch (err) {
      if (err instanceof TicketClientError) {
        setError(err.message);
      } else {
        setError('Não foi possível carregar seus ingressos. Verifique sua conexão e tente novamente.');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  return (
    <section className="edt-my-tickets" aria-labelledby="my-tickets-heading" data-testid="my-tickets-list-view">
      <header className="edt-my-tickets__header">
        <h2 id="my-tickets-heading" className="edt-my-tickets__title">
          Meus Ingressos
        </h2>
        <p className="edt-my-tickets__subtitle">
          Gerencie e acesse todos os seus ingressos emitidos para eventos confirmados.
        </p>
      </header>

      {/* Loading state */}
      {isLoading && (
        <div
          className="edt-my-tickets__loading"
          role="status"
          aria-live="polite"
          data-testid="my-tickets-loading"
        >
          <div className="edt-spinner" aria-hidden="true" />
          <p>Carregando seus ingressos...</p>
        </div>
      )}

      {/* Error state with retry */}
      {!isLoading && error && (
        <div
          className="edt-alert edt-alert--danger edt-my-tickets__error"
          role="alert"
          data-testid="my-tickets-error"
        >
          <h3 className="edt-alert__title">Erro ao carregar ingressos</h3>
          <p className="edt-alert__desc">{error}</p>
          <button
            type="button"
            className="edt-button edt-button--secondary edt-my-tickets__retry-btn"
            onClick={() => void loadData()}
            data-testid="my-tickets-retry-btn"
          >
            Tentar Novamente
          </button>
        </div>
      )}

      {/* Empty state */}
      {!isLoading && !error && tickets.length === 0 && (
        <div className="edt-empty-state edt-my-tickets__empty" data-testid="my-tickets-empty">
          <h3 className="edt-empty-state__title">Nenhum ingresso ainda</h3>
          <p className="edt-empty-state__desc">
            Você ainda não possui ingressos comprados. Explore os eventos em cartaz e garanta sua entrada!
          </p>
          {onBrowseCatalog && (
            <button
              type="button"
              className="edt-button edt-button--primary edt-empty-state__cta"
              onClick={onBrowseCatalog}
              data-testid="browse-catalog-btn"
            >
              Explorar Catálogo de Eventos →
            </button>
          )}
        </div>
      )}

      {/* List of tickets */}
      {!isLoading && !error && tickets.length > 0 && (
        <div className="edt-my-tickets__grid" data-testid="my-tickets-grid">
          {tickets.map((ticket) => {
            const ev = eventsMap[ticket.eventId];
            const sectorName = sectorsMap[ticket.sectorId];
            return (
              <TicketCard
                key={ticket.id}
                ticket={ticket}
                eventTitle={ev?.title}
                eventDate={ev?.startsAt}
                eventVenue={ev?.venueName}
                eventImageUrl={ev?.imageUrl}
                sectorName={sectorName}
                onOpenDetail={() => onSelectTicket(ticket, { event: ev, sectorName })}
              />
            );
          })}
        </div>
      )}
    </section>
  );
}
