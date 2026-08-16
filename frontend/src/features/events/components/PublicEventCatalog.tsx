import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { listPublicEvents, type PublicEventResponse } from '../api/eventsApi';
import { EventCard } from './EventCard';

export type PublicEventCatalogProps = {
  onSelectEvent?: (event: PublicEventResponse) => void;
  onLoginClick?: () => void;
};

type CatalogState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'success'; events: PublicEventResponse[]; searchSubmitted: string };

export function PublicEventCatalog({ onSelectEvent, onLoginClick }: PublicEventCatalogProps) {
  const [searchInput, setSearchInput] = useState('');
  const [currentSearch, setCurrentSearch] = useState('');
  const [state, setState] = useState<CatalogState>({ status: 'loading' });
  const [statusAnnouncement, setStatusAnnouncement] = useState('');

  const loadEvents = useCallback(async (searchTerm: string) => {
    setState({ status: 'loading' });
    setStatusAnnouncement('Carregando eventos…');
    try {
      const response = await listPublicEvents(searchTerm);
      setState({
        status: 'success',
        events: response.events,
        searchSubmitted: searchTerm,
      });

      if (response.events.length === 0) {
        setStatusAnnouncement(
          searchTerm
            ? `Nenhum evento encontrado para "${searchTerm}".`
            : 'Nenhum evento publicado no momento.',
        );
      } else {
        const count = response.events.length;
        setStatusAnnouncement(
          `${count} ${count === 1 ? 'evento encontrado' : 'eventos encontrados'}${searchTerm ? ` para "${searchTerm}"` : ''}.`,
        );
      }
    } catch {
      setState({
        status: 'error',
        message: 'Não foi possível carregar o catálogo de eventos. Verifique sua conexão e tente novamente.',
      });
      setStatusAnnouncement('Erro ao carregar catálogo de eventos.');
    }
  }, []);

  useEffect(() => {
    void loadEvents(currentSearch);
  }, [currentSearch, loadEvents]);

  const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setCurrentSearch(searchInput.trim());
  };

  const handleClearSearch = () => {
    setSearchInput('');
    setCurrentSearch('');
  };

  const handleRetry = () => {
    void loadEvents(currentSearch);
  };

  return (
    <section className="edt-catalog" aria-labelledby="catalog-heading">
      <header className="edt-catalog__header">
        <div className="edt-catalog__hero">
          <span className="edt-catalog__eyebrow">Temporada 2026</span>
          <h1 id="catalog-heading" className="edt-catalog__title">
            Catálogo de Eventos
          </h1>
          <p className="edt-catalog__subtitle">
            Explore grandes festivais, shows e experiências culturais inesquecíveis.
          </p>
          {onLoginClick && (
            <div className="edt-catalog__login-cta">
              <span>Já possui uma conta?</span>{' '}
              <button
                type="button"
                className="edt-link-btn"
                onClick={onLoginClick}
              >
                Acessar minha conta →
              </button>
            </div>
          )}
        </div>

        <form
          role="search"
          className="edt-catalog__search-form"
          onSubmit={handleSearchSubmit}
          aria-label="Buscar eventos públicos"
        >
          <div className="edt-catalog__search-group">
            <label htmlFor="catalog-search-input" className="edt-visually-hidden">
              Buscar eventos por título
            </label>
            <div className="edt-catalog__input-wrapper">
              <svg
                className="edt-catalog__search-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                id="catalog-search-input"
                type="search"
                className="edt-input edt-catalog__search-input"
                placeholder="Buscar eventos por título..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                autoComplete="off"
              />
              {searchInput && (
                <button
                  type="button"
                  className="edt-catalog__clear-btn"
                  onClick={handleClearSearch}
                  aria-label="Limpar busca"
                >
                  ✕
                </button>
              )}
            </div>
            <button
              type="submit"
              className="edt-button edt-button--primary edt-catalog__search-submit"
            >
              Buscar
            </button>
          </div>
        </form>
      </header>

      {/* Região ao vivo para leitores de tela */}
      <div className="edt-visually-hidden" role="status" aria-live="polite">
        {statusAnnouncement}
      </div>

      <main className="edt-catalog__content">
        {state.status === 'loading' && (
          <div
            className="edt-catalog__loading"
            role="status"
            aria-busy="true"
            aria-label="Carregando eventos"
          >
            <div className="edt-catalog__loading-spinner" aria-hidden="true" />
            <p className="edt-catalog__loading-text">Carregando catálogo de eventos…</p>
            <div className="edt-catalog__grid edt-catalog__grid--skeleton" aria-hidden="true">
              {[1, 2, 3].map((n) => (
                <div key={n} className="edt-event-card-skeleton" />
              ))}
            </div>
          </div>
        )}

        {state.status === 'error' && (
          <div className="edt-catalog__error-state" role="alert">
            <svg
              className="edt-catalog__error-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <h2 className="edt-catalog__error-title">Erro ao carregar eventos</h2>
            <p className="edt-catalog__error-message">{state.message}</p>
            <button
              type="button"
              className="edt-button edt-button--secondary edt-catalog__retry-btn"
              onClick={handleRetry}
            >
              Tentar novamente
            </button>
          </div>
        )}

        {state.status === 'success' && state.events.length === 0 && (
          <div className="edt-catalog__empty-state">
            <svg
              className="edt-catalog__empty-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
            {state.searchSubmitted ? (
              <>
                <h2 className="edt-catalog__empty-title">Nenhum evento encontrado</h2>
                <p className="edt-catalog__empty-message">
                  Não encontramos eventos com o termo <strong>&ldquo;{state.searchSubmitted}&rdquo;</strong>.
                </p>
                <button
                  type="button"
                  className="edt-button edt-button--secondary edt-catalog__clear-empty-btn"
                  onClick={handleClearSearch}
                >
                  Ver todos os eventos
                </button>
              </>
            ) : (
              <>
                <h2 className="edt-catalog__empty-title">Nenhum evento publicado</h2>
                <p className="edt-catalog__empty-message">
                  Não há eventos disponíveis para venda no momento. Volte em breve!
                </p>
              </>
            )}
          </div>
        )}

        {state.status === 'success' && state.events.length > 0 && (
          <div className="edt-catalog__results">
            {state.searchSubmitted ? (
              <div className="edt-catalog__results-bar">
                <span className="edt-catalog__results-count">
                  {state.events.length}{' '}
                  {state.events.length === 1 ? 'evento encontrado' : 'eventos encontrados'} para &ldquo;
                  {state.searchSubmitted}&rdquo;
                </span>
                <button
                  type="button"
                  className="edt-catalog__filter-clear-link"
                  onClick={handleClearSearch}
                >
                  Limpar filtro
                </button>
              </div>
            ) : null}

            <div className="edt-catalog__grid" role="list" aria-label="Lista de eventos públicos">
              {state.events.map((event) => (
                <div key={event.id} role="listitem">
                  <EventCard event={event} onClick={onSelectEvent} />
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </section>
  );
}
