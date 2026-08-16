import { useState, useEffect } from 'react';
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
import {
  getPurchaseIntention,
  clearPurchaseIntention,
} from '../events/model/purchaseIntention';
import {
  createReservation,
  ReservationClientError,
  type ReservationResponse,
} from '../reservations';

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
  const [customerView, setCustomerView] = useState<CustomerView>(() => {
    return user.role === 'CUSTOMER' && getPurchaseIntention() !== null ? 'detail' : 'catalog';
  });
  const [selectedEvent, setSelectedEvent] = useState<EventResponse | null>(null);
  const [selectedPublicEvent, setSelectedPublicEvent] = useState<PublicEventResponse | null>(null);
  const [activeReservation, setActiveReservation] = useState<ReservationResponse | null>(null);
  const [restorationError, setRestorationError] = useState<string | null>(null);
  const [isRestoringIntention, setIsRestoringIntention] = useState(false);
  const [restoredEventId, setRestoredEventId] = useState<string | null>(() => {
    return user.role === 'CUSTOMER' ? (getPurchaseIntention()?.eventId ?? null) : null;
  });

  useEffect(() => {
    if (user.role !== 'CUSTOMER') {
      return;
    }

    const intention = getPurchaseIntention();
    if (!intention) {
      return;
    }

    let active = true;
    setIsRestoringIntention(true);

    void createReservation(
      intention.eventId,
      intention.ticketSectorId,
      {
        quantity: intention.quantity,
      },
      intention.idempotencyKey,
    )
      .then((reservation) => {
        if (!active) return;
        clearPurchaseIntention();
        setActiveReservation(reservation);
        setRestoredEventId(intention.eventId);
        setCustomerView('detail');
      })
      .catch((err) => {
        if (!active) return;
        clearPurchaseIntention();
        let message = 'Não foi possível concluir sua reserva automaticamente.';
        if (err instanceof ReservationClientError) {
          if (err.code === 'INSUFFICIENT_AVAILABILITY') {
            message =
              'Não foi possível concluir sua reserva automaticamente: a quantidade solicitada não está mais disponível no setor selecionado.';
          } else if (err.code === 'SALES_CLOSED') {
            message =
              'Não foi possível concluir sua reserva automaticamente: as vendas para este evento foram encerradas.';
          } else if (err.code === 'EVENT_NOT_PUBLISHED') {
            message =
              'Não foi possível concluir sua reserva automaticamente: este evento não está mais disponível para vendas.';
          } else if (err.message) {
            message = `Não foi possível concluir sua reserva automaticamente: ${err.message}`;
          }
        }
        setRestorationError(message);
        setRestoredEventId(intention.eventId);
        setCustomerView('detail');
      })
      .finally(() => {
        if (active) {
          setIsRestoringIntention(false);
        }
      });

    return () => {
      active = false;
    };
  }, [user.role]);

  const activeEventId = selectedPublicEvent?.id ?? restoredEventId;

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

      {isRestoringIntention && (
        <div
          className="edt-alert edt-alert--info"
          role="status"
          aria-live="polite"
          data-testid="restoring-intention-indicator"
          style={{ margin: '1rem 0' }}
        >
          <p>Restaurando sua seleção de ingressos e confirmando disponibilidade com o servidor…</p>
        </div>
      )}

      {user.role === 'CUSTOMER' ? (
        customerView === 'detail' && activeEventId !== null ? (
          <PublicEventDetail
            eventId={activeEventId}
            initialEvent={selectedPublicEvent ?? undefined}
            initialReservation={activeReservation}
            initialErrorMessage={restorationError}
            currentUser={user}
            onBackToCatalog={() => {
              setSelectedPublicEvent(null);
              setRestoredEventId(null);
              setActiveReservation(null);
              setRestorationError(null);
              setCustomerView('catalog');
            }}
          />
        ) : (
          <PublicEventCatalog
            onSelectEvent={(event) => {
              setSelectedPublicEvent(event);
              setRestoredEventId(null);
              setActiveReservation(null);
              setRestorationError(null);
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
