import { useState, useRef } from 'react';
import { AboutProjectModal } from './AboutProjectModal';
import './Footer.css';

const tcSealPinkPng = new URL('../assets/ta-em-cartaz/brand/tc-seal-pink.png', import.meta.url).href;

export type FooterProps = {
  onNavigateCatalog?: () => void;
  onNavigateLogin?: () => void;
};

export function Footer(_props?: FooterProps) {
  const [isAboutOpen, setIsAboutOpen] = useState(false);
  const aboutButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <footer className="tc-footer" role="contentinfo">
        <div className="tc-container tc-footer__inner">
          {/* Brand Block */}
          <div className="tc-footer__brand-block">
            <img
              src={tcSealPinkPng}
              alt=""
              aria-hidden="true"
              className="tc-footer__seal-img"
            />
            <div className="tc-footer__brand-info">
              <span className="tc-footer__brand-name">TÁ EM CARTAZ</span>
              <span className="tc-footer__handle">@TAEMCARTAZ.BR</span>
            </div>
          </div>

          {/* Links Group */}
          <nav aria-label="Links do rodapé" className="tc-footer__links-group">
            <button
              ref={aboutButtonRef}
              type="button"
              className="tc-footer__link tc-footer__link--btn"
              onClick={() => setIsAboutOpen(true)}
            >
              Sobre o Projeto
            </button>
            <a
              href="https://github.com/SauloSSM"
              target="_blank"
              rel="noopener noreferrer"
              className="tc-footer__link"
            >
              GitHub
            </a>
            <a
              href="https://www.linkedin.com/in/saulo-da-silva-stuque-menegucci/"
              target="_blank"
              rel="noopener noreferrer"
              className="tc-footer__link"
            >
              LinkedIn
            </a>
          </nav>
        </div>
      </footer>

      <AboutProjectModal
        isOpen={isAboutOpen}
        onClose={() => setIsAboutOpen(false)}
        triggerRef={aboutButtonRef}
      />
    </>
  );
}
