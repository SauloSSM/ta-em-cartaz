import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { Button } from '../../../shared/components/Button/Button';
import { SearchInput } from '../../../shared/components/SearchInput/SearchInput';
import { listPublicEvents, type PublicEventResponse } from '../api/eventsApi';
import { CategoryStrip } from './CategoryStrip';
import { EventRow } from './EventRow';
import { HomeHero } from './HomeHero';
import './PublicEventCatalog.css';

const homeLeftEditorialPanelSvg = new URL(
  '../../../assets/ta-em-cartaz/home/home-left-editorial-panel.png',
  import.meta.url,
).href;
const homeCulturaVivaPanelSvg = new URL(
  '../../../assets/ta-em-cartaz/home/home-cultura-viva-panel.png',
  import.meta.url,
).href;

export type PublicEventCatalogProps = {
  onSelectEvent?: (event: PublicEventResponse) => void;
  onLoginClick?: () => void;
};

type CatalogState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'success'; events: PublicEventResponse[]; searchSubmitted: string };

export function PublicEventCatalog({ onSelectEvent }: PublicEventCatalogProps) {
  const [searchInput, setSearchInput] = useState('');
  const [currentSearch, setCurrentSearch] = useState('');
  const [state, setState] = useState<CatalogState>({ status: 'loading' });
  const [statusAnnouncement, setStatusAnnouncement] = useState('');
  const [headerSearchTarget, setHeaderSearchTarget] = useState<HTMLElement | null>(null);

  useEffect(() => {
    setHeaderSearchTarget(document.getElementById('tc-header-search-slot'));
  }, []);

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

  const searchUi = (
    <div className="tc-home-search">
      <form
        role="search"
        className="tc-home-search__form"
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
          className="tc-home-search__submit edt-catalog__search-submit"
          aria-label="Buscar"
        >
          Buscar
        </Button>
      </form>
    </div>
  );

  return (
    <div className="tc-home-catalog edt-catalog">
      {headerSearchTarget ? createPortal(searchUi, headerSearchTarget) : (
        <div className="tc-home-search-fallback tc-container">{searchUi}</div>
      )}
      <HomeHero />
      <CategoryStrip />

      <section className="tc-cartaz-section edt-catalog" aria-labelledby="catalog-heading">
        <div className="tc-container tc-cartaz-section__container">
          <div className="tc-visually-hidden edt-visually-hidden" role="status" aria-live="polite">
            {statusAnnouncement}
          </div>

          <h2 id="catalog-heading" className="tc-visually-hidden">
            Em Cartaz
          </h2>

          <div className="tc-cartaz-layout">
            <aside className="tc-cartaz-layout__editorial" aria-hidden="true">
              <img
                src={homeLeftEditorialPanelSvg}
                alt=""
                className="tc-cartaz-layout__editorial-art"
              />
            </aside>

            <div className="tc-cartaz-layout__programming">
              {state.status === 'success' && state.searchSubmitted ? (
                <div className="tc-cartaz-filter edt-catalog__results-bar">
                  <span className="edt-catalog__results-count">
                    {state.events.length}{' '}
                    {state.events.length === 1 ? 'evento encontrado' : 'eventos encontrados'} para &ldquo;
                    {state.searchSubmitted}&rdquo;
                  </span>
                  <button
                    type="button"
                    className="tc-cartaz-filter__clear edt-catalog__filter-clear-link"
                    onClick={handleClearSearch}
                  >
                    Limpar filtro
                  </button>
                </div>
              ) : null}

              <div className="tc-cartaz-programming-card">
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
                    <div className="tc-cartaz-skeleton-row edt-event-card-skeleton" />
                  </div>
                )}

                {state.status === 'error' && (
                  <div className="tc-cartaz-state tc-cartaz-state--error edt-catalog__error-state" role="alert">
                    <svg
                      className="tc-cartaz-state__icon edt-catalog__error-icon"
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
                    <h2 className="tc-cartaz-state__title edt-catalog__error-title">
                      Erro ao carregar eventos
                    </h2>
                    <p className="tc-cartaz-state__message edt-catalog__error-message">
                      {state.message}
                    </p>
                    <Button
                      type="button"
                      variant="secondary"
                      className="tc-cartaz-state__button edt-catalog__retry-btn"
                      onClick={handleRetry}
                    >
                      Tentar novamente
                    </Button>
                  </div>
                )}

                {state.status === 'success' && state.events.length === 0 && (
                  <div className="tc-cartaz-state tc-cartaz-state--empty edt-catalog__empty-state">
                    {state.searchSubmitted ? (
                      <>
                        <h2 className="tc-cartaz-state__title edt-catalog__empty-title">
                          Nenhum evento encontrado
                        </h2>
                        <p className="tc-cartaz-state__message edt-catalog__empty-message">
                          Não encontramos eventos com o termo <strong>&ldquo;{state.searchSubmitted}&rdquo;</strong>.
                        </p>
                        <Button
                          type="button"
                          variant="secondary"
                          className="tc-cartaz-state__button edt-catalog__clear-empty-btn"
                          onClick={handleClearSearch}
                        >
                          Ver todos os eventos
                        </Button>
                      </>
                    ) : (
                      <>
                        <h2 className="tc-cartaz-state__title edt-catalog__empty-title">
                          Nenhum evento publicado
                        </h2>
                        <p className="tc-cartaz-state__message edt-catalog__empty-message">
                          Não há eventos disponíveis para venda no momento. Volte em breve!
                        </p>
                      </>
                    )}
                  </div>
                )}

                {state.status === 'success' && state.events.length > 0 && (
                  <div
                    className="tc-cartaz-events edt-catalog__grid"
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

            <aside className="tc-cartaz-layout__culture" aria-hidden="true">
              <img
                src={homeCulturaVivaPanelSvg}
                alt=""
                className="tc-cartaz-layout__culture-art"
              />
            </aside>
          </div>
        </div>
      </section>
    </div>
  );
}
