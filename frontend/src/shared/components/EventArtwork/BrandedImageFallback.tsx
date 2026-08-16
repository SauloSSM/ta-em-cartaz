import { BrandSeal } from '../Brand/BrandSeal';

export type EventTheme = 'orange' | 'pink' | 'yellow' | 'green';

export function getEventTheme(id?: string): EventTheme {
  if (!id) return 'orange';
  let hash = 0;
  for (let i = 0; i < id.length; i++) {
    hash = (hash << 5) - hash + id.charCodeAt(i);
    hash |= 0;
  }
  const themes: EventTheme[] = ['orange', 'pink', 'yellow', 'green'];
  const index = Math.abs(hash) % themes.length;
  return themes[index]!;
}

export type BrandedImageFallbackProps = {
  eventId?: string;
  eventTitle?: string;
  theme?: EventTheme;
  className?: string;
};

export function BrandedImageFallback({
  eventId,
  eventTitle,
  theme,
  className = '',
}: BrandedImageFallbackProps) {
  const activeTheme = theme ?? getEventTheme(eventId);

  return (
    <div
      className={`tc-image-fallback tc-image-fallback--${activeTheme} ${className}`}
      role="img"
      aria-label={eventTitle ? `Arte padrão para ${eventTitle}` : 'Imagem padrão do evento'}
    >
      <BrandSeal variant="primary" size={44} className="tc-image-fallback__seal" />
      {eventTitle && (
        <span className="tc-image-fallback__title">{eventTitle}</span>
      )}
      <span className="tc-image-fallback__motif">CULTURA QUE CONECTA.</span>

      {/* Halftone / geometric SVG background motif */}
      <svg
        className="tc-image-fallback__halftone"
        viewBox="0 0 100 100"
        fill="currentColor"
        aria-hidden="true"
      >
        <pattern id={`dots-${activeTheme}`} x="0" y="0" width="16" height="16" patternUnits="userSpaceOnUse">
          <circle cx="4" cy="4" r="2.5" />
        </pattern>
        <rect width="100" height="100" fill={`url(#dots-${activeTheme})`} />
      </svg>
    </div>
  );
}
