import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SectorEditor } from '../SectorEditor';
import * as eventsApi from '../../api/eventsApi';

describe('SectorEditor', () => {
  const eventId = 'ev-123';

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders correctly for new sector creation', () => {
    render(
      <SectorEditor
        eventId={eventId}
        isOpen={true}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Adicionar Novo Setor' })).toBeDefined();
    const nameInput = screen.getByLabelText(/Nome do Setor/i) as HTMLInputElement;
    expect(nameInput.value).toBe('');
    const capInput = screen.getByLabelText(/Capacidade Total/i) as HTMLInputElement;
    expect(capInput.value).toBe('');
    const priceInput = screen.getByLabelText(/Preço Unitário/i) as HTMLInputElement;
    expect(priceInput.value).toBe('');
    expect(screen.getByRole('button', { name: 'Criar Setor' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDefined();
  });

  it('renders populated fields when editing an existing sector', () => {
    const existingSector: eventsApi.TicketSectorResponse = {
      id: 'sec-1',
      eventId,
      name: 'Pista VIP',
      description: 'Acesso exclusivo',
      capacity: 200,
      availableQuantity: 200,
      price: 150.0,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };

    render(
      <SectorEditor
        eventId={eventId}
        sector={existingSector}
        isOpen={true}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Editar Setor: Pista VIP' })).toBeDefined();
    const nameInput = screen.getByLabelText(/Nome do Setor/i) as HTMLInputElement;
    expect(nameInput.value).toBe('Pista VIP');
    const capInput = screen.getByLabelText(/Capacidade Total/i) as HTMLInputElement;
    expect(capInput.value).toBe('200');
    const priceInput = screen.getByLabelText(/Preço Unitário/i) as HTMLInputElement;
    expect(priceInput.value).toBe('150');
    const descInput = screen.getByLabelText(/Descrição/i) as HTMLTextAreaElement;
    expect(descInput.value).toBe('Acesso exclusivo');
    expect(screen.getByRole('button', { name: 'Salvar Alterações' })).toBeDefined();
  });

  it('validates client-side constraints and displays field errors', async () => {
    render(
      <SectorEditor
        eventId={eventId}
        isOpen={true}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const submitBtn = screen.getByRole('button', { name: 'Criar Setor' });
    fireEvent.click(submitBtn);

    expect(await screen.findByText('Nome do setor é obrigatório.')).toBeDefined();
    expect(screen.getByText('Capacidade deve ser um número inteiro maior que zero.')).toBeDefined();
    expect(screen.getByText('Preço deve ser maior ou igual a zero.')).toBeDefined();
  });

  it('submits create request with valid inputs and invokes onSaved callback', async () => {
    const onSaved = vi.fn();
    const createdSector: eventsApi.TicketSectorResponse = {
      id: 'sec-created',
      eventId,
      name: 'Camarote',
      description: 'Vista privilegiada',
      capacity: 80,
      availableQuantity: 80,
      price: 350.0,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };

    vi.spyOn(eventsApi, 'createTicketSector').mockResolvedValue(createdSector);

    render(
      <SectorEditor
        eventId={eventId}
        isOpen={true}
        onSaved={onSaved}
        onCancel={vi.fn()}
      />,
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Nome do Setor/i), 'Camarote');
    await user.type(screen.getByLabelText(/Capacidade Total/i), '80');
    await user.type(screen.getByLabelText(/Preço Unitário/i), '350.00');
    await user.type(screen.getByLabelText(/Descrição/i), 'Vista privilegiada');

    await user.click(screen.getByRole('button', { name: 'Criar Setor' }));

    await waitFor(() => {
      expect(eventsApi.createTicketSector).toHaveBeenCalledWith(eventId, {
        name: 'Camarote',
        description: 'Vista privilegiada',
        capacity: 80,
        price: 350,
      });
      expect(onSaved).toHaveBeenCalledWith(createdSector);
    });
  });

  it('submits update request when editing and invokes onSaved callback', async () => {
    const onSaved = vi.fn();
    const existingSector: eventsApi.TicketSectorResponse = {
      id: 'sec-existing',
      eventId,
      name: 'Pista',
      capacity: 100,
      availableQuantity: 100,
      price: 80.0,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };
    const updatedSector: eventsApi.TicketSectorResponse = {
      ...existingSector,
      name: 'Pista Premium',
      capacity: 150,
      price: 100.0,
    };

    vi.spyOn(eventsApi, 'updateTicketSector').mockResolvedValue(updatedSector);

    render(
      <SectorEditor
        eventId={eventId}
        sector={existingSector}
        isOpen={true}
        onSaved={onSaved}
        onCancel={vi.fn()}
      />,
    );

    const user = userEvent.setup();
    const nameInput = screen.getByLabelText(/Nome do Setor/i);
    await user.clear(nameInput);
    await user.type(nameInput, 'Pista Premium');

    const capInput = screen.getByLabelText(/Capacidade Total/i);
    await user.clear(capInput);
    await user.type(capInput, '150');

    const priceInput = screen.getByLabelText(/Preço Unitário/i);
    await user.clear(priceInput);
    await user.type(priceInput, '100.00');

    await user.click(screen.getByRole('button', { name: 'Salvar Alterações' }));

    await waitFor(() => {
      expect(eventsApi.updateTicketSector).toHaveBeenCalledWith(eventId, 'sec-existing', {
        name: 'Pista Premium',
        description: undefined,
        capacity: 150,
        price: 100,
      });
      expect(onSaved).toHaveBeenCalledWith(updatedSector);
    });
  });

  it('handles cancel button click and Escape key', async () => {
    const onCancel = vi.fn();
    render(
      <SectorEditor
        eventId={eventId}
        isOpen={true}
        onSaved={vi.fn()}
        onCancel={onCancel}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancel).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onCancel).toHaveBeenCalledTimes(2);
  });

  it('validates minimum capacity against committed tickets when editing sector with commitments', async () => {
    const updateSpy = vi.spyOn(eventsApi, 'updateTicketSector');
    const committedSector: eventsApi.TicketSectorResponse = {
      id: 'sec-committed',
      eventId,
      name: 'Pista',
      capacity: 100,
      availableQuantity: 60, // 40 committed
      price: 80.0,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    };

    render(
      <SectorEditor
        eventId={eventId}
        sector={committedSector}
        isOpen={true}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    // Verify hint
    expect(screen.getByText('Mínimo permitido: 40 (ingressos já comprometidos)')).toBeDefined();

    const capInput = screen.getByLabelText(/Capacidade Total/i);
    fireEvent.change(capInput, { target: { value: '35' } });

    const submitBtn = screen.getByRole('button', { name: 'Salvar Alterações' });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText('A capacidade não pode ser menor que a quantidade já comprometida (40).'),
    ).toBeDefined();
    expect(updateSpy).not.toHaveBeenCalled();
  });
});
