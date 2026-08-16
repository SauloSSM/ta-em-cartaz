import './HomeHero.css';

export function HomeHero() {
  return (
    <section className="tc-hero" aria-labelledby="hero-title">
      <div className="tc-container tc-hero__container">
        {/* Left Typography Block */}
        <div className="tc-hero__content">
          <div className="tc-hero__title-group">
            {/* Green Asterisk / Star decoration */}
            <svg
              className="tc-hero__star-decoration"
              viewBox="0 0 100 100"
              fill="currentColor"
              aria-hidden="true"
            >
              <polygon points="50,5 61,38 95,25 71,50 95,75 61,62 50,95 39,62 5,75 29,50 5,25 39,38" />
            </svg>

            <h1 id="hero-title" className="tc-hero__title" aria-label="Catálogo de Eventos">
              TÁ EM CARTAZ
            </h1>
          </div>

          <div className="tc-hero__tagline-box">
            <p className="tc-hero__tagline">
              A cultura move.{' '}
              <span className="tc-hero__highlight">A gente conecta.</span>
            </p>
          </div>
        </div>

        {/* Right Editorial Collage */}
        <div className="tc-hero__collage" aria-hidden="true">
          {/* Orange sun disk */}
          <div className="tc-hero__sun" />

          {/* Halftone dots */}
          <svg className="tc-hero__halftone" viewBox="0 0 100 100">
            <pattern id="hero-dots" x="0" y="0" width="12" height="12" patternUnits="userSpaceOnUse">
              <circle cx="3" cy="3" r="2" fill="var(--brand-ink)" />
            </pattern>
            <rect width="100" height="100" fill="url(#hero-dots)" />
          </svg>

          {/* Crowd / Stage silhouette vector */}
          <svg
            className="tc-hero__crowd-svg"
            viewBox="0 0 500 300"
            fill="currentColor"
            preserveAspectRatio="xMidYMax meet"
          >
            {/* Stage lights beams */}
            <path
              d="M 50 300 L 150 50 L 250 300 Z"
              fill="rgba(236, 167, 5, 0.12)"
            />
            <path
              d="M 280 300 L 350 20 L 450 300 Z"
              fill="rgba(242, 87, 9, 0.15)"
            />
            {/* Crowd arms and silhouettes */}
            <path
              d="M 0 300 
                 C 20 270 30 240 40 230
                 C 45 225 50 240 60 220
                 C 65 210 70 215 75 190
                 C 80 180 85 190 90 220
                 C 100 210 110 195 115 180
                 C 120 165 125 170 130 195
                 C 140 180 150 160 155 140
                 C 160 130 165 140 170 170
                 C 180 160 190 150 195 130
                 C 200 120 205 130 210 160
                 C 220 150 230 140 235 120
                 C 240 110 245 120 250 150
                 C 260 140 270 130 275 110
                 C 280 100 285 110 290 140
                 C 300 130 310 120 315 100
                 C 320 90 325 100 330 130
                 C 340 120 350 110 355 90
                 C 360 80 365 90 370 120
                 C 380 130 390 120 395 105
                 C 400 95 405 105 410 135
                 C 420 140 430 130 435 115
                 C 440 105 445 115 450 145
                 C 460 150 470 140 475 125
                 C 480 115 485 125 490 160
                 C 495 180 500 220 500 300 Z"
              fill="var(--brand-ink)"
            />
          </svg>

          {/* Green note sticker */}
          <div className="tc-hero__note">
            A CULTURA MOVE.
            <br />
            A GENTE CONECTA.
          </div>

          {/* Pink badge */}
          <div className="tc-hero__badge">
            <span>VIVA</span>
            <span>AGORA</span>
            <span style={{ fontSize: '14px', lineHeight: '0.8' }}>→</span>
          </div>
        </div>
      </div>
    </section>
  );
}
