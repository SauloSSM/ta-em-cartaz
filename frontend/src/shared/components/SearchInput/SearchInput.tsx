import { type ChangeEvent } from 'react';
import './SearchInput.css';

export type SearchInputProps = {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  onClear?: () => void;
  placeholder?: string;
  label?: string;
  className?: string;
  autoFocus?: boolean;
};

export function SearchInput({
  id = 'tc-search-input',
  value,
  onChange,
  onClear,
  placeholder = 'Buscar eventos...',
  label = 'Buscar eventos',
  className = '',
  autoFocus = false,
}: SearchInputProps) {
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value);
  };

  const handleClear = () => {
    onChange('');
    if (onClear) {
      onClear();
    }
  };

  return (
    <div className={`tc-search ${className}`}>
      <label htmlFor={id} className="tc-visually-hidden">
        {label}
      </label>
      <div className="tc-search__wrapper">
        <svg
          className="tc-search__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          id={id}
          type="search"
          className="tc-search__input"
          placeholder={placeholder}
          value={value}
          onChange={handleChange}
          autoComplete="off"
          autoFocus={autoFocus}
        />
        {value && (
          <button
            type="button"
            className="tc-search__clear"
            onClick={handleClear}
            aria-label="Limpar busca"
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
}
