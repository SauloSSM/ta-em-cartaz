import { useState, useRef, useEffect } from 'react';
import type { GateEvent, ValidateTicketResponse } from '../api/gateApi';
import { validateTicket, GateClientError } from '../api/gateApi';

export type GateManualValidationProps = {
  selectedEvent: GateEvent;
  onAnnouncement?: (message: string) => void;
};

export function GateManualValidation({
  selectedEvent,
  onAnnouncement,
}: GateManualValidationProps) {
  const [manualCode, setManualCode] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [result, setResult] = useState<ValidateTicketResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);
  const resultHeadingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (result && resultHeadingRef.current) {
      resultHeadingRef.current.focus();
    }
  }, [result]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanCode = manualCode.trim();
    if (!cleanCode || isSubmitting) {
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const validationAttemptId = crypto.randomUUID();

    try {
      const response = await validateTicket({
        validationAttemptId,
        selectedEventId: selectedEvent.id,
        manualCode: cleanCode,
        method: 'MANUAL',
      });

      setResult(response);

      if (onAnnouncement) {
        switch (response.result) {
          case 'VALID':
            onAnnouncement('Resultado: Ingresso Válido. Entrada liberada.');
            break;
          case 'INVALID':
            onAnnouncement('Resultado: Ingresso Inválido. Código não reconhecido.');
            break;
          case 'ALREADY_USED':
            onAnnouncement('Resultado: Ingresso Já Utilizado. Entrada não autorizada.');
            break;
          case 'WRONG_EVENT':
            onAnnouncement('Resultado: Evento Incorreto. Ingresso pertence a outro evento.');
            break;
        }
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
      setIsSubmitting(false);
    }
  };

  const handleNextValidation = () => {
    setResult(null);
    setManualCode('');
    setErrorMessage(null);
    if (onAnnouncement) {
      onAnnouncement('Pronto para próxima validação. Digite o código manual.');
    }
    setTimeout(() => {
      inputRef.current?.focus();
    }, 50);
  };

  if (result) {
    const outcomeDetails = getOutcomeDetails(result.result);

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
          <span className="edt-gate-result-card__badge">
            {outcomeDetails.badgeText}
          </span>
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
            <strong>Método:</strong> Manual
          </span>
        </div>

        <div className="edt-gate-result-card__actions">
          <button
            type="button"
            className="edt-gate-btn edt-gate-btn--primary"
            onClick={handleNextValidation}
            data-testid="gate-next-validation-btn"
          >
            Validar próximo ingresso
          </button>
        </div>
      </div>
    );
  }

  return (
    <section
      className="edt-gate-manual-section"
      aria-labelledby="gate-manual-heading"
      data-testid="gate-manual-section"
    >
      <h4 id="gate-manual-heading" className="edt-gate-manual-section__title">
        Entrada de Código Manual
      </h4>
      <p id="gate-manual-code-hint" className="edt-gate-manual-section__subtitle">
        Digite o código do ingresso para validar a entrada no evento atual.
      </p>

      <form onSubmit={handleSubmit} className="edt-gate-manual-form" noValidate>
        <div className="edt-gate-form-group">
          <label htmlFor="gate-manual-code-input" className="edt-gate-form-label">
            Código do Ingresso
          </label>
          <input
            id="gate-manual-code-input"
            ref={inputRef}
            type="text"
            className="edt-gate-code-input"
            data-testid="gate-manual-code-input"
            value={manualCode}
            onChange={(e) => setManualCode(e.target.value)}
            placeholder="Ex: 0123-4567-89"
            autoCapitalize="characters"
            autoCorrect="off"
            spellCheck={false}
            disabled={isSubmitting}
            aria-describedby="gate-manual-code-hint"
            required
          />
        </div>

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

        <button
          type="submit"
          className="edt-gate-btn edt-gate-btn--primary edt-gate-btn--full"
          disabled={isSubmitting || !manualCode.trim()}
          data-testid="gate-validate-btn"
        >
          {isSubmitting ? 'Validando ingresso...' : 'Validar código'}
        </button>
      </form>
    </section>
  );
}

function getOutcomeDetails(result: string) {
  switch (result) {
    case 'VALID':
      return {
        icon: '✓',
        badgeText: 'LIBERADO',
        title: 'Ingresso Válido',
        instruction: 'Entrada autorizada. O ingresso foi registrado e consumido com sucesso.',
      };
    case 'INVALID':
      return {
        icon: '✕',
        badgeText: 'RECUSADO',
        title: 'Ingresso Inválido',
        instruction: 'Código não encontrado ou não reconhecido. Entrada não autorizada.',
      };
    case 'ALREADY_USED':
      return {
        icon: '⚠',
        badgeText: 'JÁ UTILIZADO',
        title: 'Ingresso Já Utilizado',
        instruction: 'Este ingresso já foi validado e consumido anteriormente. Entrada não autorizada.',
      };
    case 'WRONG_EVENT':
      return {
        icon: '⊘',
        badgeText: 'OUTRO EVENTO',
        title: 'Evento Incorreto',
        instruction: 'Este ingresso pertence a outro evento. Não foi consumido nesta portaria.',
      };
    default:
      return {
        icon: '?',
        badgeText: 'DESCONHECIDO',
        title: 'Resultado Desconhecido',
        instruction: 'Não foi possível processar a validação.',
      };
  }
}
