import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Button } from '../Button/Button';

describe('Button', () => {
  it('renders children and handles clicks', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();
    render(<Button onClick={handleClick}>Acessar</Button>);

    const btn = screen.getByRole('button', { name: 'Acessar' });
    await user.click(btn);

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('renders loading state with spinner, aria-busy and disabled', () => {
    render(<Button loading loadingText="Carregando...">Enviar</Button>);

    const btn = screen.getByRole('button', { name: 'Carregando...' }) as HTMLButtonElement;
    expect(btn.getAttribute('aria-busy')).toBe('true');
    expect(btn.disabled).toBe(true);
  });

  it('renders arrow indicators when showArrow is true', () => {
    render(<Button showArrow arrowDirection="right">Ver mais</Button>);
    expect(screen.getByText('→')).toBeDefined();
  });
});
