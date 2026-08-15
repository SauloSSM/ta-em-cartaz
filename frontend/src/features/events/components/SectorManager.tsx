import { useState, useEffect, useCallback } from 'react';
import {
  listTicketSectors,
  deleteTicketSector,
  type TicketSectorResponse,
} from '../api/eventsApi';
import { SectorEditor } from './SectorEditor';

type SectorManagerProps = {
  eventId: string;
  isDraft: boolean;
};

export function SectorManager({ eventId, isDraft }: SectorManagerProps) {
  const [sectors, setSectors] = useState<TicketSectorResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editingSector, setEditingSector] = useState<TicketSectorResponse | null>(null);

  const [deletingSector, setDeletingSector] = useState<TicketSectorResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadSectors = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const response = await listTicketSectors(eventId);
      setSectors(response.sectors);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar setores do evento.';
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    void loadSectors();
  }, [loadSectors]);

  const handleOpenCreate = () => {
    setEditingSector(null);
    setIsEditorOpen(true);
    setStatusMessage(null);
  };

  const handleOpenEdit = (sector: TicketSectorResponse) => {
    setEditingSector(sector);
    setIsEditorOpen(true);
    setStatusMessage(null);
  };

  const handleSectorSaved = (savedSector: TicketSectorResponse) => {
    setIsEditorOpen(false);
    setEditingSector(null);
    setSectors((prev) => {
      const exists = prev.some((s) => s.id === savedSector.id);
      if (exists) {
        return prev.map((s) => (s.id === savedSector.id ? savedSector : s));
      }
      return [...prev, savedSector];
    });
    setStatusMessage(`Setor "${savedSector.name}" salvo com sucesso!`);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingSector) return;
    setIsDeleting(true);
    setErrorMessage(null);

    try {
      await deleteTicketSector(eventId, deletingSector.id);
      setSectors((prev) => prev.filter((s) => s.id !== deletingSector.id));
      setStatusMessage(`Setor "${deletingSector.name}" excluído com sucesso.`);
      setDeletingSector(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir setor.';
      setErrorMessage(msg);
      setDeletingSector(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(price);
  };

  const totalCapacity = sectors.reduce((sum, s) => sum + s.capacity, 0);

  return (
    <section className="sector-manager-section" aria-labelledby="sector-manager-title">
      <header className="sector-manager-header">
        <div>
          <h3 id="sector-manager-title">Setores de Ingressos</h3>
          <p className="sector-manager-subtitle">
            Configure a capacidade e os valores dos ingressos por setor para este evento.
          </p>
        </div>
        {isDraft ? (
          <button
            type="button"
            className="add-sector-btn"
            onClick={handleOpenCreate}
            aria-label="Adicionar novo setor de ingressos"
          >
            + Novo Setor
          </button>
        ) : null}
      </header>

      {statusMessage !== null ? (
        <div className="sector-manager-feedback success" role="status" aria-live="polite">
          <p>{statusMessage}</p>
        </div>
      ) : null}

      {errorMessage !== null ? (
        <div className="sector-manager-feedback error" role="alert">
          <p>{errorMessage}</p>
        </div>
      ) : null}

      {loading ? (
        <div className="sector-manager-loading" role="status">
          <p>Carregando setores…</p>
        </div>
      ) : sectors.length === 0 ? (
        <div className="empty-sectors-card">
          <p className="empty-sectors-title">Nenhum setor cadastrado ainda.</p>
          {isDraft ? (
            <p className="empty-sectors-hint">
              Clique em &quot;+ Novo Setor&quot; para cadastrar áreas (Ex.: Pista, Camarote, VIP).
            </p>
          ) : null}
        </div>
      ) : (
        <div className="sectors-list-container">
          <div className="sectors-summary-bar">
            <span>Total de setores: <strong>{sectors.length}</strong></span>
            <span>Capacidade total: <strong>{totalCapacity} ingressos</strong></span>
          </div>

          <ul className="sectors-list" aria-label="Lista de setores de ingressos">
            {sectors.map((sector) => (
              <li key={sector.id} className="sector-card">
                <div className="sector-card-header">
                  <h4 className="sector-card-title">{sector.name}</h4>
                  <span className="sector-price-badge">{formatPrice(sector.price)}</span>
                </div>

                {sector.description ? (
                  <p className="sector-card-desc">{sector.description}</p>
                ) : null}

                <div className="sector-card-meta">
                  <div className="sector-meta-item">
                    <span className="sector-meta-label">Capacidade:</span>
                    <strong className="sector-meta-value">{sector.capacity}</strong>
                  </div>
                  <div className="sector-meta-item">
                    <span className="sector-meta-label">Disponibilidade:</span>
                    <strong className="sector-meta-value">
                      {sector.availableQuantity} / {sector.capacity}
                    </strong>
                  </div>
                </div>

                {isDraft ? (
                  <div className="sector-card-actions">
                    <button
                      type="button"
                      className="sector-edit-btn"
                      onClick={() => handleOpenEdit(sector)}
                      aria-label={`Editar setor ${sector.name}`}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="sector-delete-btn"
                      onClick={() => setDeletingSector(sector)}
                      aria-label={`Excluir setor ${sector.name}`}
                    >
                      Excluir
                    </button>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      )}

      <SectorEditor
        eventId={eventId}
        sector={editingSector}
        isOpen={isEditorOpen}
        onSaved={handleSectorSaved}
        onCancel={() => {
          setIsEditorOpen(false);
          setEditingSector(null);
        }}
      />

      {deletingSector ? (
        <div className="dialog-backdrop" role="presentation">
          <div
            className="dialog-box"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-sector-dialog-title"
            aria-describedby="delete-sector-dialog-desc"
          >
            <h3 id="delete-sector-dialog-title">Excluir Setor</h3>
            <p id="delete-sector-dialog-desc">
              Tem certeza de que deseja excluir o setor &quot;{deletingSector.name}&quot;?
            </p>

            <div className="dialog-actions">
              <button
                type="button"
                className="dialog-cancel-btn"
                disabled={isDeleting}
                onClick={() => setDeletingSector(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="dialog-danger-btn"
                disabled={isDeleting}
                onClick={() => void handleDeleteConfirm()}
              >
                {isDeleting ? 'Excluindo…' : 'Sim, excluir setor'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
