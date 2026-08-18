import { useState, useTransition, type FormEvent } from 'react';
import { searchCatalogEvents, type CatalogEventReference } from '../api/catalogApi';
import { createDraftEvent, type EventResponse } from '../../events/api/eventsApi';
import { TicketmasterResultCard } from './TicketmasterResultCard';

import './TicketmasterSearch.css';
type TicketmasterSearchProps = {
  onSelectReference?: (event: CatalogEventReference) => void;
  onDraftCreated?: (event: EventResponse) => void;
  onOpenDraft?: (event: EventResponse) => void;
};

type SearchState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'empty'; query: string }
  | { kind: 'success'; events: CatalogEventReference[]; query: string }
  | { kind: 'error'; message: string; query: string };

export function TicketmasterSearch({
  onSelectReference,
  onDraftCreated,
  onOpenDraft,
}: TicketmasterSearchProps) {
  const [keyword, setKeyword] = useState('');
  const [searchState, setSearchState] = useState<SearchState>({ kind: 'idle' });
  const [selectedReference, setSelectedReference] = useState<CatalogEventReference | null>(null);
  const [createdDraft, setCreatedDraft] = useState<EventResponse | null>(null);
  const [creatingExternalId, setCreatingExternalId] = useState<string | null>(null);
  const [creationError, setCreationError] = useState<string | null>(null);
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

  const handleSelectAndCreateDraft = async (event: CatalogEventReference) => {
    setCreatingExternalId(event.externalId);
    setCreationError(null);
    setAnnouncement(`Criando rascunho a partir de "${event.title}"…`);

    try {
      const draft = await createDraftEvent(
        event.title,
        event.externalId,
        event.description,
        event.imageUrl,
        event.category,
      );

      startTransition(() => {
        setSelectedReference(event);
        setCreatedDraft(draft);
        setAnnouncement(`Rascunho criado com sucesso para o evento "${draft.title}".`);
      });

      if (onDraftCreated !== undefined) {
        onDraftCreated(draft);
      }
      if (onSelectReference !== undefined) {
        onSelectReference(event);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Falha ao criar rascunho de evento.';
      setCreationError(message);
      setAnnouncement(`Erro ao criar rascunho: ${message}`);
    } finally {
      setCreatingExternalId(null);
    }
  };

  const isBusy = searchState.kind === 'loading' || creatingExternalId !== null;

  return (
    <section className="catalog-search-section" aria-labelledby="catalog-search-title">
      <header className="catalog-search-header">
        <span className="catalog-search-kicker" aria-hidden="true">ORGANIZADOR / CRIAÇÃO</span>
        <h2 id="catalog-search-title">Novo Evento</h2>
        <p className="catalog-search-subtitle">
          Comece por uma referência da Ticketmaster. Depois você define data, local, setores e publicação.
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
          {searchState.kind === 'loading' ? 'Buscando…' : 'Buscar referências'}
        </button>
      </form>

      <div className="sr-only" role="status" aria-live="polite" aria-atomic="true">
        {announcement}
      </div>

      {creationError !== null ? (
        <div className="catalog-search-error" role="alert">
          <p>{creationError}</p>
        </div>
      ) : null}

      {createdDraft !== null ? (
        <aside className="catalog-selected-feedback" role="status">
          <div className="draft-created-banner">
            <strong>Rascunho criado com sucesso:</strong> {createdDraft.title}
            <span className="event-status-badge status-draft-mini">DRAFT</span>
            {onOpenDraft !== undefined ? (
              <button
                type="button"
                className="draft-open-btn"
                onClick={() => onOpenDraft(createdDraft)}
              >
                Abrir rascunho no editor →
              </button>
            ) : null}
          </div>
        </aside>
      ) : selectedReference !== null ? (
        <aside className="catalog-selected-feedback" role="status">
          <strong>Referência selecionada:</strong> {selectedReference.title}
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
                  onSelectReference={handleSelectAndCreateDraft}
                  disabled={isBusy}
                  isLoading={creatingExternalId === event.externalId}
                />
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  );
}
