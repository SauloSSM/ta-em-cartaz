const sealBlackPng = new URL('../../../assets/ta-em-cartaz/brand/tc-seal-black.png', import.meta.url).href;
const sealPinkPng = new URL('../../../assets/ta-em-cartaz/brand/tc-seal-pink.png', import.meta.url).href;

export type BrandSealVariant = 'primary' | 'inverse' | 'accent';

export type BrandSealProps = {
  variant?: BrandSealVariant;
  size?: number;
  className?: string;
  'aria-label'?: string;
};

export function BrandSeal({
  variant = 'primary',
  size = 40,
  className = '',
  'aria-label': ariaLabel,
}: BrandSealProps) {
  const hasLabel = Boolean(ariaLabel);
  const sealSrc = variant === 'accent' ? sealPinkPng : sealBlackPng;

  return (
    <div
      className={`tc-brand-seal tc-brand-seal--${variant} ${className}`}
      style={{
        width: size,
        height: size,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}
      role={hasLabel ? 'img' : 'presentation'}
      aria-label={ariaLabel}
      aria-hidden={!hasLabel}
    >
      <img
        src={sealSrc}
        alt=""
        aria-hidden="true"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'contain',
          display: 'block',
          ...(variant === 'inverse' ? { filter: 'invert(1)' } : {}),
        }}
      />
    </div>
  );
}
