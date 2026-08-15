import type { EventResponse } from '../api/eventsApi';

type DraftEventEditorProps = {
  event: EventResponse;
  onBack?: () => void;
};

export function DraftEventEditor({ event, onBack }: DraftEventEditorProps) {
  return (
    <section className="draft-editor-section" aria-labelledby="draft-editor-title">
      <header className="draft-editor-header">
        <div className="draft-editor-title-container">
          <h2 id="draft-editor-title">Editor de Evento</h2>
          <span className="event-status-badge status-draft" aria-label={`Status do evento: ${event.status}`}>
            {event.status}
          </span>
        </div>
        {onBack !== undefined ? (
          <button
            type="button"
            className="draft-editor-back-btn"
            onClick={onBack}
            aria-label="Voltar para a pesquisa de catálogo"
          >
            ← Voltar para a pesquisa
          </button>
        ) : null}
      </header>

      <div className="draft-editor-content">
        <aside className="draft-editor-banner-aside">
          {event.imageUrl !== undefined && event.imageUrl.trim().length > 0 ? (
            <img
              src={event.imageUrl}
              alt={`Banner do evento ${event.title}`}
              className="draft-editor-banner-img"
            />
          ) : (
            <div className="draft-editor-banner-placeholder" aria-hidden="true">
              <span>Sem banner cadastrado</span>
            </div>
          )}
        </aside>

        <div className="draft-editor-info-card">
          <div className="draft-editor-field">
            <span className="draft-editor-label">Identificador único (UUID):</span>
            <code className="draft-editor-value">{event.id}</code>
          </div>

          <div className="draft-editor-field">
            <span className="draft-editor-label">Título do Evento:</span>
            <span className="draft-editor-value font-bold">{event.title}</span>
          </div>

          {event.category !== undefined && event.category.trim().length > 0 ? (
            <div className="draft-editor-field">
              <span className="draft-editor-label">Categoria:</span>
              <span className="draft-editor-value">{event.category}</span>
            </div>
          ) : null}

          {event.externalId !== undefined && event.externalId.trim().length > 0 ? (
            <div className="draft-editor-field">
              <span className="draft-editor-label">Referência Ticketmaster:</span>
              <code className="draft-editor-value">{event.externalId}</code>
            </div>
          ) : null}

          {event.description !== undefined && event.description.trim().length > 0 ? (
            <div className="draft-editor-field">
              <span className="draft-editor-label">Descrição:</span>
              <p className="draft-editor-description">{event.description}</p>
            </div>
          ) : null}

          <div className="draft-editor-field">
            <span className="draft-editor-label">Local / Venue:</span>
            <span className="draft-editor-value">{event.venue ?? 'A definir nas próximas etapas'}</span>
          </div>

          <div className="draft-editor-field">
            <span className="draft-editor-label">Data e Hora de Início:</span>
            <span className="draft-editor-value">{event.startsAt ?? 'A definir nas próximas etapas'}</span>
          </div>

          <div className="draft-editor-timestamps">
            <small>Criado em: {new Date(event.createdAt).toLocaleString('pt-BR')}</small>
            <small>Atualizado em: {new Date(event.updatedAt).toLocaleString('pt-BR')}</small>
          </div>
        </div>
      </div>

      <footer className="draft-editor-footer">
        <div className="draft-editor-status-note" role="note">
          <p>
            Este evento está salvo em rascunho (<strong>DRAFT</strong>) com identidade própria no banco de dados.
            A configuração de setores de ingressos e a revisão para publicação estarão disponíveis nas próximas etapas.
          </p>
        </div>
      </footer>
    </section>
  );
}
