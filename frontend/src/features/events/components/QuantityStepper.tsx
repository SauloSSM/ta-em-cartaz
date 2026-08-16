export type QuantityStepperProps = {
  value: number;
  min?: number;
  max?: number;
  disabled?: boolean;
  onChange: (newValue: number) => void;
  id?: string;
  label?: string;
};

export function QuantityStepper({
  value,
  min = 1,
  max = 6,
  disabled = false,
  onChange,
  id = 'quantity-stepper',
  label = 'Quantidade de ingressos',
}: QuantityStepperProps) {
  const effectiveMax = Math.min(6, Math.max(min, max));
  const canDecrement = !disabled && value > min;
  const canIncrement = !disabled && value < effectiveMax;

  const handleDecrement = () => {
    if (canDecrement) {
      onChange(Math.max(min, value - 1));
    }
  };

  const handleIncrement = () => {
    if (canIncrement) {
      onChange(Math.min(effectiveMax, value + 1));
    }
  };

  return (
    <div className="edt-quantity-stepper-container">
      <label id={`${id}-label`} htmlFor={id} className="edt-quantity-stepper__label">
        {label}
      </label>

      <div
        className={`edt-quantity-stepper ${disabled ? 'edt-quantity-stepper--disabled' : ''}`}
        role="group"
        aria-labelledby={`${id}-label`}
      >
        <button
          type="button"
          className="edt-quantity-stepper__btn edt-quantity-stepper__btn--decrement"
          onClick={handleDecrement}
          disabled={!canDecrement}
          aria-label="Diminuir quantidade"
          data-testid="quantity-stepper-decrement"
        >
          −
        </button>

        <div
          id={id}
          className="edt-quantity-stepper__value"
          role="status"
          aria-live="polite"
          aria-atomic="true"
          data-testid="quantity-stepper-value"
        >
          <span className="edt-quantity-stepper__number">{value}</span>
          <span className="edt-visually-hidden">
            {value} {value === 1 ? 'ingresso selecionado' : 'ingressos selecionados'} (mínimo: {min}, máximo: {effectiveMax})
          </span>
        </div>

        <button
          type="button"
          className="edt-quantity-stepper__btn edt-quantity-stepper__btn--increment"
          onClick={handleIncrement}
          disabled={!canIncrement}
          aria-label="Aumentar quantidade"
          data-testid="quantity-stepper-increment"
        >
          +
        </button>
      </div>

      <p className="edt-quantity-stepper__hint" id={`${id}-hint`}>
        Limite de 1 a 6 ingressos por compra, sujeito à disponibilidade.
      </p>
    </div>
  );
}
