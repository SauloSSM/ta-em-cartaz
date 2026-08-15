import { useState, useTransition, type FormEvent } from 'react';
import { updateDraftEvent, deleteDraftEvent, type EventResponse, type UpdateDraftEventRequest } from '../api/eventsApi';
import { DeleteConfirmDialog } from './DeleteConfirmDialog';

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
  const isDraft = event.status === 'DRAFT';

  const [formData, setFormData] = useState<UpdateDraftEventRequest>({
    title: event.title,
    description: event.description ?? '',
    imageUrl: event.imageUrl ?? '',
    category: event.category ?? '',
    venue: event.venue ?? '',
    startsAt: event.startsAt ?? '',
  });

  const [currentEvent, setCurrentEvent] = useState<EventResponse>(event);
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [, startTransition] = useTransition();

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
        venue: formData.venue?.trim() ? formData.venue : undefined,
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

  const isBusy = isSaving || isDeleting;

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
                <span className="draft-editor-meta-label">Ref. Ticketmaster:</span>
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

            <div className="form-group">
              <label htmlFor="event-venue-input">Local / Venue</label>
              <input
                id="event-venue-input"
                type="text"
                disabled={!isDraft || isBusy}
                value={formData.venue}
                onChange={(e) => handleFieldChange('venue', e.target.value)}
                placeholder="Ex.: Allianz Parque, São Paulo"
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
