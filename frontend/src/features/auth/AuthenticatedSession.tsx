import { useState } from 'react';
import type { SessionUser } from '../../app/api/authApi';
import { TicketmasterSearch } from '../catalog';
import { DraftEventEditor, MyEventsList, type EventResponse } from '../events';

type AuthenticatedSessionProps = {
  user: SessionUser;
  busy: boolean;
  error?: string;
  onLogout: () => Promise<void>;
};

type OrganizerView = 'my-events' | 'catalog' | 'editor';

const roleLabels = {
  ORGANIZER: 'Organizador',
  CUSTOMER: 'Cliente',
  GATE: 'Portaria',
} as const;

export function AuthenticatedSession({ user, busy, error, onLogout }: AuthenticatedSessionProps) {
  const [organizerView, setOrganizerView] = useState<OrganizerView>('my-events');
  const [selectedEvent, setSelectedEvent] = useState<EventResponse | null>(null);

  return (
    <div className="session-view">
      <section aria-labelledby="session-title" aria-busy={busy}>
        <h2 id="session-title">Sessão atual</h2>
        <dl>
          <div>
            <dt>E-mail</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Papel</dt>
            <dd>{roleLabels[user.role]}</dd>
          </div>
        </dl>
        {error === undefined ? null : <p role="alert">{error}</p>}
        <button type="button" disabled={busy} onClick={() => void onLogout()}>
          {busy ? 'Saindo…' : 'Sair e trocar de conta'}
        </button>
      </section>

      {user.role === 'ORGANIZER' ? (
        organizerView === 'editor' && selectedEvent !== null ? (
          <DraftEventEditor
            event={selectedEvent}
            onBack={() => {
              setSelectedEvent(null);
              setOrganizerView('my-events');
            }}
            onEventUpdated={(updated) => setSelectedEvent(updated)}
            onEventDeleted={() => {
              setSelectedEvent(null);
              setOrganizerView('my-events');
            }}
          />
        ) : organizerView === 'catalog' ? (
          <div className="catalog-search-wrapper">
            <button
              type="button"
              className="catalog-back-to-list-btn"
              onClick={() => setOrganizerView('my-events')}
            >
              ← Voltar para Meus Eventos
            </button>
            <TicketmasterSearch
              onOpenDraft={(draft) => {
                setSelectedEvent(draft);
                setOrganizerView('editor');
              }}
            />
          </div>
        ) : (
          <MyEventsList
            onNewEvent={() => setOrganizerView('catalog')}
            onSelectEvent={(event) => {
              setSelectedEvent(event);
              setOrganizerView('editor');
            }}
          />
        )
      ) : null}
    </div>
  );
}
