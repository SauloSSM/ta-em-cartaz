import { useEffect, useRef } from 'react';

import './OrganizerWorkspace.css';
type DeleteConfirmDialogProps = {
  isOpen: boolean;
  eventTitle: string;
  busy: boolean;
  onConfirm: () => Promise<void> | void;
  onCancel: () => void;
};

export function DeleteConfirmDialog({
  isOpen,
  eventTitle,
  busy,
  onConfirm,
  onCancel,
}: DeleteConfirmDialogProps) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (isOpen) {
      cancelButtonRef.current?.focus();
      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Escape' && !busy) {
          onCancel();
        }
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, busy, onCancel]);

  if (!isOpen) {
    return null;
  }

  return (
    <div className="dialog-backdrop" role="presentation">
      <div
        className="dialog-box"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-desc"
      >
        <h3 id="delete-dialog-title">Excluir rascunho de evento</h3>
        <p id="delete-dialog-desc">
          Tem certeza de que deseja excluir o rascunho do evento &quot;{eventTitle}&quot;? Esta ação não pode ser desfeita.
        </p>

        <div className="dialog-actions">
          <button
            ref={cancelButtonRef}
            type="button"
            className="dialog-cancel-btn"
            disabled={busy}
            onClick={onCancel}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="dialog-danger-btn"
            disabled={busy}
            onClick={() => void onConfirm()}
          >
            {busy ? 'Excluindo…' : 'Sim, excluir rascunho'}
          </button>
        </div>
      </div>
    </div>
  );
}
