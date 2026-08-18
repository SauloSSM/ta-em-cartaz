import type { ReactNode } from 'react';
import './HomeHero.css';

const homeHeroCompositeSvg = new URL(
  '../../../assets/ta-em-cartaz/home/home-hero-composite.png',
  import.meta.url,
).href;

export type HomeHeroProps = {
  searchSlot?: ReactNode;
};

/**
 * The hero artwork is a canonical composition exported from the Canva anchor.
 * Keeping the static art as a single SVG preserves the designer's exact collage
 * while the interactive search remains real React UI.
 */
export function HomeHero({ searchSlot }: HomeHeroProps) {
  return (
    <section className="tc-hero" aria-labelledby="hero-title">
      <h1 id="hero-title" className="tc-visually-hidden">
        Catálogo de Eventos
      </h1>
      <span className="tc-visually-hidden">TÁ EM CARTAZ</span>
      <p className="tc-visually-hidden">
        A cultura move. <span>A gente conecta.</span>
      </p>

      <div className="tc-hero__artwork-wrap" aria-hidden="true">
        <img
          src={homeHeroCompositeSvg}
          alt=""
          className="tc-hero__artwork"
        />
      </div>

      {searchSlot ? <div className="tc-hero__search-slot">{searchSlot}</div> : null}
    </section>
  );
}
