import { useState, useCallback, useRef, useEffect } from 'react';
import type { GateEvent, ValidateTicketResponse } from '../api/gateApi';
import { validateTicket, GateClientError } from '../api/gateApi';
import { GateQrScanner } from './GateQrScanner';
import { GateManualValidation } from './GateManualValidation';

// --- Types ---

type ValidationMode = 'qr' | 'manual';

type GateValidationAreaProps = {
  selectedEvent: GateEvent;
  onAnnouncement?: (message: string) => void;
};

// --- Result card (shared between QR and Manual modes) ---

function getOutcomeDetails(result: string) {
  switch (result) {
    case 'VALID':
      return {
        icon: '✓',
        badgeText: 'LIBERADO',
        title: 'Ingresso Válido',
        instruction: 'Entrada autorizada. O ingresso foi registrado e consumido com sucesso.',
        announcement: 'Resultado: Ingresso Válido. Entrada liberada.',
      };
    case 'INVALID':
      return {
        icon: '✕',
        badgeText: 'RECUSADO',
        title: 'Ingresso Inválido',
        instruction: 'Código não encontrado ou não reconhecido. Entrada não autorizada.',
        announcement: 'Resultado: Ingresso Inválido. Código não reconhecido.',
      };
    case 'ALREADY_USED':
      return {
        icon: '⚠',
        badgeText: 'JÁ UTILIZADO',
        title: 'Ingresso Já Utilizado',
        instruction: 'Este ingresso já foi validado e consumido anteriormente. Entrada não autorizada.',
        announcement: 'Resultado: Ingresso Já Utilizado. Entrada não autorizada.',
      };
    case 'WRONG_EVENT':
      return {
        icon: '⊘',
        badgeText: 'OUTRO EVENTO',
        title: 'Evento Incorreto',
        instruction: 'Este ingresso pertence a outro evento. Não foi consumido nesta portaria.',
        announcement: 'Resultado: Evento Incorreto. Ingresso pertence a outro evento. Não foi consumido.',
      };
    default:
      return {
        icon: '?',
        badgeText: 'DESCONHECIDO',
        title: 'Resultado Desconhecido',
        instruction: 'Não foi possível processar a validação.',
        announcement: 'Resultado desconhecido.',
      };
  }
}

type GateResultCardProps = {
  result: ValidateTicketResponse;
  mode: ValidationMode;
  onNext: () => void;
};

function GateResultCard({ result, mode, onNext }: GateResultCardProps) {
  const outcomeDetails = getOutcomeDetails(result.result);
  const resultHeadingRef = useRef<HTMLHeadingElement>(null);

  // Move focus to heading when result appears (EXPERIENCE.md §8)
  useEffect(() => {
    if (resultHeadingRef.current) {
      resultHeadingRef.current.focus();
    }
  }, []);

  return (
    <div
      className={`edt-gate-result-card edt-gate-result-card--${result.result.toLowerCase()}`}
      data-testid={`gate-result-${result.result.toLowerCase()}`}
      role="region"
      aria-label="Resultado da Validação"
    >
      <div className="edt-gate-result-card__header">
        <span className="edt-gate-result-card__icon" aria-hidden="true">
          {outcomeDetails.icon}
        </span>
        <span className="edt-gate-result-card__badge">{outcomeDetails.badgeText}</span>
      </div>

      <h4
        ref={resultHeadingRef}
        tabIndex={-1}
        className="edt-gate-result-card__title"
        data-testid="gate-result-title"
      >
        {outcomeDetails.title}
      </h4>

      <p className="edt-gate-result-card__instruction" data-testid="gate-result-instruction">
        {outcomeDetails.instruction}
      </p>

      <div className="edt-gate-result-card__meta">
        <span>
          <strong>Horário:</strong>{' '}
          {new Date(result.processedAt).toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
          })}
        </span>
        <span>
          <strong>Método:</strong> {mode === 'qr' ? 'QR Code' : 'Manual'}
        </span>
      </div>

      <div className="edt-gate-result-card__actions">
        <button
          type="button"
          className="edt-gate-btn edt-gate-btn--primary"
          onClick={onNext}
          data-testid="gate-next-validation-btn"
        >
          Validar próximo ingresso
        </button>
      </div>
    </div>
  );
}

// --- Main Orchestrator ---

