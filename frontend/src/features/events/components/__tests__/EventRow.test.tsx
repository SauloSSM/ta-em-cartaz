import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EventRow } from '../EventRow';
import type { PublicEventResponse } from '../../api/eventsApi';

const mockEvent: PublicEventResponse = {
  id: 'ev-row-1',
  title: 'TIM MAIA EXPERIENCE — A GRANDE CELEBRAÇÃO DA SOUL MUSIC BRASILEIRA',
  description: 'Show especial',
  imageUrl: 'https://images.example.com/tim.jpg',
  category: 'Show',
  status: 'PUBLISHED',
  venueName: 'Teatro Positivo Grande Auditório',
  venueAddress: 'R. Prof. Pedro Viriato Parigot de Souza, 5300 - Curitiba, PR',
  startsAt: '2026-08-18T19:00:00Z',
  startingPrice: 120,
  salesClosed: false,
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

describe('EventRow', () => {
  it('renders editorial row with date badge, title, category, venue and price', () => {
    render(<EventRow event={mockEvent} />);

    expect(screen.getByRole('heading', { level: 3, name: mockEvent.title })).toBeDefined();
    expect(screen.getByText('Show')).toBeDefined();
    expect(screen.getByText(/18/)).toBeDefined();
    expect(screen.getByText(/Teatro Positivo Grande Auditório/)).toBeDefined();
    expect(screen.getByText(/R\$\s*120,00/)).toBeDefined();
  });

  it('triggers onClick with event data when action button is clicked', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(<EventRow event={mockEvent} onClick={handleClick} />);

    const button = screen.getByRole('button', { name: `Ver detalhes de ${mockEvent.title}` });
    await user.click(button);

    expect(handleClick).toHaveBeenCalledWith(mockEvent);
  });

  it('displays sales closed badge when salesClosed is true', () => {
    render(<EventRow event={{ ...mockEvent, salesClosed: true }} />);

    expect(screen.getByRole('status')).toBeDefined();
    expect(screen.getByText('Vendas encerradas')).toBeDefined();
  });

  it('handles long content and special characters gracefully (stress fixture)', () => {
    const longEvent: PublicEventResponse = {
      ...mockEvent,
      title: 'A'.repeat(120),
      venueName: 'B'.repeat(80),
      venueAddress: 'C'.repeat(100),
      startingPrice: 99999.99,
    };

    render(<EventRow event={longEvent} />);

    expect(screen.getByRole('heading', { level: 3 })).toBeDefined();
    expect(screen.getByText(/99\.999,99/)).toBeDefined();
  });
});
