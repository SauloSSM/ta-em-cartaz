import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SectorManager } from '../SectorManager';
import * as eventsApi from '../../api/eventsApi';

describe('SectorManager', () => {
  const eventId = 'ev-100';

  const mockSectors: eventsApi.TicketSectorResponse[] = [
    {
      id: 'sec-1',
      eventId,
      name: 'Pista Comum',
      description: 'Acesso geral',
      capacity: 500,
      availableQuantity: 500,
      price: 120.5,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
    {
      id: 'sec-2',
      eventId,
      name: 'Camarote VIP',
      description: 'Área VIP com open bar',
      capacity: 80,
      availableQuantity: 80,
      price: 350.0,
      createdAt: '2026-08-15T12:00:00Z',
      updatedAt: '2026-08-15T12:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially and then lists sectors', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: mockSectors });

    render(<SectorManager eventId={eventId} isDraft={true} />);

    expect(screen.getByText('Carregando setores…')).toBeDefined();

    expect(await screen.findByRole('heading', { name: 'Pista Comum' })).toBeDefined();
    expect(screen.getByRole('heading', { name: 'Camarote VIP' })).toBeDefined();
    expect(screen.getByText(/500 \/ 500/)).toBeDefined();
    expect(screen.getByText(/80 \/ 80/)).toBeDefined();
    expect(screen.getByText('580 ingressos')).toBeDefined();
  });

  it('renders empty state message when no sectors are registered', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: [] });

    render(<SectorManager eventId={eventId} isDraft={true} />);

    expect(await screen.findByText('Nenhum setor cadastrado ainda.')).toBeDefined();
    expect(screen.getByText(/Clique em "\+ Novo Setor" para cadastrar áreas/i)).toBeDefined();
  });

  it('allows adding and editing sectors in published events and disables delete on committed sectors', async () => {
    const publishedSectors: eventsApi.TicketSectorResponse[] = [
      {
        ...mockSectors[0],
        availableQuantity: 450, // 50 committed
      },
      {
        ...mockSectors[1],
        availableQuantity: 80, // 0 committed
      },
    ];

    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: publishedSectors });

    render(<SectorManager eventId={eventId} isDraft={false} />);

    await screen.findByRole('heading', { name: 'Pista Comum' });

    // "+ Novo Setor" button is available
    expect(screen.getByRole('button', { name: /Novo Setor/i })).toBeDefined();

    // "Editar" button is available on both sectors
    expect(screen.getByRole('button', { name: 'Editar setor Pista Comum' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Editar setor Camarote VIP' })).toBeDefined();

    // Committed indicators
    expect(screen.getByText('50')).toBeDefined(); // committed for Pista Comum

    // Delete button on committed sector is disabled
    const deleteCommittedBtn = screen.getByRole('button', {
      name: 'Setor Pista Comum possui ingressos comprometidos e não pode ser excluído',
    }) as HTMLButtonElement;
    expect(deleteCommittedBtn.disabled).toBe(true);

    // Delete button on uncommitted sector is enabled
    const deleteUncommittedBtn = screen.getByRole('button', {
      name: 'Excluir setor Camarote VIP',
    }) as HTMLButtonElement;
    expect(deleteUncommittedBtn.disabled).toBe(false);
  });

  it('opens SectorEditor modal on "+ Novo Setor" button click', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: mockSectors });

    render(<SectorManager eventId={eventId} isDraft={true} />);

    const newBtn = await screen.findByRole('button', { name: /Adicionar novo setor de ingressos/i });
    fireEvent.click(newBtn);

    expect(screen.getByRole('heading', { name: 'Adicionar Novo Setor' })).toBeDefined();
  });

  it('opens SectorEditor modal on "Editar" button click populated with sector', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: mockSectors });

    render(<SectorManager eventId={eventId} isDraft={true} />);

    const editBtn = await screen.findByRole('button', { name: 'Editar setor Pista Comum' });
    fireEvent.click(editBtn);

    expect(screen.getByRole('heading', { name: 'Editar Setor: Pista Comum' })).toBeDefined();
    const nameInput = screen.getByLabelText(/Nome do Setor/i) as HTMLInputElement;
    expect(nameInput.value).toBe('Pista Comum');
  });

  it('opens confirmation dialog on "Excluir" and deletes sector on confirm', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockResolvedValue({ sectors: [...mockSectors] });
    vi.spyOn(eventsApi, 'deleteTicketSector').mockResolvedValue();

    render(<SectorManager eventId={eventId} isDraft={true} />);

    const deleteBtn = await screen.findByRole('button', { name: 'Excluir setor Camarote VIP' });
    fireEvent.click(deleteBtn);

    expect(screen.getByRole('heading', { name: 'Excluir Setor' })).toBeDefined();
    expect(screen.getByText(/Tem certeza de que deseja excluir o setor "Camarote VIP"?/)).toBeDefined();

    const confirmDeleteBtn = screen.getByRole('button', { name: 'Sim, excluir setor' });
    fireEvent.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(eventsApi.deleteTicketSector).toHaveBeenCalledWith(eventId, 'sec-2');
      expect(screen.queryByRole('heading', { name: 'Camarote VIP' })).toBeNull();
      expect(screen.getByText('Setor "Camarote VIP" excluído com sucesso.')).toBeDefined();
    });
  });

  it('handles error when loading sectors fails', async () => {
    vi.spyOn(eventsApi, 'listTicketSectors').mockRejectedValue(new Error('Falha de rede'));

    render(<SectorManager eventId={eventId} isDraft={true} />);

    const alertEl = await screen.findByRole('alert');
    expect(alertEl.textContent).toContain('Falha de rede');
  });
});
