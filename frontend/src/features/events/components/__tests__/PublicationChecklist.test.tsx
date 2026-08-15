import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { PublicationChecklist } from '../PublicationChecklist';
import type { EventResponse, TicketSectorResponse } from '../../api/eventsApi';

const baseEvent: EventResponse = {
  id: 'ev-test-1',
  organizerId: 'org-test-1',
  externalSource: 'TICKETMASTER',
  externalId: 'tm-event-100',
  title: 'Festival de Teste',
  description: 'Uma descrição de teste',
  imageUrl: 'https://images.example.com/banner.jpg',
  category: 'Festival',
  status: 'DRAFT',
  venueName: 'Allianz Parque',
  venueAddress: 'Av. Francisco Matarazzo, 1705, São Paulo - SP',
  startsAt: '2026-12-25T20:00:00Z',
  createdAt: '2026-08-15T12:00:00Z',
  updatedAt: '2026-08-15T12:00:00Z',
};

const validSectors: TicketSectorResponse[] = [
  {
    id: 'sec-1',
    eventId: 'ev-test-1',
    name: 'Pista',
    description: 'Pista geral',
    capacity: 200,
    availableQuantity: 200,
    price: 100.0,
    createdAt: '2026-08-15T12:00:00Z',
    updatedAt: '2026-08-15T12:00:00Z',
  },
];

describe('PublicationChecklist (Superfície S13)', () => {
  it('renders ready state and enables publication button when all mandatory conditions are satisfied', () => {
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    render(
      <PublicationChecklist
        event={baseEvent}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={false}
      />
    );

    expect(screen.getByRole('heading', { name: /Revisão e Publicação do Evento/i })).toBeDefined();
    expect(screen.getByText(/Todas as condições obrigatórias foram atendidas/i)).toBeDefined();

    const publishButton = screen.getByRole('button', { name: /Publicar Evento/i }) as HTMLButtonElement;
    expect(publishButton.disabled).toBe(false);
  });

  it('identifies pending mandatory items, disables publish CTA, and triggers focus on pending field', async () => {
    const user = userEvent.setup();
    const onFocusMock = vi.fn();
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    const eventWithPendencies: EventResponse = {
      ...baseEvent,
      venueName: undefined,
      venueAddress: undefined,
      startsAt: undefined,
    };

    render(
      <PublicationChecklist
        event={eventWithPendencies}
        sectors={[]} // Sem setores
        onFocusField={onFocusMock}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={false}
      />
    );

    expect(screen.getByText(/Existem.*pendência\(s\) obrigatória\(s\)/i)).toBeDefined();

    const publishButton = screen.getByRole('button', { name: /Publicar Evento/i }) as HTMLButtonElement;
    expect(publishButton.disabled).toBe(true);

    // Botão de focar no campo Nome do Local
    const fixVenueNameBtn = screen.getByRole('button', { name: /Corrigir pendência de Nome do Local/i });
    await user.click(fixVenueNameBtn);
    expect(onFocusMock).toHaveBeenCalledWith('event-venue-name-input');

    // Botão de focar no campo Endereço do Local
    const fixVenueAddrBtn = screen.getByRole('button', { name: /Corrigir pendência de Endereço do Local/i });
    await user.click(fixVenueAddrBtn);
    expect(onFocusMock).toHaveBeenCalledWith('event-venue-address-input');

    // Botão de focar no setor
    const fixSectorBtn = screen.getByRole('button', { name: /Corrigir pendência de Setores de Ingressos/i });
    await user.click(fixSectorBtn);
    expect(onFocusMock).toHaveBeenCalledWith('add-sector-btn');
  });

  it('submits publication when publish button is clicked in ready state', async () => {
    const user = userEvent.setup();
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    render(
      <PublicationChecklist
        event={baseEvent}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={false}
      />
    );

    const publishButton = screen.getByRole('button', { name: /Publicar Evento/i });
    await user.click(publishButton);

    expect(onPublishMock).toHaveBeenCalledTimes(1);
  });

  it('displays busy state during publication with aria-busy and disabled button', () => {
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    render(
      <PublicationChecklist
        event={baseEvent}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={true}
        publishedSuccess={false}
      />
    );

    const publishButton = screen.getByRole('button', { name: /Publicando evento…/i }) as HTMLButtonElement;
    expect(publishButton.disabled).toBe(true);
    expect(document.querySelector('.publication-checklist-section')?.getAttribute('aria-busy')).toBe('true');
  });

  it('displays persistent success message when event is published', () => {
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    const publishedEvent: EventResponse = {
      ...baseEvent,
      status: 'PUBLISHED',
    };

    render(
      <PublicationChecklist
        event={publishedEvent}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={true}
      />
    );

    expect(screen.getByText(/Evento Publicado!/i)).toBeDefined();
    expect(screen.queryByRole('button', { name: /Publicar Evento/i })).toBeNull();
  });

  it('displays error alert when error prop is provided', () => {
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    render(
      <PublicationChecklist
        event={baseEvent}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={false}
        error="Erro autoritativo do backend ao publicar."
      />
    );

    const alert = screen.getByRole('alert');
    expect(alert.textContent).toContain('Erro autoritativo do backend ao publicar.');
  });

  it('allows publication without optional fields (description, imageUrl, category)', () => {
    const onPublishMock = vi.fn().mockResolvedValue(undefined);

    const eventWithoutOptionals: EventResponse = {
      ...baseEvent,
      description: undefined,
      imageUrl: undefined,
      category: undefined,
    };

    render(
      <PublicationChecklist
        event={eventWithoutOptionals}
        sectors={validSectors}
        onPublish={onPublishMock}
        isPublishing={false}
        publishedSuccess={false}
      />
    );

    const publishButton = screen.getByRole('button', { name: /Publicar Evento/i }) as HTMLButtonElement;
    expect(publishButton.disabled).toBe(false);
    expect(screen.getByText(/Opcional - não bloqueia publicação/i)).toBeDefined();
  });
});
