import './CategoryStrip.css';

export function CategoryStrip() {
  return (
    <section
      className="tc-category-strip"
      aria-label="Destaques de programação cultural"
    >
      <div className="tc-container">
        <div className="tc-category-strip__grid">
          {/* 1. Shows */}
          <div className="tc-category-card tc-category-card--shows">
            <div className="tc-category-card__header">
              <h2 className="tc-category-card__title">SHOWS</h2>
              <p className="tc-category-card__subtitle">Música ao vivo de todos os ritmos</p>
            </div>
            <svg
              className="tc-category-card__art"
              viewBox="0 0 100 80"
              fill="currentColor"
              aria-hidden="true"
            >
              {/* Microphone silhouette */}
              <rect x="50" y="10" width="16" height="30" rx="8" />
              <path d="M42 25 C42 42 74 42 74 25" stroke="currentColor" strokeWidth="4" fill="none" />
              <line x1="58" y1="45" x2="58" y2="70" stroke="currentColor" strokeWidth="4" />
              <line x1="45" y1="70" x2="71" y2="70" stroke="currentColor" strokeWidth="4" />
            </svg>
          </div>

          {/* 2. Festivais */}
          <div className="tc-category-card tc-category-card--festivais">
            <div className="tc-category-card__header">
              <h2 className="tc-category-card__title">FESTIVAIS</h2>
              <p className="tc-category-card__subtitle">Dias de encontros e experiências</p>
            </div>
            <svg
              className="tc-category-card__art"
              viewBox="0 0 100 80"
              fill="currentColor"
              aria-hidden="true"
            >
              {/* Hands in the air */}
              <path d="M20 80 L30 40 L40 50 L45 35 L55 45 L60 80 Z" />
              <path d="M65 80 L75 30 L85 45 L95 80 Z" />
            </svg>
          </div>

          {/* 3. Cultura */}
          <div className="tc-category-card tc-category-card--cultura">
            <div className="tc-category-card__header">
              <h2 className="tc-category-card__title">CULTURA</h2>
              <p className="tc-category-card__subtitle">Teatro, cinema e exposições</p>
            </div>
            <svg
              className="tc-category-card__art"
              viewBox="0 0 100 80"
              fill="currentColor"
              aria-hidden="true"
            >
              {/* Theater / Architectural geometry */}
              <polygon points="10,75 50,15 90,75" />
              <polygon points="25,75 50,35 75,75" fill="var(--brand-paper)" />
            </svg>
          </div>

          {/* 4. Perto de Você */}
          <div className="tc-category-card tc-category-card--perto">
            <div className="tc-category-card__header">
              <h2 className="tc-category-card__title">PERTO DE VOCÊ</h2>
              <p className="tc-category-card__subtitle">Eventos na sua cidade e região</p>
            </div>
            <svg
              className="tc-category-card__art"
              viewBox="0 0 100 80"
              fill="currentColor"
              aria-hidden="true"
            >
              {/* City skyline silhouette */}
              <rect x="20" y="30" width="18" height="50" />
              <rect x="42" y="15" width="22" height="65" />
              <rect x="68" y="35" width="16" height="45" />
            </svg>
          </div>
        </div>
      </div>
    </section>
  );
}
