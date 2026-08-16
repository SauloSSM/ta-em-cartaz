import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SearchInput } from '../SearchInput/SearchInput';

describe('SearchInput', () => {
  it('renders input with placeholder and accessibility label', () => {
    render(<SearchInput value="" onChange={vi.fn()} placeholder="Buscar eventos..." />);

    expect(screen.getByPlaceholderText('Buscar eventos...')).toBeDefined();
    expect(screen.getByLabelText('Buscar eventos')).toBeDefined();
  });

  it('updates value and allows clearing via clear button', async () => {
    const handleChange = vi.fn();
    const handleClear = vi.fn();
    const user = userEvent.setup();

    render(
      <SearchInput
        value="Festival"
        onChange={handleChange}
        onClear={handleClear}
      />,
    );

    const clearBtn = screen.getByRole('button', { name: 'Limpar busca' });
    await user.click(clearBtn);

    expect(handleChange).toHaveBeenCalledWith('');
    expect(handleClear).toHaveBeenCalledTimes(1);
  });
});
