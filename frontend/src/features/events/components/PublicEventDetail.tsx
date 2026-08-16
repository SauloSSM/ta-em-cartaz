import { useState, useEffect, useCallback } from 'react';
import type { SessionUser } from '../../../app/api/authApi';
import {
  getEvent,
  listTicketSectors,
  EventClientError,
  type EventResponse,
  type PublicEventResponse,
  type TicketSectorResponse,
} from '../api/eventsApi';
import {
  savePurchaseIntention,
  type PurchaseIntention,
} from '../model/purchaseIntention';
import { QuantityStepper } from './QuantityStepper';
import { TicketSectorCard } from './TicketSectorCard';

export type PublicEventDetailProps = {
  eventId: string;
  initialEvent?: PublicEventResponse | EventResponse;
  onBackToCatalog: () => void;
  onProceedToLogin?: (noticeMessage: string) => void;
  currentUser?: SessionUser | null;
  onIntentionFormed?: (intention: PurchaseIntention) => void;
};

type DetailState =
  | { status: 'loading' }
  | { status: 'not-found'; message: string }
  | { status: 'forbidden'; message: string }
  | { status: 'error'; message: string }
  | {
      status: 'success';
      event: EventResponse | PublicEventResponse;
      sectors: TicketSectorResponse[];
      salesClosed: boolean;
    };

