import { useState } from 'react';
import type { SessionUser } from '../../app/api/authApi';
import { TicketmasterSearch } from '../catalog';
import {
  DraftEventEditor,
  MyEventsList,
  PublicEventCatalog,
  PublicEventDetail,
  type EventResponse,
  type PublicEventResponse,
} from '../events';

type AuthenticatedSessionProps = {
  user: SessionUser;
  busy: boolean;
  error?: string;
  onLogout: () => Promise<void>;
};

type OrganizerView = 'my-events' | 'catalog' | 'editor' | 'public-catalog' | 'public-detail';
type CustomerView = 'catalog' | 'detail';

const roleLabels = {
  ORGANIZER: 'Organizador',
  CUSTOMER: 'Cliente',
  GATE: 'Portaria',
} as const;

export function AuthenticatedSession({ user, busy, error, onLogout }: AuthenticatedSessionProps) {
  const [organizerView, setOrganizerView] = useState<OrganizerView>('my-events');
  const [customerView, setCustomerView] = useState<CustomerView>('catalog');
  const [selectedEvent, setSelectedEvent] = useState<EventResponse | null>(null);
  const [selectedPublicEvent, setSelectedPublicEvent] = useState<PublicEventResponse | null>(null);

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

      {user.role === 'CUSTOMER' ? (
        customerView === 'detail' && selectedPublicEvent !== null ? (
          <PublicEventDetail
            eventId={selectedPublicEvent.id}
            initialEvent={selectedPublicEvent}
            currentUser={user}
            onBackToCatalog={() => {
              setSelectedPublicEvent(null);
              setCustomerView('catalog');
            }}
          />
        ) : (
          <PublicEventCatalog
            onSelectEvent={(event) => {
              setSelectedPublicEvent(event);
              setCustomerView('detail');
            }}
          />
        )
      ) : user.role === 'ORGANIZER' ? (
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
        ) : organizerView === 'public-detail' && selectedPublicEvent !== null ? (
          <div className="public-catalog-wrapper">
            <PublicEventDetail
              eventId={selectedPublicEvent.id}
              initialEvent={selectedPublicEvent}
              currentUser={user}
              onBackToCatalog={() => {
                setSelectedPublicEvent(null);
                setOrganizerView('public-catalog');
              }}
            />
          </div>
        ) : organizerView === 'public-catalog' ? (
          <div className="public-catalog-wrapper">
            <button
              type="button"
              className="catalog-back-to-list-btn"
              onClick={() => setOrganizerView('my-events')}
            >
              ← Voltar para Meus Eventos
            </button>
            <PublicEventCatalog
              onSelectEvent={(event) => {
                setSelectedPublicEvent(event);
                setOrganizerView('public-detail');
              }}
            />
          </div>
        ) : (
          <div>
            <MyEventsList
              onNewEvent={() => setOrganizerView('catalog')}
              onSelectEvent={(event) => {
                setSelectedEvent(event);
                setOrganizerView('editor');
              }}
            />
            <div style={{ marginTop: '1.5rem', textAlign: 'center' }}>
              <button
                type="button"
                className="edt-button edt-button--secondary"
                onClick={() => setOrganizerView('public-catalog')}
              >
                Ver Catálogo Público de Eventos
              </button>
            </div>
          </div>
        )
      ) : null}
    </div>
  );
}
