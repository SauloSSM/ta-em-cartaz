import { useState } from 'react';
import type { SessionUser } from '../../../app/api/authApi';
import type { GateEvent } from '../api/gateApi';
import { GateContextSelector } from './GateContextSelector';
import './gate.css';

export type GateViewProps = {
  user: SessionUser;
  initialSelectedEvent?: GateEvent | null;
  onEventChange?: (event: GateEvent | null) => void;
};

export function GateView({
  user,
  initialSelectedEvent = null,
  onEventChange,
}: GateViewProps) {
  const [selectedEvent, setSelectedEvent] = useState<GateEvent | null>(initialSelectedEvent);
  const [announcement, setAnnouncement] = useState<string>('');

  const handleSelectEvent = (event: GateEvent) => {
    setSelectedEvent(event);
    setAnnouncement(`Evento selecionado: ${event.title}`);
    if (onEventChange) {
      onEventChange(event);
    }
  };

  const handleClearSelection = () => {
    setSelectedEvent(null);
    setAnnouncement('Seleção de evento cancelada. Escolha um novo evento.');
    if (onEventChange) {
      onEventChange(null);
    }
  };

  const formattedSelectedDate = selectedEvent?.startsAt
    ? new Date(selectedEvent.startsAt).toLocaleDateString('pt-BR', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : null;

  return (
    <div className="edt-gate-root" data-testid="gate-view-root">
      {/* Live Region for Screen Reader Announcements */}
      <div
        className="sr-only"
        role="status"
        aria-live="polite"
        aria-atomic="true"
        style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0,0,0,0)' }}
      >
        {announcement}
      </div>

      {/* Header with Portaria Badge */}
      <header className="edt-gate-header" role="region" aria-label="Cabeçalho da Portaria">
        <div className="edt-gate-header__badge-row">
          <span className="edt-gate-badge">
            <span aria-hidden="true">🔒</span> Controle de Portaria
          </span>
          <span className="edt-gate-operator" aria-label={`Operador logado: ${user.email}`}>
            {user.email}
          </span>
        </div>
        <h2 className="edt-gate-header__title">Validação de Ingressos</h2>
        <p className="edt-gate-header__subtitle">
          Ambiente operacional de portaria para verificação de entrada em tempo real.
        </p>
      </header>

      {/* Selected Event Prominent Banner */}
      {selectedEvent ? (
        <section
          aria-labelledby="gate-selected-event-title"
          className="edt-gate-selected-banner"
          data-testid="gate-selected-event-banner"
        >
          <span className="edt-gate-selected-banner__label">Evento em Operação</span>
          <h3 id="gate-selected-event-title" className="edt-gate-selected-banner__title">
            {selectedEvent.title}
          </h3>

          <div className="edt-gate-selected-banner__meta">
            {formattedSelectedDate && (
              <span><strong>Data/Hora:</strong> {formattedSelectedDate}</span>
            )}
            {(selectedEvent.venueName || selectedEvent.venueAddress) && (
              <span>
                <strong>Local:</strong> {[selectedEvent.venueName, selectedEvent.venueAddress].filter(Boolean).join(' — ')}
              </span>
            )}
            {selectedEvent.category && (
              <span><strong>Categoria:</strong> {selectedEvent.category}</span>
            )}
          </div>

          <div className="edt-gate-selected-banner__actions">
            <button
              type="button"
              className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
              onClick={handleClearSelection}
              aria-label="Trocar evento selecionado"
              data-testid="gate-change-event-btn"
            >
              Trocar evento
            </button>
          </div>

          {/* Operational Area Placeholder for Stories 7.2–7.4 */}
          <div className="edt-gate-operational-placeholder" data-testid="gate-operational-ready">
            <p className="edt-gate-operational-placeholder__status">
              ✓ Evento configurado para conferência
            </p>
            <p className="edt-gate-operational-placeholder__hint">
              Contexto operacional ativo ({selectedEvent.id}). Pronto para validação de ingressos.
            </p>
          </div>
        </section>
      ) : (
        /* Event Selection Area */
        <GateContextSelector
          selectedEventId={null}
          onSelectEvent={handleSelectEvent}
        />
      )}
    </div>
  );
}
