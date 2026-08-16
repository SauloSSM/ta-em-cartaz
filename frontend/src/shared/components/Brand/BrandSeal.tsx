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
  // Variant colors:
  // primary: black seal (#10100F), light TC (#F3E5D8)
  // inverse: light seal (#F3E5D8), dark TC (#10100F)
  // accent: pink seal (#E774A4), dark TC (#10100F)
  const colors = {
    primary: { seal: 'var(--brand-ink, #10100F)', text: 'var(--brand-paper, #F3E5D8)' },
    inverse: { seal: 'var(--brand-paper, #F3E5D8)', text: 'var(--brand-ink, #10100F)' },
    accent: { seal: 'var(--brand-pink, #E774A4)', text: 'var(--brand-ink, #10100F)' },
  }[variant];

  const hasLabel = Boolean(ariaLabel);

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      className={`tc-brand-seal tc-brand-seal--${variant} ${className}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role={hasLabel ? 'img' : 'presentation'}
      aria-label={ariaLabel}
      aria-hidden={!hasLabel}
    >
      {/* 16-point scallop / serrated stamp circle */}
      <path
        d="M50 0
           C54 8 59 10 68 8
           C75 7 81 12 83 19
           C86 27 91 30 98 33
           C103 36 104 43 102 50
           C99 57 101 62 105 69
           C107 76 103 83 97 86
           C90 89 87 95 86 102
           C84 108 77 112 70 110
           C62 108 57 111 50 116
           C43 111 38 108 30 110
           C23 112 16 108 14 102
           C13 95 10 89 3 86
           C-3 83 -7 76 -5 69
           C-1 62 1 57 -2 50
           C-4 43 -3 36 2 33
           C9 30 14 27 17 19
           C19 12 25 7 32 8
           C41 10 46 8 50 0 Z"
        transform="translate(10, -5) scale(0.8)"
        fill={colors.seal}
      />
      {/* Fallback geometric 16-pointed star polygon if needed, or refined badge */}
      <polygon
        points="50,4 61,12 75,9 81,22 95,25 94,39 104,49 96,60 100,74 87,80 84,94 70,93 61,104 49,97 38,103 30,92 16,92 14,78 2,71 7,57 -1,46 7,34 5,20 19,16 23,3 37,7"
        fill={colors.seal}
      />
      <text
        x="50"
        y="62"
        fontFamily="var(--font-display, Anton, sans-serif)"
        fontSize="44"
        fontWeight="bold"
        fill={colors.text}
        textAnchor="middle"
        letterSpacing="-0.04em"
      >
        TC
      </text>
    </svg>
  );
}