export function PublicEventDetail({
  eventId,
  initialEvent,
  onBackToCatalog,
  onProceedToLogin,
  currentUser,
  onIntentionFormed,
}: PublicEventDetailProps) {
  const [state, setState] = useState<DetailState>({ status: 'loading' });
  const [selectedSectorId, setSelectedSectorId] = useState<string | null>(null);
  const [quantity, setQuantity] = useState<number>(1);
  const [imageError, setImageError] = useState(false);
  const [intentionFeedback, setIntentionFeedback] = useState<string | null>(null);
  const [roleErrorMessage, setRoleErrorMessage] = useState<string | null>(null);

  const loadEventData = useCallback(async () => {
    setState({ status: 'loading' });
    setIntentionFeedback(null);
    setRoleErrorMessage(null);

    try {
      const [eventData, sectorsData] = await Promise.all([
        initialEvent && initialEvent.id === eventId
          ? Promise.resolve(initialEvent)
          : getEvent(eventId),
        listTicketSectors(eventId),
      ]);

      const isSalesClosed = computeSalesClosed(eventData);

      setState({
        status: 'success',
        event: eventData,
        sectors: sectorsData.sectors,
        salesClosed: isSalesClosed,
      });

      // Se houver setores disponíveis, seleciona o primeiro por padrão caso nenhum esteja selecionado
      if (sectorsData.sectors.length > 0 && !isSalesClosed) {
        const firstAvailable = sectorsData.sectors.find((s) => s.availableQuantity > 0);
        if (firstAvailable) {
          setSelectedSectorId(firstAvailable.id);
        }
      }
    } catch (err) {
      if (err instanceof EventClientError) {
        if (err.code === 'EVENT_NOT_FOUND') {
          setState({
            status: 'not-found',
            message: 'O evento solicitado não foi encontrado ou não está disponível.',
          });
          return;
        }
        if (err.code === 'EVENT_FORBIDDEN' || err.code === 'AUTH_FORBIDDEN') {
          setState({
            status: 'forbidden',
            message: 'Este evento está em modo rascunho e não está disponível publicamente.',
          });
          return;
        }
      }
      setState({
        status: 'error',
        message: 'Não foi possível carregar os detalhes do evento. Verifique sua conexão e tente novamente.',
      });
    }
  }, [eventId, initialEvent]);

  useEffect(() => {
    void loadEventData();
  }, [loadEventData]);

  if (state.status === 'loading') {
    return (
      <section className="edt-event-detail edt-event-detail--loading" aria-labelledby="detail-loading-title">
        <button
          type="button"
          className="edt-back-link"
          onClick={onBackToCatalog}
          aria-label="Voltar para o catálogo de eventos"
        >
          ← Voltar para o Catálogo
        </button>
        <h2 id="detail-loading-title" className="edt-visually-hidden">
          Carregando detalhes do evento
        </h2>
        <div className="edt-skeleton edt-skeleton--hero" aria-hidden="true" />
        <div className="edt-skeleton edt-skeleton--text" aria-hidden="true" />
        <div className="edt-skeleton edt-skeleton--card" aria-hidden="true" />
        <p role="status" className="edt-loading-text">
          Carregando detalhes do evento…
        </p>
      </section>
    );
  }

  if (state.status === 'not-found') {
    return (
      <section className="edt-event-detail edt-event-detail--error" aria-labelledby="detail-not-found-title">
        <h2 id="detail-not-found-title" className="edt-error-title">
          Evento não encontrado
        </h2>
        <p role="alert" className="edt-error-desc">
          {state.message}
        </p>
        <button
          type="button"
          className="edt-button edt-button--primary"
          onClick={onBackToCatalog}
        >
          ← Voltar para o Catálogo de Eventos
        </button>
      </section>
    );
  }

  if (state.status === 'forbidden') {
    return (
      <section className="edt-event-detail edt-event-detail--error" aria-labelledby="detail-forbidden-title">
        <h2 id="detail-forbidden-title" className="edt-error-title">
          Acesso Restrito
        </h2>
        <p role="alert" className="edt-error-desc">
          {state.message}
        </p>
        <button
          type="button"
          className="edt-button edt-button--primary"
          onClick={onBackToCatalog}
        >
          ← Voltar para o Catálogo de Eventos
        </button>
      </section>
    );
  }

  if (state.status === 'error') {
    return (
      <section className="edt-event-detail edt-event-detail--error" aria-labelledby="detail-error-title">
        <h2 id="detail-error-title" className="edt-error-title">
          Não foi possível exibir o evento
        </h2>
        <p role="alert" className="edt-error-desc">
          {state.message}
        </p>
        <div className="edt-error-actions">
          <button
            type="button"
            className="edt-button edt-button--primary"
            onClick={() => void loadEventData()}
          >
            Tentar novamente
          </button>
          <button
            type="button"
            className="edt-button edt-button--secondary"
            onClick={onBackToCatalog}
          >
            ← Voltar para o Catálogo
          </button>
        </div>
      </section>
    );
  }

  const { event, sectors, salesClosed } = state;
  const selectedSector = sectors.find((s) => s.id === selectedSectorId) ?? null;
  const isSectorAvailable = selectedSector !== null && selectedSector.availableQuantity > 0;
  const maxSelectable = selectedSector ? Math.min(6, selectedSector.availableQuantity) : 6;

  const formattedDate = event.startsAt
    ? formatEventDate(event.startsAt)
    : 'Data a confirmar';

  const hasImage = Boolean(event.imageUrl) && !imageError;

  const handleReserveClick = () => {
    if (!selectedSector || selectedSector.availableQuantity <= 0 || salesClosed) {
      return;
    }

    const intention = savePurchaseIntention({
      eventId: event.id,
      ticketSectorId: selectedSector.id,
      quantity,
      internalReturnPath: `/events/${event.id}`,
    });

    if (!currentUser) {
      const notice =
        'Para continuar com a compra dos seus ingressos, acesse sua conta de Cliente (CUSTOMER). Sua seleção foi guardada e a disponibilidade será revalidada após o login.';
      if (onProceedToLogin) {
        onProceedToLogin(notice);
      } else {
        setIntentionFeedback(notice);
      }
      return;
    }

    if (currentUser.role === 'CUSTOMER') {
      setIntentionFeedback(
        `Intenção de compra registrada com sucesso para o setor ${selectedSector.name} (${quantity} ${quantity === 1 ? 'ingresso' : 'ingressos'}). Prossiga para o checkout quando disponível.`,
      );
      if (onIntentionFormed) {
        onIntentionFormed(intention);
      }
      return;
    }

    // Usuário autenticado com outro papel (ORGANIZER ou GATE)
    const roleName = currentUser.role === 'ORGANIZER' ? 'Organizador' : 'Portaria';
    setRoleErrorMessage(
      `Sua conta atual possui papel de ${roleName}. Apenas contas com papel de Cliente (CUSTOMER) podem realizar reservas e compras de ingressos. Por favor, encerre a sessão atual e faça login com uma conta de Cliente para prosseguir.`,
    );
  };

  return (
    <article className="edt-event-detail" data-testid="public-event-detail" aria-labelledby="event-detail-title">
      <nav aria-label="Navegação secundária" className="edt-event-detail__nav">
        <button
          type="button"
          className="edt-back-link"
          onClick={onBackToCatalog}
          aria-label="Voltar para o catálogo de eventos"
        >
          ← Voltar para o Catálogo de Eventos
        </button>
      </nav>

      {/* Hero do Evento */}
      <header className="edt-event-detail__hero">
        <div className="edt-event-detail__banner-container">
          {hasImage ? (
            <img
              src={event.imageUrl}
              alt={`Banner do evento ${event.title}`}
              className="edt-event-detail__banner"
              onError={() => setImageError(true)}
            />
          ) : (
            <div className="edt-event-detail__banner-fallback" role="img" aria-label="Imagem padrão do evento">
              <svg
                className="edt-event-detail__fallback-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <rect x="3" y="4" width="18" height="16" rx="2" />
                <path d="M7 8h10M7 12h10M7 16h6" />
                <circle cx="16.5" cy="16.5" r="1.5" />
              </svg>
              <span>EliteDevTicket</span>
            </div>
          )}

          <div className="edt-event-detail__hero-badges">
            {event.category ? (
              <span className="edt-event-detail__badge edt-event-detail__badge--category">
                {event.category}
              </span>
            ) : null}
            {salesClosed ? (
              <span className="edt-event-detail__badge edt-event-detail__badge--sales-closed" role="status">
                Vendas encerradas
              </span>
            ) : null}
          </div>
        </div>

        <div className="edt-event-detail__header-content">
          <h1 id="event-detail-title" className="edt-event-detail__title">
            {event.title}
          </h1>

          <div className="edt-event-detail__metadata" aria-label="Informações sobre data e local">
            <div className="edt-event-detail__meta-item">
              <svg
                className="edt-event-detail__meta-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              <div>
                <strong>Data e Horário</strong>
                <p>{formattedDate}</p>
              </div>
            </div>

            {(event.venueName || event.venueAddress) && (
              <div className="edt-event-detail__meta-item">
                <svg
                  className="edt-event-detail__meta-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
                <div>
                  <strong>Local</strong>
                  <p>
                    {event.venueName}
                    {event.venueName && event.venueAddress ? ' — ' : ''}
                    {event.venueAddress}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Descrição do Evento */}
      {event.description ? (
        <section className="edt-event-detail__description-section" aria-labelledby="desc-heading">
          <h2 id="desc-heading" className="edt-section-title">
            Sobre o Evento
          </h2>
          <p className="edt-event-detail__description-text">{event.description}</p>
        </section>
      ) : null}

      {/* Alerta de Vendas Encerradas */}
      {salesClosed && (
        <div className="edt-alert edt-alert--warning" role="alert" data-testid="sales-closed-alert">
          <h2 className="edt-alert__title">Vendas Encerradas</h2>
          <p className="edt-alert__desc">
            As vendas para este evento foram encerradas pois o horário de início já foi atingido.
            A visualização das informações do evento permanece disponível.
          </p>
        </div>
      )}

      {/* Alerta de Incompatibilidade de Papel */}
      {roleErrorMessage && (
        <div className="edt-alert edt-alert--danger" role="alert" data-testid="role-error-alert">
          <h2 className="edt-alert__title">Conta incompatível para compra</h2>
          <p className="edt-alert__desc">{roleErrorMessage}</p>
        </div>
      )}

      {/* Feedback de Intenção Registrada */}
      {intentionFeedback && (
        <div className="edt-alert edt-alert--success" role="status" data-testid="intention-success-alert">
          <h2 className="edt-alert__title">Intenção de Compra</h2>
          <p className="edt-alert__desc">{intentionFeedback}</p>
        </div>
      )}

      {/* Painel de Compra e Setores */}
      <section className="edt-event-detail__purchase-section" aria-labelledby="sectors-heading">
        <h2 id="sectors-heading" className="edt-section-title">
          Setores e Ingressos
        </h2>

        {sectors.length === 0 ? (
          <p className="edt-empty-text">Nenhum setor de ingressos disponível para este evento.</p>
        ) : (
          <div className="edt-event-detail__sectors-grid" role="radiogroup" aria-labelledby="sectors-heading">
            {sectors.map((sector) => (
              <TicketSectorCard
                key={sector.id}
                sector={sector}
                selected={selectedSectorId === sector.id}
                disabled={salesClosed}
                onSelect={(sec) => {
                  setSelectedSectorId(sec.id);
                  setQuantity(1);
                  setIntentionFeedback(null);
                  setRoleErrorMessage(null);
                }}
              />
            ))}
          </div>
        )}

        {/* Seleção de Quantidade e Resumo de Preço */}
        {selectedSector && !salesClosed && (
          <div className="edt-event-detail__checkout-box" data-testid="purchase-intention-box">
            <h3 className="edt-checkout-box__title">Escolha a quantidade</h3>

            {isSectorAvailable ? (
              <>
                <QuantityStepper
                  value={quantity}
                  min={1}
                  max={maxSelectable}
                  onChange={(val) => {
                    setQuantity(val);
                    setIntentionFeedback(null);
                    setRoleErrorMessage(null);
                  }}
                />

                <div className="edt-price-summary" aria-label="Resumo estimado de compra">
                  <div className="edt-price-summary__row">
                    <span>Setor selecionado:</span>
                    <strong>{selectedSector.name}</strong>
                  </div>
                  <div className="edt-price-summary__row">
                    <span>Preço unitário:</span>
                    <span>{formatCurrency(selectedSector.price)}</span>
                  </div>
                  <div className="edt-price-summary__row">
                    <span>Quantidade:</span>
                    <span>{quantity}</span>
                  </div>
                  <div className="edt-price-summary__total">
                    <span>Valor estimado total:</span>
                    <strong className="edt-price-summary__total-value">
                      {formatCurrency(selectedSector.price * quantity)}
                    </strong>
                  </div>
                  <p className="edt-price-summary__disclaimer">
                    Estimativa calculada localmente. O valor final e a disponibilidade serão confirmados pelo servidor ao iniciar a reserva.
                  </p>
                </div>

                <div className="edt-event-detail__cta-container">
                  <button
                    type="button"
                    className="edt-button edt-button--primary edt-button--large edt-event-detail__reserve-btn"
                    onClick={handleReserveClick}
                    aria-label={`Reservar ${quantity} ${quantity === 1 ? 'ingresso' : 'ingressos'} no setor ${selectedSector.name}`}
                  >
                    Reservar Ingressos →
                  </button>
                  <p className="edt-event-detail__cta-hint">
                    A escolha do setor e quantidade forma uma intenção de compra. A reserva com garantia de tempo será criada após a validação.
                  </p>
                </div>
              </>
            ) : (
              <p className="edt-sold-out-warning" role="alert">
                O setor selecionado ({selectedSector.name}) está esgotado. Por favor, escolha outro setor com ingressos disponíveis.
              </p>
            )}
          </div>
        )}
      </section>
    </article>
  );
}

function computeSalesClosed(event: EventResponse | PublicEventResponse): boolean {
  if ('salesClosed' in event && typeof event.salesClosed === 'boolean') {
    return event.salesClosed;
  }
  if (!event.startsAt) {
    return false;
  }
  const startsAtMs = new Date(event.startsAt).getTime();
  if (Number.isNaN(startsAtMs)) {
    return false;
  }
  return Date.now() >= startsAtMs;
}

function formatEventDate(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
      return 'Data a confirmar';
    }
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'America/Sao_Paulo',
    }).format(date);
  } catch {
    return 'Data a confirmar';
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}
