import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HomeHero } from '../HomeHero';
import { CategoryStrip } from '../CategoryStrip';

describe('HomeHero & CategoryStrip', () => {
  it('renders HomeHero with expressive display headline and culture tagline', () => {
    render(<HomeHero />);

    expect(screen.getByRole('heading', { level: 1 })).toBeDefined();
    expect(screen.getByText('TÁ EM CARTAZ')).toBeDefined();
    expect(screen.getByText(/A cultura move\./)).toBeDefined();
    expect(screen.getByText('A gente conecta.')).toBeDefined();
  });

  it('renders CategoryStrip with all 4 visual motif sections', () => {
    render(<CategoryStrip />);

    expect(screen.getByRole('heading', { level: 2, name: 'SHOWS' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'FESTIVAIS' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'CULTURA' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: 'PERTO DE VOCÊ' })).toBeDefined();
  });
});
