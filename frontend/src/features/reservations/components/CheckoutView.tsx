import { useEffect, useRef, useState } from 'react';
import type { ReservationResponse } from '../api/reservationsApi';
import { ReservationTimer } from './ReservationTimer';
import { CheckoutSummary } from './CheckoutSummary';
import { DemoEnvironmentNotice } from './DemoEnvironmentNotice';
import './CheckoutView.css';

export type CheckoutViewProps = {
  reservation: ReservationResponse;
  eventTitle?: string;
  eventDate?: string;
  eventVenue?: string;
  sectorName?: string;
  onBackToEvent: () => void;
  onBackToCatalog: () => void;
  onReconcile?: () => Promise<ReservationResponse | null | void> | void;
};

export function CheckoutView({
  reservation: initialReservation,
  eventTitle,
  eventDate,
  eventVenue,
  sectorName,
  onBackToEvent,
  onBackToCatalog,
  onReconcile,
}: CheckoutViewProps) {
  const [reservation, setReservation] = useState<ReservationResponse>(initialReservation);
  const [isExpiredLocally, setIsExpiredLocally] = useState<boolean>(
    initialReservation.status === 'EXPIRED',
  );

  const expiredHeadingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    setReservation(initialReservation);
    if (initialReservation.status === 'EXPIRED') {
      setIsExpiredLocally(true);
    }
  }, [initialReservation]);

  const handleExpire = () => {
    setIsExpiredLocally(true);
    setReservation((prev) => ({
      ...prev,
      status: 'EXPIRED',
    }));
  };

  useEffect(() => {
    if (isExpiredLocally) {
      // Move o foco para a mensagem persistente de expiração para acessibilidade
      expiredHeadingRef.current?.focus();
    }
  }, [isExpiredLocally]);

  const handleReconcile = async () => {
    if (onReconcile) {
      const result = await onReconcile();
      if (result && typeof result === 'object' && 'status' in result) {
        setReservation(result);
        if (result.status === 'EXPIRED') {
          setIsExpiredLocally(true);
        }
      }
    }
  };

  useEffect(() => {
    // Reconcilia com o backend imediatamente no mount para que reload/restauração obtenha o serverNow/expiresAt autoritativo
    if (onReconcile && initialReservation.status === 'HOLDING') {
      void handleReconcile();
    }
  }, []);

  const isExpired = isExpiredLocally || reservation.status === 'EXPIRED';

  return (
    <article
      className={`edt-checkout-view ${isExpired ? 'edt-checkout-view--expired' : ''}`}
      aria-labelledby="checkout-page-title"
      data-testid="checkout-view"
    >
      <nav aria-label="Navegação secundária do checkout" className="edt-checkout-view__nav">
        <button
          type="button"
          className="edt-back-link"
          onClick={onBackToEvent}
          aria-label="Voltar para o evento"
        >
          ← Voltar para o Evento
        </button>
        <button
          type="button"
          className="edt-back-link"
          onClick={onBackToCatalog}
          aria-label="Voltar para o catálogo de eventos"
        >
          Ir para o Catálogo
        </button>
      </nav>

      <header className="edt-checkout-view__header">
        <h1 id="checkout-page-title" className="edt-checkout-view__title">
          {isExpired ? 'Reserva Expirada' : 'Checkout — Concluir Reserva'}
        </h1>
        <p className="edt-checkout-view__subtitle">
          Superfície S04 • Finalização de reserva de ingressos
        </p>
      </header>

      {/* Aviso obrigatório de ambiente de demonstração */}
      <DemoEnvironmentNotice />

      {/* Alerta de Expiração quando o hold vence */}
      {isExpired && (
        <section
          className="edt-alert edt-alert--danger edt-checkout-view__expired-alert"
          role="alert"
          data-testid="checkout-expired-alert"
        >
          <h2
            ref={expiredHeadingRef}
            tabIndex={-1}
            className="edt-alert__title"
            data-testid="expired-alert-heading"
          >
            O tempo da sua reserva expirou
          </h2>
          <p className="edt-alert__desc">
            O período de garantia de 10 minutos para esta reserva foi encerrado. Os ingressos foram liberados e devolvidos ao estoque do evento.
          </p>
          <div className="edt-checkout-view__expired-actions">
            <button
              type="button"
              className="edt-button edt-button--primary"
              onClick={onBackToEvent}
              data-testid="return-to-event-btn"
            >
              Escolher Ingressos Novamente →
            </button>
            <button
              type="button"
              className="edt-button edt-button--secondary"
              onClick={onBackToCatalog}
            >
              Voltar ao Catálogo de Eventos
            </button>
          </div>
        </section>
      )}

      {/* Timer autoritativo da reserva */}
      <section aria-label="Tempo restante da reserva" className="edt-checkout-view__timer-section">
        <ReservationTimer
          expiresAt={reservation.expiresAt}
          serverNow={reservation.serverNow}
          status={reservation.status}
          onExpire={handleExpire}
          onReconcile={handleReconcile}
        />
      </section>

      <div className="edt-checkout-view__body">
        {/* Resumo com snapshots autoritativos */}
        <CheckoutSummary
          reservation={reservation}
          eventTitle={eventTitle}
          eventDate={eventDate}
          eventVenue={eventVenue}
          sectorName={sectorName}
        />

        {/* Bloco de Pagamento Simulador (Desabilitado/Placeholder funcional para Story 4.5) */}
        {!isExpired && (
          <section
            className="edt-checkout-view__payment-placeholder"
            aria-labelledby="payment-section-title"
            data-testid="payment-placeholder-section"
          >
            <h3 id="payment-section-title" className="edt-payment-placeholder__title">
              Pagamento Simulado
            </h3>
            <p className="edt-payment-placeholder__desc">
              Sua reserva está garantida enquanto o cronômetro estiver ativo. A etapa de simulação e processamento de pagamento será executada a seguir.
            </p>
            <div className="edt-payment-placeholder__actions">
              <button
                type="button"
                className="edt-button edt-button--primary edt-button--large"
                disabled
                title="A simulação de pagamento estará disponível no próximo fluxo (Epic 5)"
                aria-disabled="true"
                data-testid="proceed-payment-placeholder-btn"
              >
                Avançar para Pagamento Simulado (Epic 5)
              </button>
            </div>
          </section>
        )}
      </div>
    </article>
  );
}
