import { useState, useTransition, useCallback, type FormEvent } from 'react';
import {
  updateDraftEvent,
  deleteDraftEvent,
  publishEvent,
  type EventResponse,
  type UpdateDraftEventRequest,
  type TicketSectorResponse,
} from '../api/eventsApi';
import { DeleteConfirmDialog } from './DeleteConfirmDialog';
import { SectorManager } from './SectorManager';
import { PublicationChecklist } from './PublicationChecklist';

type DraftEventEditorProps = {
  event: EventResponse;
  onBack?: () => void;
  onEventUpdated?: (updatedEvent: EventResponse) => void;
  onEventDeleted?: (deletedEventId: string) => void;
};

export function DraftEventEditor({
  event,
  onBack,
  onEventUpdated,
  onEventDeleted,
}: DraftEventEditorProps) {
  const [currentEvent, setCurrentEvent] = useState<EventResponse>(event);
  const isDraft = currentEvent.status === 'DRAFT';

  const [formData, setFormData] = useState<UpdateDraftEventRequest>({
    title: event.title,
    description: event.description ?? '',
    imageUrl: event.imageUrl ?? '',
    category: event.category ?? '',
    venueName: event.venueName ?? '',
    venueAddress: event.venueAddress ?? '',
    startsAt: event.startsAt ?? '',
  });

  const [sectors, setSectors] = useState<TicketSectorResponse[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishedSuccess, setPublishedSuccess] = useState(event.status === 'PUBLISHED');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  const handleSectorsChange = useCallback((newSectors: TicketSectorResponse[]) => {
    setSectors(newSectors);
  }, []);

  const handleFieldChange = (field: keyof UpdateDraftEventRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!isDraft) return;

    setIsSaving(true);
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      const updated = await updateDraftEvent(currentEvent.id, {
        title: formData.title,
        description: formData.description?.trim() ? formData.description : undefined,
        imageUrl: formData.imageUrl?.trim() ? formData.imageUrl : undefined,
        category: formData.category?.trim() ? formData.category : undefined,
        venueName: formData.venueName?.trim() ? formData.venueName : undefined,
        venueAddress: formData.venueAddress?.trim() ? formData.venueAddress : undefined,
        startsAt: formData.startsAt?.trim() ? formData.startsAt : undefined,
      });

      startTransition(() => {
        setCurrentEvent(updated);
        setStatusMessage('Alterações salvas com sucesso!');
      });

      if (onEventUpdated !== undefined) {
        onEventUpdated(updated);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar alterações no rascunho.';
      setErrorMessage(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!isDraft) return;

    setIsDeleting(true);
    setErrorMessage(null);

    try {
      await deleteDraftEvent(currentEvent.id);
      setShowDeleteDialog(false);
      if (onEventDeleted !== undefined) {
        onEventDeleted(currentEvent.id);
      } else if (onBack !== undefined) {
        onBack();
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir rascunho de evento.';
      setErrorMessage(msg);
      setShowDeleteDialog(false);
    } finally {
      setIsDeleting(false);
    }
  };

  const handlePublish = async () => {
    if (!isDraft || isPublishing) return;
    setIsPublishing(true);
    setPublishError(null);
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      const published = await publishEvent(currentEvent.id);
      startTransition(() => {
        setCurrentEvent(published);
        setPublishedSuccess(true);
        setStatusMessage('Evento publicado com sucesso!');
      });
      if (onEventUpdated !== undefined) {
        onEventUpdated(published);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao publicar evento.';
      setPublishError(msg);
    } finally {
      setIsPublishing(false);
    }
  };

  const isBusy = isSaving || isDeleting || isPublishing;

  return (
    <section className="draft-editor-section" aria-labelledby="draft-editor-title">
      <header className="draft-editor-header">
        <div className="draft-editor-title-container">
          <h2 id="draft-editor-title">Editor de Evento</h2>
          <span
            className={`event-status-badge ${isDraft ? 'status-draft' : 'status-published'}`}
            aria-label={`Status do evento: ${currentEvent.status}`}
          >
            {currentEvent.status}
          </span>
        </div>
        {onBack !== undefined ? (
          <button
            type="button"
            className="draft-editor-back-btn"
            onClick={onBack}
            disabled={isBusy}
            aria-label="Voltar para a lista de eventos"
          >
            ← Voltar para Meus Eventos
          </button>
        ) : null}
      </header>

      {statusMessage !== null ? (
        <div className="draft-editor-feedback success" role="status" aria-live="polite">
          <p>{statusMessage}</p>
        </div>
      ) : null}

      {errorMessage !== null ? (
        <div className="draft-editor-feedback error" role="alert">
          <p>{errorMessage}</p>
        </div>
      ) : null}

      <div className="draft-editor-content">
        <aside className="draft-editor-banner-aside">
          {formData.imageUrl && formData.imageUrl.trim().length > 0 ? (
            <img
              src={formData.imageUrl}
              alt={`Banner do evento ${formData.title}`}
              className="draft-editor-banner-img"
            />
          ) : (
            <div className="draft-editor-banner-placeholder" aria-hidden="true">
              <span>Sem banner cadastrado</span>
            </div>
          )}
        </aside>

        <form onSubmit={handleSave} className="draft-editor-form" aria-busy={isBusy}>
          <div className="draft-editor-meta-info">
            <span className="draft-editor-meta-label">ID do evento:</span>
            <code className="draft-editor-meta-value">{currentEvent.id}</code>
            {currentEvent.externalId ? (
              <>
                <span className="draft-editor-meta-label">
                  Ref. {currentEvent.externalSource ?? 'Ticketmaster'}:
                </span>
                <code className="draft-editor-meta-value">{currentEvent.externalId}</code>
              </>
            ) : null}
          </div>

          <div className="form-group">
            <label htmlFor="event-title-input">
              Título do Evento <span aria-hidden="true">*</span>
            </label>
            <input
              id="event-title-input"
              type="text"
              required
              disabled={!isDraft || isBusy}
              value={formData.title}
              onChange={(e) => handleFieldChange('title', e.target.value)}
              placeholder="Ex.: Festival de Música de Verão"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="event-venue-name-input">Nome do Local / Venue</label>
              <input
                id="event-venue-name-input"
                type="text"
                disabled={!isDraft || isBusy}
                value={formData.venueName}
                onChange={(e) => handleFieldChange('venueName', e.target.value)}
                placeholder="Ex.: Allianz Parque"
              />
            </div>

            <div className="form-group">
              <label htmlFor="event-venue-address-input">Endereço do Local</label>
              <input
                id="event-venue-address-input"
                type="text"
                disabled={!isDraft || isBusy}
                value={formData.venueAddress}
                onChange={(e) => handleFieldChange('venueAddress', e.target.value)}
                placeholder="Ex.: Av. Francisco Matarazzo, 1705, São Paulo - SP"
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="event-starts-at-input">Data e Hora de Início (ISO)</label>
              <input
                id="event-starts-at-input"
                type="text"
                disabled={!isDraft || isBusy}
                value={formData.startsAt}
                onChange={(e) => handleFieldChange('startsAt', e.target.value)}
                placeholder="Ex.: 2026-10-15T20:00:00Z"
              />
            </div>

            <div className="form-group">
              <label htmlFor="event-category-input">Categoria</label>
              <input
                id="event-category-input"
                type="text"
                disabled={!isDraft || isBusy}
                value={formData.category}
                onChange={(e) => handleFieldChange('category', e.target.value)}
                placeholder="Ex.: Show, Teatro, Festival..."
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="event-image-url-input">URL do Banner / Imagem</label>
              <input
                id="event-image-url-input"
                type="url"
                disabled={!isDraft || isBusy}
                value={formData.imageUrl}
                onChange={(e) => handleFieldChange('imageUrl', e.target.value)}
                placeholder="https://..."
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="event-description-input">Descrição do Evento</label>
            <textarea
              id="event-description-input"
              rows={4}
              disabled={!isDraft || isBusy}
              value={formData.description}
              onChange={(e) => handleFieldChange('description', e.target.value)}
              placeholder="Descreva as principais atrações, horários e informações relevantes..."
            />
          </div>

          <div className="draft-editor-timestamps">
            <small>Criado em: {new Date(currentEvent.createdAt).toLocaleString('pt-BR')}</small>
            <small>Atualizado em: {new Date(currentEvent.updatedAt).toLocaleString('pt-BR')}</small>
          </div>

          <div className="draft-editor-actions">
            {isDraft ? (
              <>
                <button
                  type="submit"
                  className="draft-save-btn"
                  disabled={isBusy}
                >
                  {isSaving ? 'Salvando…' : 'Salvar alterações'}
                </button>

                <button
                  type="button"
                  className="draft-delete-btn"
                  disabled={isBusy}
                  onClick={() => setShowDeleteDialog(true)}
                  aria-label={`Excluir rascunho de ${currentEvent.title}`}
                >
                  Excluir rascunho
                </button>
              </>
            ) : (
              <div className="published-lock-note" role="note">
                <p>Eventos publicados possuem dados estruturais protegidos e não podem ser excluídos.</p>
              </div>
            )}
          </div>
        </form>
      </div>

      <SectorManager
        eventId={currentEvent.id}
        isDraft={isDraft}
        onSectorsChange={handleSectorsChange}
      />

      <PublicationChecklist
        event={currentEvent}
        sectors={sectors}
        onFocusField={(fieldId) => {
          const el = document.getElementById(fieldId);
          if (el) {
            el.focus();
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }
        }}
        onPublish={handlePublish}
        isPublishing={isPublishing}
        publishedSuccess={publishedSuccess}
        error={publishError}
      />

      <DeleteConfirmDialog
        isOpen={showDeleteDialog}
        eventTitle={currentEvent.title}
        busy={isDeleting}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setShowDeleteDialog(false)}
      />
    </section>
  );
}