export function GateValidationArea({ selectedEvent, onAnnouncement }: GateValidationAreaProps) {
  const [mode, setMode] = useState<ValidationMode>('qr');
  const [isValidating, setIsValidating] = useState(false);
  const [result, setResult] = useState<ValidateTicketResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  // Stable ref for the current selectedEvent id — used by QR decode handler
  const selectedEventIdRef = useRef(selectedEvent.id);
  selectedEventIdRef.current = selectedEvent.id;

  // Keeps the current attemptId so network errors can retry with the same ID
  const currentAttemptIdRef = useRef<string | null>(null);

  // When selectedEvent changes, clear result, reset to QR mode, and clear any pending state
  const prevEventIdRef = useRef(selectedEvent.id);
  useEffect(() => {
    if (prevEventIdRef.current !== selectedEvent.id) {
      prevEventIdRef.current = selectedEvent.id;
      setResult(null);
      setErrorMessage(null);
      setIsValidating(false);
      setMode('qr');
      currentAttemptIdRef.current = null;
    }
  }, [selectedEvent.id]);

  // --- QR decode handler ---
  // Called by GateQrScanner when a QR code is successfully read.
  // The decoded value IS the validationToken — sent as manualCode per existing OpenAPI contract.
  const handleQrDecode = useCallback(
    async (validationToken: string) => {
      // Guard: do not start a new request while one is in flight
      if (isValidating || result !== null) return;

      setIsValidating(true);
      setErrorMessage(null);

      // Generate a fresh attemptId for this new scan
      const validationAttemptId = crypto.randomUUID();
      currentAttemptIdRef.current = validationAttemptId;

      try {
        const response = await validateTicket({
          validationAttemptId,
          selectedEventId: selectedEventIdRef.current,
          manualCode: validationToken, // QR payload is validationToken — sent as manualCode
          method: 'QR',
        });

        setResult(response);

        if (onAnnouncement) {
          const details = getOutcomeDetails(response.result);
          onAnnouncement(details.announcement);
        }
      } catch (err) {
        let msg = 'Erro de comunicação ao validar ingresso. Nenhum ingresso foi consumido.';
        if (err instanceof GateClientError) {
          msg = err.message || msg;
        }
        setErrorMessage(msg);
        if (onAnnouncement) {
          onAnnouncement(`Erro ao validar: ${msg}`);
        }
      } finally {
        setIsValidating(false);
        currentAttemptIdRef.current = null;
      }
    },
    [isValidating, result, onAnnouncement],
  );

  // --- "Next ticket" handler ---
  // Clears result and resumes scanner
  const handleNext = useCallback(() => {
    setResult(null);
    setErrorMessage(null);
    setIsValidating(false);
    currentAttemptIdRef.current = null;
    if (onAnnouncement) {
      onAnnouncement(
        mode === 'qr'
          ? 'Pronto para próxima validação. Aponte a câmera para o próximo QR Code.'
          : 'Pronto para próxima validação. Digite o código manual.',
      );
    }
  }, [mode, onAnnouncement]);

  // --- Render ---

  // When there is a result, show the result card (above the mode switcher)
  if (result !== null) {
    return (
      <>
        {/* Announce result once via assertive live region (EXPERIENCE.md §8) */}
        <div
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
          className="sr-only"
          data-testid="gate-result-announcement"
        >
          {getOutcomeDetails(result.result).announcement}
        </div>
        <GateResultCard result={result} mode={mode} onNext={handleNext} />
      </>
    );
  }

  return (
    <div className="edt-gate-validation-area" data-testid="gate-validation-area">
      {/* Network error banner */}
      {errorMessage && (
        <div
          className="edt-gate-error-banner"
          role="alert"
          data-testid="gate-error-banner"
        >
          <span aria-hidden="true">⚠️</span>
          <div>
            <p className="edt-gate-error-banner__msg">{errorMessage}</p>
            <p className="edt-gate-error-banner__hint">
              Nenhum ingresso foi consumido. Verifique a conexão e tente novamente.
            </p>
          </div>
        </div>
      )}

      {/* QR Scanner (primary path) */}
      {mode === 'qr' && (
        <GateQrScanner
          paused={isValidating}
          onDecode={handleQrDecode}
          onSwitchToManual={() => {
            setErrorMessage(null);
            setMode('manual');
          }}
        />
      )}

      {/* Manual fallback (always available per AD-16) */}
      {mode === 'manual' && (
        <div data-testid="gate-manual-mode-container">
          {/* Button to return to QR mode */}
          <div className="edt-gate-mode-switcher">
            <button
              type="button"
              className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
              onClick={() => {
                setErrorMessage(null);
                setMode('qr');
              }}
              data-testid="gate-switch-to-qr-btn"
            >
              ← Usar câmera QR
            </button>
          </div>
          <GateManualValidation
            key={selectedEvent.id}
            selectedEvent={selectedEvent}
            onAnnouncement={onAnnouncement}
          />
        </div>
      )}
    </div>
  );
}
