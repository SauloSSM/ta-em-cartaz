import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EventArtwork } from '../EventArtwork/EventArtwork';
import { getEventTheme } from '../EventArtwork/BrandedImageFallback';

describe('EventArtwork & BrandedImageFallback', () => {
  it('renders image when imageUrl is valid', () => {
    render(
      <EventArtwork
        eventId="ev-123"
        eventTitle="Festival de Verão"
        imageUrl="https://images.example.com/summer.jpg"
      />,
    );

    const img = screen.getByAltText('Banner do evento Festival de Verão');
    expect(img).toBeDefined();
  });

  it('renders BrandedImageFallback when imageUrl is null or empty', () => {
    render(
      <EventArtwork
        eventId="ev-123"
        eventTitle="Festival de Verão"
        imageUrl={null}
      />,
    );

    expect(screen.getByRole('img', { name: 'Arte padrão para Festival de Verão' })).toBeDefined();
    expect(screen.getByText('CULTURA QUE CONECTA.')).toBeDefined();
  });

  it('switches to BrandedImageFallback upon image error', () => {
    render(
      <EventArtwork
        eventId="ev-123"
        eventTitle="Festival de Verão"
        imageUrl="https://images.example.com/broken.jpg"
      />,
    );

    const img = screen.getByAltText('Banner do evento Festival de Verão');
    fireEvent.error(img);

    expect(screen.getByRole('img', { name: 'Arte padrão para Festival de Verão' })).toBeDefined();
    expect(screen.getByText('CULTURA QUE CONECTA.')).toBeDefined();
  });

  it('computes deterministic theme from eventId', () => {
    const theme1 = getEventTheme('00000000-0000-0000-0000-000000000001');
    const theme2 = getEventTheme('00000000-0000-0000-0000-000000000001');
    const theme3 = getEventTheme('00000000-0000-0000-0000-000000000002');

    expect(theme1).toBe(theme2);
    expect(['orange', 'pink', 'yellow', 'green']).toContain(theme1);
    expect(['orange', 'pink', 'yellow', 'green']).toContain(theme3);
  });
});
