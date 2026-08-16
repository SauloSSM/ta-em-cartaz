import { useEffect, useRef, useState } from 'react';
import type { ReservationResponse } from '../api/reservationsApi';
import {
  processPayment,
  generatePaymentAttemptId,
  PaymentClientError,
  type PaymentResponse,
} from '../../payments/api/paymentsApi';
import {
  getUncertainPayment,
  saveUncertainPayment,
  clearUncertainPayment,
  type StoredUncertainPayment,
} from '../../payments/model/uncertainPayment';
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
  onNavigateMyTickets?: () => void;
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
  onNavigateMyTickets,
  onReconcile,
}: CheckoutViewProps) {
  const [reservation, setReservation] = useState<ReservationResponse>(initialReservation);
  const [isExpiredLocally, setIsExpiredLocally] = useState<boolean>(
    initialReservation.status === 'EXPIRED',
  );

  // Recupera tentativa incerta pendente do sessionStorage (Story 5.3)
  const [uncertainAttempt, setUncertainAttempt] = useState<StoredUncertainPayment | null>(() => {
    return getUncertainPayment(initialReservation.id);
  });

  // Estados de processamento de pagamento simulado (Story 5.1, 5.2, 5.3)
  const [currentAttemptId, setCurrentAttemptId] = useState<string>(() => {
    const stored = getUncertainPayment(initialReservation.id);
    return stored ? stored.paymentAttemptId : generatePaymentAttemptId();
  });
  const [isProcessingPayment, setIsProcessingPayment] = useState<boolean>(false);
  const [lastPaymentAttempt, setLastPaymentAttempt] = useState<PaymentResponse | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [reconcileError, setReconcileError] = useState<string | null>(null);

  const expiredHeadingRef = useRef<HTMLHeadingElement>(null);
  const declinedAlertRef = useRef<HTMLHeadingElement>(null);
  const confirmedAlertRef = useRef<HTMLHeadingElement>(null);
  const verifyingAlertRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    setReservation(initialReservation);
    if (initialReservation.status === 'EXPIRED') {
      setIsExpiredLocally(true);
    }
    const storedUncertain = getUncertainPayment(initialReservation.id);
    if (storedUncertain) {
      setUncertainAttempt(storedUncertain);
      setCurrentAttemptId(storedUncertain.paymentAttemptId);
    }
  }, [initialReservation]);

  const handleExpire = () => {
    setIsExpiredLocally(true);
    setReservation((prev) => ({
      ...prev,
      status: 'EXPIRED',
    }));
  };

  const isConfirmed = reservation.status === 'CONFIRMED';
  const isExpired = !isConfirmed && (isExpiredLocally || reservation.status === 'EXPIRED');

  useEffect(() => {
    if (isExpired) {
      expiredHeadingRef.current?.focus();
    }
  }, [isExpired]);

  useEffect(() => {
    if (uncertainAttempt !== null && !isConfirmed && !isExpired) {
      verifyingAlertRef.current?.focus();
    } else if (lastPaymentAttempt?.status === 'DECLINED') {
      declinedAlertRef.current?.focus();
    } else if (lastPaymentAttempt?.status === 'APPROVED' || isConfirmed) {
      confirmedAlertRef.current?.focus();
    }
  }, [uncertainAttempt, lastPaymentAttempt, isConfirmed, isExpired]);

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
    if (onReconcile && initialReservation.status === 'HOLDING') {
      void handleReconcile();
    }
  }, []);

  const handleSimulatePayment = async (outcome: 'APPROVED' | 'DECLINED') => {
    if (isExpired || isConfirmed || isProcessingPayment || uncertainAttempt !== null) {
      return;
    }

    const attemptData: StoredUncertainPayment = {
      reservationId: reservation.id,
      paymentAttemptId: currentAttemptId,
      simulatedOutcome: outcome,
      timestamp: Date.now(),
    };

    // Armazena a tentativa em trânsito antes do envio (Story 5.3)
    saveUncertainPayment(attemptData);
    setIsProcessingPayment(true);
    setPaymentError(null);
    setReconcileError(null);

    try {
      const response = await processPayment(reservation.id, {
        paymentAttemptId: currentAttemptId,
        simulatedOutcome: outcome,
      });

      clearUncertainPayment(reservation.id);
      setUncertainAttempt(null);
      setLastPaymentAttempt(response);

      if (response.status === 'APPROVED') {
        setReservation((prev) => ({
          ...prev,
          status: 'CONFIRMED',
        }));
        if (onReconcile) {
          void onReconcile();
        }
      } else {
        // Para nova tentativa deliberada após recusa, gera novo attemptId (AD-9)
        setCurrentAttemptId(generatePaymentAttemptId());
      }
    } catch (err: unknown) {
      if (err instanceof PaymentClientError) {
        if (err.code === 'RESERVATION_EXPIRED') {
          clearUncertainPayment(reservation.id);
          setUncertainAttempt(null);
          handleExpire();
        } else if (err.code === 'PAYMENT_INVALID_RESPONSE') {
          // Formato inválido ou resposta técnica truncada -> estado de incerteza (Story 5.3)
          setUncertainAttempt(attemptData);
        } else {
          // Erros determinísticos de domínio (400, 401, 403, 409)
          clearUncertainPayment(reservation.id);
          setUncertainAttempt(null);
          setPaymentError(err.message);
        }
      } else {
        // Falha de rede / timeout / conexão interrompida -> estado de incerteza (Story 5.3)
        setUncertainAttempt(attemptData);
      }
    } finally {
      setIsProcessingPayment(false);
    }
  };

  const handleReconcilePayment = async () => {
    if (!uncertainAttempt || isProcessingPayment) {
      return;
    }

    setIsProcessingPayment(true);
    setReconcileError(null);

    try {
      const response = await processPayment(reservation.id, {
        paymentAttemptId: uncertainAttempt.paymentAttemptId,
        simulatedOutcome: uncertainAttempt.simulatedOutcome,
      });

      clearUncertainPayment(reservation.id);
      setUncertainAttempt(null);
      setLastPaymentAttempt(response);

      if (response.status === 'APPROVED') {
        setReservation((prev) => ({
          ...prev,
          status: 'CONFIRMED',
        }));
        if (onReconcile) {
          void onReconcile();
        }
      } else {
        // Para nova tentativa deliberada após recusa, gera novo attemptId (AD-9)
        setCurrentAttemptId(generatePaymentAttemptId());
      }
    } catch (err: unknown) {
      if (err instanceof PaymentClientError) {
        if (err.code === 'RESERVATION_EXPIRED') {
          clearUncertainPayment(reservation.id);
          setUncertainAttempt(null);
          handleExpire();
        } else if (err.code === 'PAYMENT_INVALID_RESPONSE') {
          setReconcileError('Não foi possível validar a resposta do servidor. Por favor, tente novamente.');
        } else {
          clearUncertainPayment(reservation.id);
          setUncertainAttempt(null);
          setPaymentError(err.message);
        }
      } else {
        setReconcileError('Não foi possível conectar ao servidor para verificar o resultado. Por favor, tente novamente.');
      }
    } finally {
      setIsProcessingPayment(false);
    }
  };

  return (
    <article
      className={`edt-checkout-view ${isExpired ? 'edt-checkout-view--expired' : ''} ${isConfirmed ? 'edt-checkout-view--confirmed' : ''}`}
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
          {isConfirmed
            ? 'Reserva Confirmada'
            : isExpired
              ? 'Reserva Expirada'
              : 'Checkout — Concluir Reserva'}
        </h1>
        <p className="edt-checkout-view__subtitle">
          Superfície S04 • Finalização de reserva de ingressos
        </p>
      </header>

      {/* Aviso obrigatório de ambiente de demonstração */}
      <DemoEnvironmentNotice />

      {/* Alerta de Confirmação quando o pagamento APPROVED é concluído */}
      {isConfirmed && (
        <section
          className="edt-alert edt-alert--success edt-checkout-view__confirmed-alert"
          role="alert"
          data-testid="checkout-confirmed-alert"
        >
          <h2
            ref={confirmedAlertRef}
            tabIndex={-1}
            className="edt-alert__title"
            data-testid="confirmed-alert-heading"
          >
            Pagamento Aprovado e Reserva Confirmada!
          </h2>
          <p className="edt-alert__desc">
            Seu pagamento foi processado com sucesso e os ingressos foram emitidos de forma autoritativa.
          </p>
          <div className="edt-payment-section__declined-meta">
            {lastPaymentAttempt && (
              <span>ID do pagamento: <code>{lastPaymentAttempt.id}</code></span>
            )}
            <span>ID da reserva: <code>{reservation.id}</code></span>
            <span>Status: <strong>CONFIRMED</strong></span>
          </div>
          <div className="edt-checkout-view__expired-actions" style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            {onNavigateMyTickets && (
              <button
                type="button"
                className="edt-button edt-button--primary"
                onClick={onNavigateMyTickets}
                data-testid="confirmed-go-to-my-tickets-btn"
              >
                Ver Meus Ingressos →
              </button>
            )}
            <button
              type="button"
              className={`edt-button ${onNavigateMyTickets ? 'edt-button--secondary' : 'edt-button--primary'}`}
              onClick={onBackToCatalog}
              data-testid="confirmed-back-to-catalog-btn"
            >
              Voltar ao Catálogo de Eventos
            </button>
          </div>
        </section>
      )}

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

      {/* Timer autoritativo da reserva (ativo apenas enquanto HOLDING e não expirado) */}
      {!isConfirmed && (
        <section aria-label="Tempo restante da reserva" className="edt-checkout-view__timer-section">
          <ReservationTimer
            expiresAt={reservation.expiresAt}
            serverNow={reservation.serverNow}
            status={reservation.status}
            onExpire={handleExpire}
            onReconcile={handleReconcile}
          />
        </section>
      )}

      <div className="edt-checkout-view__body">
        {/* Resumo com snapshots autoritativos */}
        <CheckoutSummary
          reservation={reservation}
          eventTitle={eventTitle}
          eventDate={eventDate}
          eventVenue={eventVenue}
          sectorName={sectorName}
        />

        {/* Seção de Pagamento Simulado (Story 5.1, 5.2, 5.3) */}
        {!isExpired && !isConfirmed && (
          <section
            className="edt-checkout-view__payment-section"
            aria-labelledby="payment-section-title"
            data-testid="payment-section"
          >
            <h3 id="payment-section-title" className="edt-payment-section__title">
              Pagamento Simulado (Demonstração)
            </h3>
            <p className="edt-payment-section__desc">
              Este sistema opera exclusivamente com simulação controlada via <strong>FakePaymentGateway</strong>. Nenhuma transação financeira real será cobrada em seus meios de pagamento.
            </p>

            {paymentError && (
              <div
                className="edt-alert edt-alert--danger edt-payment-section__alert"
                role="alert"
                data-testid="payment-error-alert"
              >
                <p className="edt-alert__desc">{paymentError}</p>
              </div>
            )}

            {/* Alerta de Verificação / Incerteza de Resposta (Story 5.3) */}
            {uncertainAttempt !== null && (
              <div
                className="edt-alert edt-alert--warning edt-payment-section__alert edt-payment-section__verifying-alert"
                role="alert"
                data-testid="payment-verifying-alert"
              >
                <h4
                  ref={verifyingAlertRef}
                  tabIndex={-1}
                  className="edt-alert__title"
                  data-testid="verifying-alert-heading"
                >
                  Verificando Situação do Pagamento
                </h4>
                <p className="edt-alert__desc">
                  Houve uma instabilidade de rede após o envio da sua tentativa. Para garantir que nenhuma cobrança duplicada seja efetuada, estamos prontos para consultar o resultado autoritativo da mesma tentativa.
                </p>
                <div className="edt-payment-section__declined-meta edt-payment-section__verifying-meta">
                  <span>ID da tentativa: <code>{uncertainAttempt.paymentAttemptId}</code></span>
                  <span>Resultado enviado: <strong>{uncertainAttempt.simulatedOutcome}</strong></span>
                </div>
                {reconcileError && (
                  <div className="edt-payment-section__reconcile-error" role="alert">
                    <p className="edt-alert__desc">{reconcileError}</p>
                  </div>
                )}
                <div className="edt-payment-section__verifying-actions">
                  <button
                    type="button"
                    className="edt-button edt-button--primary edt-button--large"
                    onClick={() => void handleReconcilePayment()}
                    disabled={isProcessingPayment}
                    aria-busy={isProcessingPayment}
                    data-testid="reconcile-payment-btn"
                  >
                    {isProcessingPayment ? 'Consultando resultado...' : 'Verificar Novamente'}
                  </button>
                </div>
              </div>
            )}

            {/* Alerta de Recusa Simulada (DECLINED) */}
            {uncertainAttempt === null && lastPaymentAttempt && lastPaymentAttempt.status === 'DECLINED' && (
              <div
                className="edt-alert edt-alert--warning edt-payment-section__alert"
                role="alert"
                data-testid="payment-declined-alert"
              >
                <h4
                  ref={declinedAlertRef}
                  tabIndex={-1}
                  className="edt-alert__title"
                  data-testid="declined-alert-heading"
                >
                  Tentativa de Pagamento Recusada
                </h4>
                <p className="edt-alert__desc">
                  O provedor simulado recusou a transação (motivo: <code>{lastPaymentAttempt.declineReason || 'SIMULATED_DECLINE'}</code>). Sua reserva permanece ativa enquanto houver tempo restante no cronômetro. Você pode tentar novamente.
                </p>
                <div className="edt-payment-section__declined-meta">
                  <span>ID da tentativa: <code>{lastPaymentAttempt.id}</code></span>
                  <span>Provedor: <strong>{lastPaymentAttempt.provider}</strong></span>
                </div>
              </div>
            )}

            {/* Botões de Ação de Pagamento Simulado (ocultos durante estado de verificação/reconciliação para evitar nova cobrança) */}
            {uncertainAttempt === null && (
              <div className="edt-payment-section__actions">
                <button
                  type="button"
                  className="edt-button edt-button--secondary edt-button--large"
                  onClick={() => handleSimulatePayment('DECLINED')}
                  disabled={isProcessingPayment || isExpired}
                  aria-busy={isProcessingPayment}
                  data-testid="simulate-declined-payment-btn"
                >
                  {isProcessingPayment
                    ? 'Processando...'
                    : lastPaymentAttempt?.status === 'DECLINED'
                      ? 'Tentar Novamente (Simular Recusa)'
                      : 'Simular Pagamento Recusado (DECLINED)'}
                </button>
                <button
                  type="button"
                  className="edt-button edt-button--primary edt-button--large"
                  onClick={() => handleSimulatePayment('APPROVED')}
                  disabled={isProcessingPayment || isExpired}
                  aria-busy={isProcessingPayment}
                  data-testid="simulate-approved-payment-btn"
                >
                  {isProcessingPayment ? 'Processando...' : 'Aprovar Pagamento (APPROVED)'}
                </button>
              </div>
            )}
          </section>
        )}
      </div>
    </article>
  );
}
