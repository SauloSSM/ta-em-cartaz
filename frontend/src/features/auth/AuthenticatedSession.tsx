import { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
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
  ActiveReservationBanner,
  CheckoutView,
  saveActiveHold,
  getActiveHold,
  clearActiveHold,
} from '../reservations';
import {
  MyTicketsList,
  TicketDetail,
  type MyTicketResponse,
} from '../tickets';
import { GateView } from '../gate';
import './AuthenticatedSession.css';

type AuthenticatedSessionProps = {
  user: SessionUser;
  busy: boolean;
  error?: string;
  onLogout?: () => Promise<void>;
};

type OrganizerView = 'my-events' | 'catalog' | 'editor' | 'public-catalog' | 'public-detail';
type CustomerView = 'catalog' | 'detail' | 'checkout' | 'my-tickets' | 'ticket-detail';

const roleLabels = {
  ORGANIZER: 'Organizador',
  CUSTOMER: 'Cliente',
  GATE: 'Portaria',
} as const;

export function AuthenticatedSession({ user, busy, error }: AuthenticatedSessionProps) {
  const initialStoredHold = user.role === 'CUSTOMER' ? getActiveHold() : null;

  const [organizerView, setOrganizerView] = useState<OrganizerView>('my-events');
  const [customerView, setCustomerView] = useState<CustomerView>(() => {
    if (user.role === 'CUSTOMER') {
      if (getPurchaseIntention() !== null) {
        return 'detail';
      }
      if (initialStoredHold && initialStoredHold.reservation.status === 'HOLDING') {
        const expiresMs = new Date(initialStoredHold.reservation.expiresAt).getTime();
        const serverNowMs = new Date(initialStoredHold.reservation.serverNow).getTime();
        if (expiresMs > serverNowMs) {
          return 'checkout';
        }
      }
    }
    return 'catalog';
  });

  const [selectedEvent, setSelectedEvent] = useState<EventResponse | null>(null);
  const [selectedPublicEvent, setSelectedPublicEvent] = useState<PublicEventResponse | null>(null);
  const [selectedTicket, setSelectedTicket] = useState<MyTicketResponse | null>(null);
  const [selectedTicketMeta, setSelectedTicketMeta] = useState<{ event?: PublicEventResponse; sectorName?: string } | null>(null);
  const [activeReservation, setActiveReservation] = useState<ReservationResponse | null>(() => {
    if (!initialStoredHold) return null;
    const expiresMs = new Date(initialStoredHold.reservation.expiresAt).getTime();
    const serverNowMs = new Date(initialStoredHold.reservation.serverNow).getTime();
    if (expiresMs <= serverNowMs) {
      clearActiveHold();
      return null;
    }
    return initialStoredHold.reservation;
  });

  const [activeHoldMeta, setActiveHoldMeta] = useState<{
    eventTitle?: string;
    sectorName?: string;
    eventDate?: string;
    eventVenue?: string;
  } | null>(() => {
    if (!initialStoredHold) return null;
    return {
      eventTitle: initialStoredHold.eventTitle,
      sectorName: initialStoredHold.sectorName,
      eventDate: initialStoredHold.eventDate,
      eventVenue: initialStoredHold.eventVenue,
    };
  });

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
        saveActiveHold({
          reservation,
          eventTitle: selectedPublicEvent?.title,
          eventDate: selectedPublicEvent?.startsAt,
          eventVenue: selectedPublicEvent?.venueName,
        });
        setActiveHoldMeta({
          eventTitle: selectedPublicEvent?.title,
          eventDate: selectedPublicEvent?.startsAt,
          eventVenue: selectedPublicEvent?.venueName,
        });
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
  }, [user.role, selectedPublicEvent]);

  const activeEventId = selectedPublicEvent?.id ?? restoredEventId ?? activeReservation?.eventId ?? null;

  const handleReconcile = useCallback(async () => {
    if (!activeReservation || user.role !== 'CUSTOMER') {
      return;
    }
    try {
      const reconciled = await createReservation(
        activeReservation.eventId,
        activeReservation.sectorId,
        { quantity: activeReservation.quantity },
        `reconcile-${activeReservation.id}`,
      );
      setActiveReservation(reconciled);
      saveActiveHold({
        reservation: reconciled,
        eventTitle: selectedPublicEvent?.title ?? activeHoldMeta?.eventTitle,
        eventDate: selectedPublicEvent?.startsAt ?? activeHoldMeta?.eventDate,
        eventVenue: selectedPublicEvent?.venueName ?? activeHoldMeta?.eventVenue,
        sectorName: activeHoldMeta?.sectorName,
      });
      return reconciled;
    } catch (err) {
      if (
        err instanceof ReservationClientError &&
        (err.code === 'RESERVATION_EXPIRED' ||
          err.code === 'INSUFFICIENT_AVAILABILITY' ||
          err.code === 'SALES_CLOSED')
      ) {
        const expiredRes: ReservationResponse = {
          ...activeReservation,
          status: 'EXPIRED',
        };
        setActiveReservation(expiredRes);
        clearActiveHold();
        return expiredRes;
      }
    }
  }, [activeReservation, user.role, selectedPublicEvent, activeHoldMeta]);

  const [customerNavSlot, setCustomerNavSlot] = useState<HTMLElement | null>(null);

  useEffect(() => {
    setCustomerNavSlot(document.getElementById('tc-header-customer-nav-slot'));
  }, []);

  const customerNavMarkup = (
    <nav aria-label="Navegação do cliente" className="customer-nav customer-nav--header tc-header__customer-nav">
      <button
        type="button"
        className={`edt-button ${customerView === 'catalog' || customerView === 'detail' ? 'edt-button--primary' : 'edt-button--secondary'}`}
        onClick={() => {
          setSelectedPublicEvent(null);
          setRestoredEventId(null);
          setRestorationError(null);
          setCustomerView('catalog');
        }}
        data-testid="customer-nav-catalog-btn"
      >
        Eventos em Cartaz
      </button>
      <button
        type="button"
        className={`edt-button ${customerView === 'my-tickets' || customerView === 'ticket-detail' ? 'edt-button--primary' : 'edt-button--secondary'}`}
        onClick={() => {
          setSelectedTicket(null);
          setCustomerView('my-tickets');
        }}
        data-testid="customer-nav-my-tickets-btn"
      >
        Meus Ingressos
      </button>
    </nav>
  );

  return (
    <div className={`session-view session-view--${user.role.toLowerCase()}`}>
      <section className="session-view__account-strip" aria-labelledby="session-title" aria-busy={busy}>
        <h2 id="session-title" className="session-view__account-title">Sessão atual</h2>

        {user.role === 'CUSTOMER' ? (
          customerNavSlot ? (
            createPortal(customerNavMarkup, customerNavSlot)
          ) : (
            customerNavMarkup
          )
        ) : (
          <div />
        )}

        <dl className="session-view__account-meta">
          <div>
            <dt>E-mail</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Papel</dt>
            <dd>{roleLabels[user.role]}</dd>
          </div>
          {error === undefined ? null : <div className="session-view__account-error" role="alert">{error}</div>}
        </dl>
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
        <>

          {/* Banner de acesso persistente "Continuar reserva" quando houver hold ativo nas telas de catálogo e detalhe */}
          {customerView !== 'checkout' && activeReservation && activeReservation.status === 'HOLDING' && (
            <ActiveReservationBanner
              reservation={activeReservation}
              eventTitle={selectedPublicEvent?.title ?? activeHoldMeta?.eventTitle}
              sectorName={activeHoldMeta?.sectorName}
              onContinue={() => setCustomerView('checkout')}
              onExpire={() => {
                setActiveReservation(null);
                clearActiveHold();
              }}
            />
          )}

          {customerView === 'ticket-detail' && selectedTicket !== null ? (
            <TicketDetail
              ticket={selectedTicket}
              eventTitle={selectedTicketMeta?.event?.title}
              eventDate={selectedTicketMeta?.event?.startsAt}
              eventVenue={selectedTicketMeta?.event?.venueName}
              eventAddress={selectedTicketMeta?.event?.venueAddress}
              eventImageUrl={selectedTicketMeta?.event?.imageUrl}
              sectorName={selectedTicketMeta?.sectorName}
              onBackToList={() => {
                setSelectedTicket(null);
                setCustomerView('my-tickets');
              }}
            />
          ) : customerView === 'my-tickets' ? (
            <MyTicketsList
              onSelectTicket={(ticket, meta) => {
                setSelectedTicket(ticket);
                setSelectedTicketMeta(meta || null);
                setCustomerView('ticket-detail');
              }}
              onBrowseCatalog={() => {
                setCustomerView('catalog');
              }}
            />
          ) : customerView === 'checkout' && activeReservation !== null ? (
            <CheckoutView
              reservation={activeReservation}
              eventTitle={selectedPublicEvent?.title ?? activeHoldMeta?.eventTitle}
              eventDate={selectedPublicEvent?.startsAt ?? activeHoldMeta?.eventDate}
              eventVenue={selectedPublicEvent?.venueName ?? activeHoldMeta?.eventVenue}
              sectorName={activeHoldMeta?.sectorName}
              onBackToEvent={() => {
                if (activeEventId) {
                  setCustomerView('detail');
                } else {
                  setCustomerView('catalog');
                }
              }}
              onBackToCatalog={() => {
                setCustomerView('catalog');
              }}
              onNavigateMyTickets={() => {
                setCustomerView('my-tickets');
              }}
              onReconcile={handleReconcile}
            />
          ) : customerView === 'detail' && activeEventId !== null ? (
            <PublicEventDetail
              eventId={activeEventId}
              initialEvent={selectedPublicEvent ?? undefined}
              initialReservation={activeReservation}
              initialErrorMessage={restorationError}
              currentUser={user}
              onBackToCatalog={() => {
                setSelectedPublicEvent(null);
                setRestoredEventId(null);
                setRestorationError(null);
                setCustomerView('catalog');
              }}
              onReservationCreated={(reservation) => {
                setActiveReservation(reservation);
                saveActiveHold({
                  reservation,
                  eventTitle: selectedPublicEvent?.title,
                  eventDate: selectedPublicEvent?.startsAt,
                  eventVenue: selectedPublicEvent?.venueName,
                });
                setActiveHoldMeta({
                  eventTitle: selectedPublicEvent?.title,
                  eventDate: selectedPublicEvent?.startsAt,
                  eventVenue: selectedPublicEvent?.venueName,
                });
              }}
              onNavigateCheckout={(reservation) => {
                setActiveReservation(reservation);
                setCustomerView('checkout');
              }}
            />
          ) : (
            <PublicEventCatalog
              onSelectEvent={(event) => {
                setSelectedPublicEvent(event);
                setRestoredEventId(null);
                setRestorationError(null);
                setCustomerView('detail');
              }}
            />
          )}
        </>
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
      ) : user.role === 'GATE' ? (
        <GateView user={user} />
      ) : null}
    </div>
  );
}
