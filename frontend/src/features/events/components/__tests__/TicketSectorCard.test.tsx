import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { TicketSectorResponse } from '../../api/eventsApi';
import { TicketSectorCard } from '../TicketSectorCard';

const mockSector: TicketSectorResponse = {
  id: 'sec-1',
  eventId: 'ev-1',
  name: 'Pista Premium',
  description: 'Acesso em frente ao palco',
  capacity: 200,
  availableQuantity: 150,
  price: 250.0,
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

const mockSoldOutSector: TicketSectorResponse = {
  id: 'sec-2',
  eventId: 'ev-1',
  name: 'Área VIP',
  description: 'Área VIP esgotada',
  capacity: 50,
  availableQuantity: 0,
  price: 500.0,
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

describe('TicketSectorCard component', () => {
  it('renderiza nome, descrição, preço BRL e disponibilidade', () => {
    render(
      <TicketSectorCard
        sector={mockSector}
        selected={false}
        onSelect={vi.fn()}
      />,
    );

    expect(screen.getByText('Pista Premium')).toBeDefined();
    expect(screen.getByText('Acesso em frente ao palco')).toBeDefined();
    expect(screen.getByText(/250,00/)).toBeDefined();
    expect(screen.getByText(/150/)).toBeDefined();
    expect(
      (screen.getByRole('button', { name: /Selecionar setor Pista Premium/ }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);
  });

  it('exibe badge Esgotado e desabilita controles quando availableQuantity é zero', () => {
    render(
      <TicketSectorCard
        sector={mockSoldOutSector}
        selected={false}
        onSelect={vi.fn()}
      />,
    );

    expect(screen.getByText('Área VIP')).toBeDefined();
    expect(screen.getByText('Esgotado')).toBeDefined();
    expect((screen.getByRole('radio') as HTMLInputElement).disabled).toBe(true);
    expect(screen.queryByRole('button', { name: /Selecionar/ })).toBeNull();
  });

  it('chama onSelect ao clicar no botão ou no rádio', async () => {
    const user = userEvent.setup();
    const handleSelect = vi.fn();

    render(
      <TicketSectorCard
        sector={mockSector}
        selected={false}
        onSelect={handleSelect}
      />,
    );

    const button = screen.getByRole('button', { name: /Selecionar/ });
    await user.click(button);

    expect(handleSelect).toHaveBeenCalledWith(mockSector);
  });

  it('exibe estado selecionado quando selected=true', () => {
    render(
      <TicketSectorCard
        sector={mockSector}
        selected={true}
        onSelect={vi.fn()}
      />,
    );

    expect((screen.getByRole('radio') as HTMLInputElement).checked).toBe(true);
    expect(screen.getByRole('button', { name: /Selecionar/ }).textContent).toBe('Selecionado');
  });
});
