import { useState, useId } from 'react';
import type { MyTicketResponse } from '../api/ticketsApi';
import { QRCodePanel } from './QRCodePanel';

export type TicketDetailProps = {
  ticket: MyTicketResponse;
  eventTitle?: string;
  eventDate?: string;
  eventVenue?: string;
  eventAddress?: string;
  sectorName?: string;
  onBackToList: () => void;
};

export function TicketDetail({
  ticket,
  eventTitle,
  eventDate,
  eventVenue,
  eventAddress,
  sectorName,
  onBackToList,
}: TicketDetailProps) {
  const [shareFeedback, setShareFeedback] = useState<string | null>(null);
  const shareStatusId = useId();

  const isUsed = ticket.status === 'USED';
  const statusLabel = isUsed ? 'Utilizado' : 'Válido';

  const handleShare = async () => {
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    const shareUrl = `${origin}/t/${ticket.shareToken}`;

    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: `Ingresso - ${eventTitle || 'Evento'}`,
          text: `Acesse o ingresso para ${eventTitle || 'Evento'}:`,
          url: shareUrl,
        });
        setShareFeedback('Link de compartilhamento enviado com sucesso!');
        setTimeout(() => setShareFeedback(null), 3000);
        return;
      } catch {
        // Fallback para cópia em caso de cancelamento ou falha
      }
    }

    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareUrl);
      }
      setShareFeedback('Link permanente de compartilhamento copiado!');
      setTimeout(() => setShareFeedback(null), 3000);
    } catch {
      setShareFeedback('Não foi possível copiar o link de compartilhamento automaticamente.');
    }
  };

  return (
    <article className="edt-ticket-detail" data-testid="ticket-detail-view">
      <div className="edt-ticket-detail__navigation">
        <button
          type="button"
          className="edt-button edt-button--secondary edt-ticket-detail__back-btn"
          onClick={onBackToList}
          data-testid="back-to-tickets-btn"
        >
          ← Voltar para Meus Ingressos
        </button>
      </div>

      <header className="edt-ticket-detail__header">
        <div className="edt-ticket-detail__meta-top">
          <span className="edt-ticket-detail__unit-tag">Ingresso #{ticket.ordinal}</span>
          <span
            className={`edt-status-badge ${
              isUsed ? 'edt-status-badge--used' : 'edt-status-badge--valid'
            }`}
            data-testid="ticket-detail-status-badge"
          >
            {statusLabel}
          </span>
        </div>
        <h2 className="edt-ticket-detail__title" data-testid="ticket-detail-title">
          {eventTitle || 'Show / Evento'}
        </h2>
        <div className="edt-ticket-detail__event-info">
          {sectorName && (
            <p className="edt-ticket-detail__info-line">
              <strong>Setor:</strong> {sectorName}
            </p>
          )}
          {eventDate && (
            <p className="edt-ticket-detail__info-line">
              <strong>Data e Hora:</strong>{' '}
              {new Date(eventDate).toLocaleDateString('pt-BR', {
                weekday: 'long',
                day: '2-digit',
                month: 'long',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
          )}
          {(eventVenue || eventAddress) && (
            <p className="edt-ticket-detail__info-line">
              <strong>Local:</strong> {[eventVenue, eventAddress].filter(Boolean).join(' — ')}
            </p>
          )}
        </div>
      </header>

      {/* AC: Estado textual precede a credencial quando usado */}
      {isUsed && (
        <div
          className="edt-alert edt-alert--warning edt-ticket-detail__used-notice"
          role="status"
          data-testid="ticket-used-notice"
        >
          <h3 className="edt-alert__title">Ingresso Utilizado</h3>
          <p className="edt-alert__desc">
            Este ingresso já foi validado na portaria e <strong>não autoriza nova entrada</strong>. Suas credenciais permanecem visíveis para conferência histórica.
          </p>
        </div>
      )}

      <section className="edt-ticket-detail__credentials-section" aria-label="Credenciais de Acesso">
        <QRCodePanel
          validationToken={ticket.validationToken}
          manualCode={ticket.manualCode}
        />
      </section>

      <section className="edt-ticket-detail__share-section" aria-label="Compartilhamento">
        <h3 className="edt-ticket-detail__section-title">Compartilhar com outra pessoa</h3>
        <p className="edt-ticket-detail__share-desc">
          Envie o link público permanente para que outra pessoa possa apresentar este ingresso na entrada. O link não transfere a titularidade nem expõe seus dados pessoais.
        </p>
        <div className="edt-ticket-detail__share-actions">
          <button
            type="button"
            className="edt-button edt-button--secondary edt-ticket-detail__share-btn"
            onClick={() => void handleShare()}
            aria-describedby={shareStatusId}
            data-testid="share-ticket-btn"
          >
            Compartilhar Ingresso (Copiar Link)
          </button>
        </div>
        <div
          id={shareStatusId}
          className="edt-ticket-detail__share-status sr-only"
          role="status"
          aria-live="polite"
        >
          {shareFeedback}
        </div>
        {shareFeedback && (
          <p className="edt-ticket-detail__share-feedback" aria-hidden="true">
            {shareFeedback}
          </p>
        )}
      </section>
    </article>
  );
}
