import type { ButtonHTMLAttributes, ReactNode } from 'react';
import './Button.css';

export type ButtonVariant = 'primary' | 'secondary' | 'link';

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  showArrow?: boolean;
  arrowDirection?: 'left' | 'right';
  loading?: boolean;
  loadingText?: string;
  children: ReactNode;
};

export function Button({
  variant = 'primary',
  showArrow = false,
  arrowDirection = 'right',
  loading = false,
  loadingText,
  disabled,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      className={`tc-btn tc-btn--${variant} ${className}`}
      disabled={isDisabled}
      aria-busy={loading ? 'true' : undefined}
      {...rest}
    >
      {loading && <span className="tc-btn__spinner" aria-hidden="true" />}
      {showArrow && arrowDirection === 'left' && !loading && (
        <span className="tc-btn__arrow" aria-hidden="true">
          ←
        </span>
      )}
      <span>{loading && loadingText ? loadingText : children}</span>
      {showArrow && arrowDirection === 'right' && !loading && (
        <span className="tc-btn__arrow" aria-hidden="true">
          →
        </span>
      )}
    </button>
  );
}
