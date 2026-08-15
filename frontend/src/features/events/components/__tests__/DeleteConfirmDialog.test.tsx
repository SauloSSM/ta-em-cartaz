import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { DeleteConfirmDialog } from '../DeleteConfirmDialog';

describe('DeleteConfirmDialog', () => {
  it('renders confirmation dialog with title and description when open', () => {
    render(
      <DeleteConfirmDialog
        isOpen={true}
        eventTitle="Festival de Verão"
        busy={false}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('alertdialog')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Excluir rascunho de evento' })).toBeDefined();
    expect(screen.getByText(/Festival de Verão/)).toBeDefined();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Sim, excluir rascunho' })).toBeDefined();
  });

  it('calls onCancel when clicking cancel button or pressing Escape', async () => {
    const handleCancel = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog
        isOpen={true}
        eventTitle="Festival de Verão"
        busy={false}
        onConfirm={vi.fn()}
        onCancel={handleCancel}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(handleCancel).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleCancel).toHaveBeenCalledTimes(2);
  });

  it('calls onConfirm when clicking confirm button', async () => {
    const handleConfirm = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog
        isOpen={true}
        eventTitle="Festival de Verão"
        busy={false}
        onConfirm={handleConfirm}
        onCancel={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Sim, excluir rascunho' }));
    expect(handleConfirm).toHaveBeenCalledTimes(1);
  });

  it('does not render anything when isOpen is false', () => {
    render(
      <DeleteConfirmDialog
        isOpen={false}
        eventTitle="Festival de Verão"
        busy={false}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.queryByRole('alertdialog')).toBeNull();
  });
});
