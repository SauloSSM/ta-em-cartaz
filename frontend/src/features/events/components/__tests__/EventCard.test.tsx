import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EventCard } from '../EventCard';
import type { PublicEventResponse } from '../../api/eventsApi';

const sampleEvent: PublicEventResponse = {
  id: 'e5b12852-5a24-4f01-8b2b-98d9c1234567',
  title: 'Festival Primavera Sound 2026',
  description: 'O maior festival de música indie e alternativa da América Latina.',
  imageUrl: 'https://images.example.com/primavera.jpg',
  category: 'Festival',
  status: 'PUBLISHED',
  venueName: 'Autódromo de Interlagos',
  venueAddress: 'Av. Senador Teotônio Vilela, 261 - São Paulo',
  startsAt: '2026-11-20T19:00:00Z',
  startingPrice: 180.5,
  salesClosed: false,
  createdAt: '2026-08-15T10:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

describe('EventCard', () => {
  it('renders all event metadata: title, category, date, venue and starting price in BRL', () => {
    render(<EventCard event={sampleEvent} />);

    expect(screen.getByRole('heading', { level: 3, name: 'Festival Primavera Sound 2026' })).toBeDefined();
    expect(screen.getByText('Festival')).toBeDefined();
    expect(screen.getByText(/Autódromo de Interlagos — Av. Senador Teotônio Vilela, 261 - São Paulo/)).toBeDefined();
    expect(screen.getByText(/A partir de/)).toBeDefined();
    expect(screen.getByText(/R\$\s*180,50/)).toBeDefined();
    expect(screen.getByAltText('Banner do evento Festival Primavera Sound 2026')).toBeDefined();
    expect(screen.queryByText('Vendas encerradas')).toBeNull();
  });

  it('renders fallback placeholder when imageUrl is not provided', () => {
    const eventWithoutImage: PublicEventResponse = {
      ...sampleEvent,
      imageUrl: undefined,
    };

    render(<EventCard event={eventWithoutImage} />);

    expect(screen.queryByRole('img', { name: /Banner do evento/ })).toBeNull();
    expect(screen.getByRole('img', { name: 'Imagem padrão do evento' })).toBeDefined();
  });

  it('switches to fallback placeholder when image loading fails', () => {
    render(<EventCard event={sampleEvent} />);

    const image = screen.getByAltText('Banner do evento Festival Primavera Sound 2026');
    fireEvent.error(image);

    expect(screen.queryByAltText('Banner do evento Festival Primavera Sound 2026')).toBeNull();
    expect(screen.getByRole('img', { name: 'Imagem padrão do evento' })).toBeDefined();
  });

  it('renders "Vendas encerradas" badge when salesClosed is true', () => {
    const closedEvent: PublicEventResponse = {
      ...sampleEvent,
      salesClosed: true,
    };

    render(<EventCard event={closedEvent} />);

    const badge = screen.getByRole('status');
    expect(badge.textContent).toBe('Vendas encerradas');
  });

  it('calls onClick handler when action button is clicked', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(<EventCard event={sampleEvent} onClick={handleClick} />);

    const button = screen.getByRole('button', { name: 'Ver detalhes de Festival Primavera Sound 2026' });
    await user.click(button);

    expect(handleClick).toHaveBeenCalledTimes(1);
    expect(handleClick).toHaveBeenCalledWith(sampleEvent);
  });
});
