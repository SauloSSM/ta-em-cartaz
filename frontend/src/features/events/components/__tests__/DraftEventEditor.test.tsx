import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { DraftEventEditor } from '../DraftEventEditor';
import type { EventResponse } from '../../api/eventsApi';

describe('DraftEventEditor', () => {
  const mockEvent: EventResponse = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    organizerId: '00000000-0000-0000-0000-000000000001',
    externalId: 'tm-rock-2026',
    title: 'Rock in Rio 2026',
    description: 'Grande festival de música',
    imageUrl: 'https://images.example.com/banner.jpg',
    category: 'Rock',
    status: 'DRAFT',
    venue: 'Cidade do Rock',
    startsAt: '2026-09-20T18:00:00Z',
    createdAt: '2026-08-15T12:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  };

  it('renders draft event details with status badge and accessibility semantics', () => {
    render(<DraftEventEditor event={mockEvent} />);

    expect(screen.getByRole('heading', { name: 'Editor de Evento', level: 2 })).toBeDefined();
    expect(screen.getByLabelText('Status do evento: DRAFT')).toBeDefined();
    expect(screen.getByText('Rock in Rio 2026')).toBeDefined();
    expect(screen.getByText('123e4567-e89b-12d3-a456-426614174000')).toBeDefined();
    expect(screen.getByText('tm-rock-2026')).toBeDefined();
    expect(screen.getByText('Grande festival de música')).toBeDefined();
    expect(screen.getByText('Rock')).toBeDefined();
    expect(screen.getByText('Cidade do Rock')).toBeDefined();

    const banner = screen.getByRole('img', { name: 'Banner do evento Rock in Rio 2026' });
    expect(banner.getAttribute('src')).toBe('https://images.example.com/banner.jpg');
  });

  it('calls onBack when clicking back button', async () => {
    const handleBack = vi.fn();
    const user = userEvent.setup();

    render(<DraftEventEditor event={mockEvent} onBack={handleBack} />);

    const backButton = screen.getByRole('button', { name: 'Voltar para a pesquisa de catálogo' });
    await user.click(backButton);

    expect(handleBack).toHaveBeenCalledTimes(1);
  });
});
