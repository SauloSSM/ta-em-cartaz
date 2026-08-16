import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QuantityStepper } from '../QuantityStepper';

describe('QuantityStepper component', () => {
  it('renderiza o valor inicial e labels acessíveis', () => {
    render(<QuantityStepper value={2} min={1} max={6} onChange={vi.fn()} />);

    expect(screen.getByText('Quantidade de ingressos')).toBeDefined();
    expect(screen.getByTestId('quantity-stepper-value').textContent).toContain('2');
    expect((screen.getByRole('button', { name: 'Diminuir quantidade' }) as HTMLButtonElement).disabled).toBe(false);
    expect((screen.getByRole('button', { name: 'Aumentar quantidade' }) as HTMLButtonElement).disabled).toBe(false);
  });

  it('desabilita botão de diminuir no valor mínimo', () => {
    render(<QuantityStepper value={1} min={1} max={6} onChange={vi.fn()} />);

    expect((screen.getByRole('button', { name: 'Diminuir quantidade' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Aumentar quantidade' }) as HTMLButtonElement).disabled).toBe(false);
  });

  it('desabilita botão de aumentar no valor máximo', () => {
    render(<QuantityStepper value={6} min={1} max={6} onChange={vi.fn()} />);

    expect((screen.getByRole('button', { name: 'Diminuir quantidade' }) as HTMLButtonElement).disabled).toBe(false);
    expect((screen.getByRole('button', { name: 'Aumentar quantidade' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('respeita limite máximo customizado (ex: estoque limitado a 3)', () => {
    render(<QuantityStepper value={3} min={1} max={3} onChange={vi.fn()} />);

    expect((screen.getByRole('button', { name: 'Aumentar quantidade' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('chama onChange com valor incrementado ao clicar no botão +', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    render(<QuantityStepper value={2} min={1} max={6} onChange={handleChange} />);

    await user.click(screen.getByRole('button', { name: 'Aumentar quantidade' }));
    expect(handleChange).toHaveBeenCalledWith(3);
  });

  it('chama onChange com valor decrementado ao clicar no botão -', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    render(<QuantityStepper value={4} min={1} max={6} onChange={handleChange} />);

    await user.click(screen.getByRole('button', { name: 'Diminuir quantidade' }));
    expect(handleChange).toHaveBeenCalledWith(3);
  });

  it('desabilita todos os botões quando disabled=true', () => {
    render(<QuantityStepper value={3} min={1} max={6} disabled onChange={vi.fn()} />);

    expect((screen.getByRole('button', { name: 'Diminuir quantidade' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Aumentar quantidade' }) as HTMLButtonElement).disabled).toBe(true);
  });
});
