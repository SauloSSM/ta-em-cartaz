import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { BrandSeal } from '../../../shared/components/Brand/BrandSeal';
import { Button } from '../../../shared/components/Button/Button';
import { SearchInput } from '../../../shared/components/SearchInput/SearchInput';
import { listPublicEvents, type PublicEventResponse } from '../api/eventsApi';
import { CategoryStrip } from './CategoryStrip';
import { EventRow } from './EventRow';
import { HomeHero } from './HomeHero';
import './PublicEventCatalog.css';

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
    <div className="tc-home-catalog edt-catalog">
      {/* 1. Hero Section: Expressive Brand Headline & Festival Collage */}
      <HomeHero />

      {/* 2. Category Strip (Non-interactive Visual Motif) */}
      <CategoryStrip />

      {/* 3. Main Discovery Section: EM CARTAZ */}
      <section
        className="tc-cartaz-section"
        aria-labelledby="em-cartaz-heading"
      >
        <div className="tc-container">
          <header className="tc-cartaz-section__header">
            <div className="tc-cartaz-section__title-group">
              <h2 id="em-cartaz-heading" className="tc-cartaz-section__title">
                EM CARTAZ
              </h2>
              <div className="tc-cartaz-section__underline" aria-hidden="true" />
            </div>

            {/* Search Form */}
            <div className="tc-cartaz-section__search">
              <form
                role="search"
                className="tc-cartaz-section__search-form"
                onSubmit={handleSearchSubmit}
                aria-label="Buscar eventos públicos"
              >
                <SearchInput
                  id="tc-catalog-search"
                  value={searchInput}
                  onChange={setSearchInput}
                  onClear={handleClearSearch}
                  placeholder="Buscar eventos por título..."
                  label="Buscar eventos por título"
                />
                <Button
                  type="submit"
                  variant="primary"
                  className="tc-cartaz-section__search-btn edt-catalog__search-submit"
                >
                  Buscar
                </Button>
              </form>
              {onLoginClick ? (
                <div className="tc-cartaz-login-prompt" style={{ marginTop: 'var(--space-2)' }}>
                  <span style={{ fontSize: 'var(--type-label-size)' }}>Já possui uma conta?</span>{' '}
                  <button
                    type="button"
                    className="edt-link-btn"
                    style={{ fontSize: 'var(--type-label-size)' }}
                    onClick={onLoginClick}
                  >
                    Acessar minha conta →
                  </button>
                </div>
              ) : null}
            </div>
          </header>

          {/* Screen Reader Live Region */}
          <div className="tc-visually-hidden edt-visually-hidden" role="status" aria-live="polite">
            {statusAnnouncement}
          </div>

          {/* Active Search Filter Status Bar */}
          {state.status === 'success' && state.searchSubmitted ? (
            <div className="tc-cartaz-section__filter-bar edt-catalog__results-bar">
              <span className="edt-catalog__results-count">
                {state.events.length}{' '}
                {state.events.length === 1 ? 'evento encontrado' : 'eventos encontrados'} para &ldquo;
                {state.searchSubmitted}&rdquo;
              </span>
              <button
                type="button"
                className="tc-cartaz-section__filter-clear edt-catalog__filter-clear-link"
                onClick={handleClearSearch}
              >
                Limpar filtro
              </button>
            </div>
          ) : null}

          {/* Discovery Layout */}
          <div className="tc-cartaz-section__layout">
            {/* Sidebar Branding Stamp (Desktop Only) */}
            <aside className="tc-cartaz-sidebar" aria-hidden="true">
              <div className="tc-cartaz-sidebar__card">
                <BrandSeal variant="primary" size={54} />
                <span className="tc-cartaz-sidebar__text">CULTURA QUE CONECTA.</span>
                <span className="tc-cartaz-sidebar__note">PROGRAMAÇÃO OFICIAL</span>
              </div>
            </aside>

            {/* Main Content Area */}
            <div className="tc-cartaz-list">
              {/* Loading State */}
              {state.status === 'loading' && (
                <div
                  className="tc-cartaz-skeleton-list edt-catalog__loading"
                  role="status"
                  aria-busy="true"
                  aria-label="Carregando eventos"
                >
                  <p className="tc-visually-hidden edt-catalog__loading-text">
                    Carregando catálogo de eventos…
                  </p>
                  <div className="tc-cartaz-skeleton-row edt-event-card-skeleton" />
                  <div className="tc-cartaz-skeleton-row edt-event-card-skeleton" />
                  <div className="tc-cartaz-skeleton-row edt-event-card-skeleton" />
                </div>
              )}

              {/* Error State */}
              {state.status === 'error' && (
                <div className="tc-cartaz-error edt-catalog__error-state" role="alert">
                  <svg
                    className="tc-cartaz-error__icon edt-catalog__error-icon"
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
                  <h2 className="tc-cartaz-error__title edt-catalog__error-title">
                    Erro ao carregar eventos
                  </h2>
                  <p className="tc-cartaz-error__message edt-catalog__error-message">
                    {state.message}
                  </p>
                  <Button
                    type="button"
                    variant="secondary"
                    className="tc-cartaz-error__btn edt-catalog__retry-btn"
                    onClick={handleRetry}
                  >
                    Tentar novamente
                  </Button>
                </div>
              )}

              {/* Empty State */}
              {state.status === 'success' && state.events.length === 0 && (
                <div className="tc-cartaz-empty edt-catalog__empty-state">
                  <svg
                    className="tc-cartaz-empty__icon edt-catalog__empty-icon"
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
                      <h2 className="tc-cartaz-empty__title edt-catalog__empty-title">
                        Nenhum evento encontrado
                      </h2>
                      <p className="tc-cartaz-empty__message edt-catalog__empty-message">
                        Não encontramos eventos com o termo <strong>&ldquo;{state.searchSubmitted}&rdquo;</strong>.
                      </p>
                      <Button
                        type="button"
                        variant="secondary"
                        className="tc-cartaz-empty__btn edt-catalog__clear-empty-btn"
                        onClick={handleClearSearch}
                      >
                        Ver todos os eventos
                      </Button>
                    </>
                  ) : (
                    <>
                      <h2 className="tc-cartaz-empty__title edt-catalog__empty-title">
                        Nenhum evento publicado
                      </h2>
                      <p className="tc-cartaz-empty__message edt-catalog__empty-message">
                        Não há eventos disponíveis para venda no momento. Volte em breve!
                      </p>
                    </>
                  )}
                </div>
              )}

              {/* Success State with Event Rows */}
              {state.status === 'success' && state.events.length > 0 && (
                <div
                  className="tc-cartaz-section__events edt-catalog__grid"
                  role="list"
                  aria-label="Lista de eventos públicos"
                >
                  {state.events.map((event) => (
                    <div key={event.id} role="listitem">
                      <EventRow event={event} onClick={onSelectEvent} />
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
