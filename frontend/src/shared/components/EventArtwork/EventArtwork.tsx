import { useState } from 'react';
import { BrandedImageFallback, type EventTheme } from './BrandedImageFallback';
import './EventArtwork.css';

export type EventArtworkProps = {
  eventId?: string;
  eventTitle: string;
  imageUrl?: string | null;
  theme?: EventTheme;
  aspectRatio?: '16/9' | '4/3' | '1/1' | '3/4' | '21/9' | 'auto';
  className?: string;
  loading?: 'lazy' | 'eager';
};

export function EventArtwork({
  eventId,
  eventTitle,
  imageUrl,
  theme,
  aspectRatio = '16/9',
  className = '',
  loading = 'lazy',
}: EventArtworkProps) {
  const [loadError, setLoadError] = useState(false);

  const showFallback = !imageUrl || loadError;

  return (
    <div
      className={`tc-artwork ${className}`}
      style={{ aspectRatio: aspectRatio !== 'auto' ? aspectRatio : undefined }}
    >
      {showFallback ? (
        <BrandedImageFallback
          eventId={eventId}
          eventTitle={eventTitle}
          theme={theme}
        />
      ) : (
        <img
          src={imageUrl}
          alt={`Banner do evento ${eventTitle}`}
          className="tc-artwork__img"
          loading={loading}
          onError={() => setLoadError(true)}
        />
      )}
    </div>
  );
}
