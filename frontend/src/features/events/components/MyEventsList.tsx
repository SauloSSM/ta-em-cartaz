import { useState, useEffect, useTransition } from 'react';
import { listMyEvents, deleteDraftEvent, type EventResponse } from '../api/eventsApi';
import { DeleteConfirmDialog } from './DeleteConfirmDialog';

type MyEventsListProps = {
  onSelectEvent: (event: EventResponse) => void;
  onNewEvent: () => void;
};

type ListState =
  | { kind: 'loading' }
  | { kind: 'empty' }
  | { kind: 'success'; events: EventResponse[] }
  | { kind: 'error'; message: string };

export function MyEventsList({ onSelectEvent, onNewEvent }: MyEventsListProps) {
  const [listState, setListState] = useState<ListState>({ kind: 'loading' });
  const [eventToDelete, setEventToDelete] = useState<EventResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [announcement, setAnnouncement] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  const fetchEvents = async () => {
    setListState({ kind: 'loading' });
    setAnnouncement('Carregando seus eventos…');

    try {
      const response = await listMyEvents();
      if (response.events.length === 0) {
        setListState({ kind: 'empty' });
        setAnnouncement('Nenhum evento cadastrado.');
      } else {
        setListState({ kind: 'success', events: response.events });
        setAnnouncement(`${response.events.length} eventos carregados.`);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Falha ao carregar lista de eventos.';
      setListState({ kind: 'error', message });
      setAnnouncement(`Erro: ${message}`);
    }
  };

  useEffect(() => {
    void fetchEvents();
  }, []);

  const handleDeleteConfirm = async () => {
    if (eventToDelete === null) return;

    setIsDeleting(true);
    try {
      await deleteDraftEvent(eventToDelete.id);
      startTransition(() => {
        setAnnouncement(`Rascunho do evento "${eventToDelete.title}" excluído com sucesso.`);
        if (listState.kind === 'success') {
          const remaining = listState.events.filter((e) => e.id !== eventToDelete.id);
          if (remaining.length === 0) {
            setListState({ kind: 'empty' });
          } else {
            setListState({ kind: 'success', events: remaining });
          }
        }
      });
      setEventToDelete(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir rascunho.';
      setAnnouncement(`Erro ao excluir: ${msg}`);
      setEventToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <section className="my-events-section" aria-labelledby="my-events-title">
      <header className="my-events-header">
        <div>
          <h2 id="my-events-title">Meus Eventos</h2>
          <p className="my-events-subtitle">
            Gerencie seus eventos em rascunho e acompanhe eventos publicados.
          </p>
        </div>
        <button
          type="button"
          className="my-events-new-btn"
          onClick={onNewEvent}
        >
          + Novo evento do catálogo
        </button>
      </header>

      <div className="sr-only" role="status" aria-live="polite" aria-atomic="true">
        {announcement}
      </div>

      <div className="my-events-content">
        {listState.kind === 'loading' ? (
          <div className="my-events-loading" role="status">
            <p>Carregando seus eventos…</p>
          </div>
        ) : null}

        {listState.kind === 'empty' ? (
          <div className="my-events-empty" role="status">
            <p>Você ainda não possui eventos cadastrados.</p>
            <button
              type="button"
              className="my-events-empty-cta"
              onClick={onNewEvent}
            >
              Buscar no catálogo Ticketmaster
            </button>
          </div>
        ) : null}

        {listState.kind === 'error' ? (
          <div className="my-events-error" role="alert">
            <p>{listState.message}</p>
            <button
              type="button"
              className="my-events-retry-btn"
              onClick={() => void fetchEvents()}
            >
              Tentar novamente
            </button>
          </div>
        ) : null}

        {listState.kind === 'success' ? (
          <ul className="my-events-list" aria-label="Lista dos meus eventos">
            {listState.events.map((item) => {
              const isDraft = item.status === 'DRAFT';
              return (
                <li key={item.id} className="my-events-item">
                  <article className="my-event-card">
                    <div className="my-event-card-header">
                      <div className="my-event-card-title-group">
                        <h3 className="my-event-card-title">{item.title}</h3>
                        <span
                          className={`event-status-badge ${isDraft ? 'status-draft' : 'status-published'}`}
                          aria-label={`Status: ${item.status}`}
                        >
                          {item.status}
                        </span>
                      </div>
                    </div>

                    <div className="my-event-card-meta">
                      {item.category ? (
                        <span className="my-event-card-tag">{item.category}</span>
                      ) : null}
                      {item.venueName ? (
                        <span className="my-event-card-venue">📍 {item.venueName}</span>
                      ) : null}
                      {item.startsAt ? (
                        <span className="my-event-card-date">
                          📅 {new Date(item.startsAt).toLocaleString('pt-BR')}
                        </span>
                      ) : null}
                      {item.externalId ? (
                        <small className="my-event-card-ref">Ref.: {item.externalId}</small>
                      ) : null}
                    </div>

                    <div className="my-event-card-actions">
                      <button
                        type="button"
                        className="my-event-edit-btn"
                        onClick={() => onSelectEvent(item)}
                        aria-label={`${isDraft ? 'Editar rascunho de' : 'Ver detalhes de'} ${item.title}`}
                      >
                        {isDraft ? 'Editar rascunho' : 'Ver detalhes'}
                      </button>

                      {isDraft ? (
                        <button
                          type="button"
                          className="my-event-delete-btn"
                          onClick={() => setEventToDelete(item)}
                          aria-label={`Excluir rascunho de ${item.title}`}
                        >
                          Excluir
                        </button>
                      ) : (
                        <span className="my-event-published-locked" aria-hidden="true">
                          Publicado
                        </span>
                      )}
                    </div>
                  </article>
                </li>
              );
            })}
          </ul>
        ) : null}
      </div>

      {eventToDelete !== null ? (
        <DeleteConfirmDialog
          isOpen={true}
          eventTitle={eventToDelete.title}
          busy={isDeleting}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setEventToDelete(null)}
        />
      ) : null}
    </section>
  );
}
