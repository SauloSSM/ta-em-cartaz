import { useState, useEffect, useCallback } from 'react';
import { listGateEvents, type GateEvent } from '../api/gateApi';

export type GateContextSelectorProps = {
  selectedEventId: string | null;
  onSelectEvent: (event: GateEvent) => void;
};

export function GateContextSelector({
  selectedEventId,
  onSelectEvent,
}: GateContextSelectorProps) {
  const [events, setEvents] = useState<GateEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');

  const fetchEvents = useCallback(async (searchTerm?: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await listGateEvents(searchTerm);
      // Ensure only published events are shown
      const published = data.filter((e) => e.status === 'PUBLISHED');
      setEvents(published);
    } catch {
      setError('Não foi possível carregar os eventos publicados. Verifique sua conexão e tente novamente.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchEvents();
  }, [fetchEvents]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void fetchEvents(search);
  };

  const handleClearSearch = () => {
    setSearch('');
    void fetchEvents('');
  };

  return (
    <section aria-labelledby="gate-selector-title" className="edt-gate-selector">
      <h3 id="gate-selector-title" className="edt-gate-header__title" style={{ fontSize: '1.25rem' }}>
        Selecione o Evento de Trabalho
      </h3>
      <p className="edt-gate-header__subtitle">
        Escolha o evento publicado em que a portaria irá operar a validação de ingressos.
      </p>

      {/* Search Bar */}
      <form onSubmit={handleSearchSubmit} className="edt-gate-search-box" role="search">
        <label htmlFor="gate-search-input" className="sr-only" style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0,0,0,0)' }}>
          Buscar evento publicado por nome
        </label>
        <input
          id="gate-search-input"
          type="search"
          className="edt-gate-search-input"
          placeholder="Buscar por nome do evento…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Buscar eventos publicados"
        />
        <button type="submit" className="edt-gate-btn edt-gate-btn--primary edt-gate-btn--small">
          Buscar
        </button>
        {search && (
          <button
            type="button"
            className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
            onClick={handleClearSearch}
          >
            Limpar
          </button>
        )}
      </form>

      {/* Loading State */}
      {loading && (
        <div className="edt-gate-status" role="status" aria-live="polite">
          <p>Carregando eventos publicados disponíveis para a portaria…</p>
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="edt-gate-status edt-gate-status--error" role="alert" aria-live="assertive">
          <p>{error}</p>
          <button
            type="button"
            className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
            onClick={() => void fetchEvents(search)}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && events.length === 0 && (
        <div className="edt-gate-status" role="status">
          <p>Nenhum evento publicado disponível para controle de portaria no momento.</p>
        </div>
      )}

      {/* List of Published Events */}
      {!loading && !error && events.length > 0 && (
        <ul className="edt-gate-event-list" aria-label="Lista de eventos publicados para portaria">
          {events.map((event) => {
            const isSelected = event.id === selectedEventId;
            const formattedDate = event.startsAt
              ? new Date(event.startsAt).toLocaleDateString('pt-BR', {
                  weekday: 'short',
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })
              : null;

            return (
              <li
                key={event.id}
                className={`edt-gate-event-item ${isSelected ? 'edt-gate-event-item--active' : ''}`}
                data-testid={`gate-event-item-${event.id}`}
              >
                <div className="edt-gate-event-item__header">
                  <h4 className="edt-gate-event-item__title">{event.title}</h4>
                  {event.category && (
                    <span className="edt-gate-event-item__category">{event.category}</span>
                  )}
                </div>

                <div className="edt-gate-event-item__meta">
                  {formattedDate && <span><strong>Data:</strong> {formattedDate}</span>}
                  {(event.venueName || event.venueAddress) && (
                    <span>
                      <strong>Local:</strong> {[event.venueName, event.venueAddress].filter(Boolean).join(' — ')}
                    </span>
                  )}
                </div>

                <div className="edt-gate-event-item__action">
                  <button
                    type="button"
                    className={`edt-gate-btn ${isSelected ? 'edt-gate-btn--secondary' : 'edt-gate-btn--primary'} edt-gate-btn--small`}
                    onClick={() => onSelectEvent(event)}
                    aria-label={`Selecionar evento ${event.title}`}
                    aria-pressed={isSelected}
                    data-testid={`gate-select-event-btn-${event.id}`}
                  >
                    {isSelected ? '✓ Evento Selecionado' : 'Selecionar evento'}
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
