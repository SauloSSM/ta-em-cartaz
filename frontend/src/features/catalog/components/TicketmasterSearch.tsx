import { useState, useTransition, type FormEvent } from 'react';
import { searchCatalogEvents, type CatalogEventReference } from '../api/catalogApi';
import { TicketmasterResultCard } from './TicketmasterResultCard';

type TicketmasterSearchProps = {
  onSelectReference?: (event: CatalogEventReference) => void;
};

type SearchState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'empty'; query: string }
  | { kind: 'success'; events: CatalogEventReference[]; query: string }
  | { kind: 'error'; message: string; query: string };

export function TicketmasterSearch({ onSelectReference }: TicketmasterSearchProps) {
  const [keyword, setKeyword] = useState('');
  const [searchState, setSearchState] = useState<SearchState>({ kind: 'idle' });
  const [selectedReference, setSelectedReference] = useState<CatalogEventReference | null>(null);
  const [announcement, setAnnouncement] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  const performSearch = async (searchTerm: string) => {
    setSearchState({ kind: 'loading' });
    setAnnouncement('Buscando eventos no catálogo Ticketmaster…');

    try {
      const response = await searchCatalogEvents(searchTerm);
      if (response.events.length === 0) {
        setSearchState({ kind: 'empty', query: searchTerm });
        setAnnouncement('Nenhum evento encontrado no catálogo Ticketmaster.');
      } else {
        setSearchState({ kind: 'success', events: response.events, query: searchTerm });
        setAnnouncement(`${response.events.length} eventos encontrados no catálogo Ticketmaster.`);
      }
    } catch (err: unknown) {
      const message = err instanceof Error
        ? err.message
        : 'Catálogo Ticketmaster temporariamente indisponível.';
      setSearchState({ kind: 'error', message, query: searchTerm });
      setAnnouncement(`Erro ao buscar no catálogo: ${message}`);
    }
  };

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    void performSearch(keyword);
  };

  const handleRetry = () => {
    const termToRetry = searchState.kind === 'error' ? searchState.query : keyword;
    setKeyword(termToRetry);
    void performSearch(termToRetry);
  };

  const handleSelect = (event: CatalogEventReference) => {
    startTransition(() => {
      setSelectedReference(event);
      setAnnouncement(`Referência selecionada: ${event.title}. Criação de rascunho disponível na próxima etapa.`);
    });
    if (onSelectReference !== undefined) {
      onSelectReference(event);
    }
  };

  const isBusy = searchState.kind === 'loading';

  return (
    <section className="catalog-search-section" aria-labelledby="catalog-search-title">
      <header className="catalog-search-header">
        <h2 id="catalog-search-title">Pesquisar referências Ticketmaster</h2>
        <p className="catalog-search-subtitle">
          Consulte eventos no catálogo externo para usar como base para novos eventos.
        </p>
      </header>

      <form onSubmit={handleSubmit} className="catalog-search-form" role="search">
        <div className="catalog-search-input-group">
          <label htmlFor="catalog-search-input">Palavra-chave do evento</label>
          <input
            id="catalog-search-input"
            type="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Ex.: Rock in Rio, Teatro, Jazz..."
            disabled={isBusy}
            autoComplete="off"
          />
        </div>

        <button
          type="submit"
          className="catalog-search-submit-btn"
          disabled={isBusy}
        >
          {isBusy ? 'Buscando…' : 'Buscar referências'}
        </button>
      </form>

      <div className="sr-only" role="status" aria-live="polite" aria-atomic="true">
        {announcement}
      </div>

      {selectedReference !== null ? (
        <aside className="catalog-selected-feedback" role="status">
          <strong>Referência selecionada:</strong> {selectedReference.title}
          <span className="catalog-selected-hint"> (Criação de rascunho disponível na próxima etapa)</span>
        </aside>
      ) : null}

      <div className="catalog-search-results">
        {searchState.kind === 'loading' ? (
          <div className="catalog-search-loading" role="status">
            <p>Buscando referências no catálogo Ticketmaster…</p>
          </div>
        ) : null}

        {searchState.kind === 'empty' ? (
          <div className="catalog-search-empty" role="status">
            <p>Nenhum evento encontrado no catálogo para a busca realizada.</p>
          </div>
        ) : null}

        {searchState.kind === 'error' ? (
          <div className="catalog-search-error" role="alert">
            <p>{searchState.message}</p>
            <button
              type="button"
              className="catalog-search-retry-btn"
              onClick={handleRetry}
            >
              Tentar novamente
            </button>
          </div>
        ) : null}

        {searchState.kind === 'success' ? (
          <ul className="catalog-results-list" aria-label="Resultados do catálogo Ticketmaster">
            {searchState.events.map((event) => (
              <li key={event.externalId} className="catalog-results-item">
                <TicketmasterResultCard
                  event={event}
                  onSelectReference={handleSelect}
                  disabled={isBusy}
                />
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  );
}
