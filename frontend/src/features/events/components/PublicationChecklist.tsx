import { type FormEvent } from 'react';
import type { EventResponse, TicketSectorResponse } from '../api/eventsApi';

export type PublicationChecklistProps = {
  event: EventResponse;
  sectors: TicketSectorResponse[];
  onFocusField?: (fieldId: string) => void;
  onPublish: () => Promise<void>;
  isPublishing: boolean;
  publishedSuccess: boolean;
  error?: string | null;
};

type ChecklistItem = {
  id: string;
  label: string;
  mandatory: boolean;
  isValid: boolean;
  message: string;
  targetId: string;
};

export function PublicationChecklist({
  event,
  sectors,
  onFocusField,
  onPublish,
  isPublishing,
  publishedSuccess,
  error,
}: PublicationChecklistProps) {
  const isDraft = event.status === 'DRAFT';
  const isPublished = event.status === 'PUBLISHED' || publishedSuccess;

  // Validações obrigatórias autoritativas
  const hasExternalSource = Boolean(event.externalSource && event.externalSource.trim().length > 0);
  const hasExternalId = Boolean(event.externalId && event.externalId.trim().length > 0);
  const hasValidExternalReference = hasExternalSource && hasExternalId;

  const hasTitle = Boolean(event.title && event.title.trim().length > 0);

  const parsedStartsAt = event.startsAt ? new Date(event.startsAt) : null;
  const isStartsAtValid = Boolean(
    parsedStartsAt &&
    !isNaN(parsedStartsAt.getTime()) &&
    parsedStartsAt.getTime() > Date.now()
  );

  const hasVenueName = Boolean(event.venueName && event.venueName.trim().length > 0);
  const hasVenueAddress = Boolean(event.venueAddress && event.venueAddress.trim().length > 0);

  const hasSectors = sectors.length > 0;
  const areSectorsValid =
    hasSectors &&
    sectors.every(
      (s) => s.name.trim().length > 0 && s.capacity > 0 && s.price >= 0 && s.availableQuantity >= 0
    );

  const checklistItems: ChecklistItem[] = [
    {
      id: 'item-external-reference',
      label: 'Referência Externa',
      mandatory: true,
      isValid: hasValidExternalReference,
      message: hasValidExternalReference
        ? `Referência vinculada: ${event.externalSource}:${event.externalId}`
        : 'Origem e identificador da referência externa são obrigatórios.',
      targetId: 'event-title-input',
    },
    {
      id: 'item-title',
      label: 'Título do Evento',
      mandatory: true,
      isValid: hasTitle,
      message: hasTitle
        ? `Título definido: "${event.title}"`
        : 'O título do evento é obrigatório e não pode estar em branco.',
      targetId: 'event-title-input',
    },
    {
      id: 'item-starts-at',
      label: 'Data e Hora Futura',
      mandatory: true,
      isValid: isStartsAtValid,
      message: isStartsAtValid
        ? `Data e hora futura válida: ${parsedStartsAt?.toLocaleString('pt-BR')}`
        : event.startsAt
        ? 'A data e hora de início deve estar no futuro.'
        : 'Data e hora de início não informada.',
      targetId: 'event-starts-at-input',
    },
    {
      id: 'item-venue-name',
      label: 'Nome do Local',
      mandatory: true,
      isValid: hasVenueName,
      message: hasVenueName
        ? `Nome do local informado: "${event.venueName}"`
        : 'O nome do local do evento é obrigatório para publicação.',
      targetId: 'event-venue-name-input',
    },
    {
      id: 'item-venue-address',
      label: 'Endereço do Local',
      mandatory: true,
      isValid: hasVenueAddress,
      message: hasVenueAddress
        ? `Endereço informado: "${event.venueAddress}"`
        : 'O endereço do local do evento é obrigatório para publicação.',
      targetId: 'event-venue-address-input',
    },
    {
      id: 'item-sectors',
      label: 'Setores de Ingressos',
      mandatory: true,
      isValid: areSectorsValid,
      message: areSectorsValid
        ? `${sectors.length} setor(es) de ingressos válido(s) configurado(s).`
        : !hasSectors
        ? 'Pelo menos um setor de ingressos deve ser cadastrado antes da publicação.'
        : 'Todos os setores devem possuir nome, capacidade maior que zero e preço válido.',
      targetId: 'add-sector-btn',
    },
  ];

  const mandatoryPendingItems = checklistItems.filter((item) => item.mandatory && !item.isValid);
  const isReadyToPublish = mandatoryPendingItems.length === 0 && isDraft;

  const handleFocusClick = (targetId: string) => {
    if (onFocusField) {
      onFocusField(targetId);
    }
    const element = document.getElementById(targetId);
    if (element) {
      element.focus();
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  const handlePublishSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!isReadyToPublish || isPublishing) {
      return;
    }
    await onPublish();
  };

  return (
    <section
      className="publication-checklist-section"
      aria-labelledby="publication-checklist-title"
      aria-busy={isPublishing}
    >
      <header className="publication-checklist-header">
        <h3 id="publication-checklist-title">Revisão e Publicação do Evento</h3>
        <p className="publication-checklist-subtitle">
          Verifique se todos os requisitos obrigatórios foram preenchidos antes de publicar o evento para venda.
        </p>
      </header>

      {error ? (
        <div className="publication-feedback error" role="alert">
          <p>{error}</p>
        </div>
      ) : null}

      {isPublished ? (
        <div className="publication-feedback success" role="status" aria-live="polite">
          <p>
            <strong>Evento Publicado!</strong> Este evento está no estado <code>PUBLISHED</code> e pronto para venda.
            Campos estruturais foram protegidos contra alterações acidentais.
          </p>
        </div>
      ) : isReadyToPublish ? (
        <div className="publication-feedback ready" role="status" aria-live="polite">
          <p>
            <strong>Tudo pronto!</strong> Todas as condições obrigatórias foram atendidas. Você já pode publicar o evento.
          </p>
        </div>
      ) : (
        <div className="publication-feedback warning" role="status" aria-live="polite">
          <p>
            Existem <strong>{mandatoryPendingItems.length}</strong> pendência(s) obrigatória(s) que impedem a publicação.
          </p>
        </div>
      )}

      <ul className="publication-checklist-list" role="list" aria-label="Lista de verificação de publicação">
        {checklistItems.map((item) => (
          <li
            key={item.id}
            className={`checklist-item ${item.isValid ? 'item-valid' : 'item-pending'}`}
          >
            <div className="checklist-item-status-icon" aria-hidden="true">
              {item.isValid ? '✓' : '⚠️'}
            </div>
            <div className="checklist-item-info">
              <span className="checklist-item-title">
                {item.label}
                <span className="checklist-mandatory-tag"> (Obrigatório)</span>
              </span>
              <p className="checklist-item-message">{item.message}</p>
            </div>
            {!item.isValid && !isPublished ? (
              <button
                type="button"
                className="checklist-focus-action-btn"
                onClick={() => handleFocusClick(item.targetId)}
                aria-label={`Corrigir pendência de ${item.label}`}
              >
                Ir para o campo →
              </button>
            ) : null}
          </li>
        ))}

        {/* Itens Opcionais / Não bloqueantes */}
        <li className="checklist-item item-optional">
          <div className="checklist-item-status-icon" aria-hidden="true">
            ℹ️
          </div>
          <div className="checklist-item-info">
            <span className="checklist-item-title">
              Campos Opcionais
              <span className="checklist-optional-tag"> (Opcional - não bloqueia publicação)</span>
            </span>
            <p className="checklist-item-message">
              Descrição: {event.description ? 'Informada' : 'Não informada'} | Banner:{' '}
              {event.imageUrl ? 'Cadastrado' : 'Não cadastrado (usará fallback)'} | Categoria:{' '}
              {event.category ? event.category : 'Não informada'}
            </p>
          </div>
        </li>
      </ul>

      {!isPublished ? (
        <form onSubmit={handlePublishSubmit} className="publication-publish-form">
          <div className="publication-actions">
            <button
              type="submit"
              className="publication-publish-btn"
              disabled={!isReadyToPublish || isPublishing}
              aria-disabled={!isReadyToPublish || isPublishing}
            >
              {isPublishing ? 'Publicando evento…' : 'Publicar Evento'}
            </button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
