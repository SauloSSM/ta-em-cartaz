import { BrandSeal } from '../shared/components/Brand/BrandSeal';
import './Footer.css';

export type FooterProps = {
  onNavigateCatalog?: () => void;
  onNavigateLogin?: () => void;
};

export function Footer({ onNavigateCatalog, onNavigateLogin }: FooterProps) {
  return (
    <footer className="tc-footer" role="contentinfo">
      <div className="tc-container tc-footer__inner">
        {/* Brand Block */}
        <div className="tc-footer__brand-block">
          <BrandSeal variant="accent" size={48} />
          <div className="tc-footer__brand-info">
            <span className="tc-footer__brand-name">TÁ EM CARTAZ</span>
            <span className="tc-footer__handle">@TAEMCARTAZ.BR</span>
          </div>
        </div>

        {/* Real Navigation Links */}
        <nav aria-label="Links do rodapé" className="tc-footer__links-group">
          {onNavigateCatalog && (
            <button
              type="button"
              className="tc-footer__link"
              onClick={onNavigateCatalog}
            >
              Eventos
            </button>
          )}
          {onNavigateLogin && (
            <button
              type="button"
              className="tc-footer__link"
              onClick={onNavigateLogin}
            >
              Minha Conta
            </button>
          )}
          <span className="tc-footer__link" style={{ cursor: 'default' }}>
            Sobre o Projeto
          </span>
          <span className="tc-footer__link" style={{ cursor: 'default' }}>
            Termos de Uso
          </span>
          <span className="tc-footer__link" style={{ cursor: 'default' }}>
            Privacidade
          </span>
        </nav>

        {/* Stamp Motif */}
        <div className="tc-footer__stamp" aria-hidden="true">
          <span className="tc-footer__stamp-text">CULTURA QUE CONECTA.</span>
          <BrandSeal variant="primary" size={24} />
        </div>

        {/* Bottom Legal / Year */}
        <div className="tc-footer__bottom">
          <span>&copy; {new Date().getFullYear()} Tá em Cartaz — Plataforma de Eventos e Ingressos.</span>
          <span>Desafio Elite Dev 2026</span>
        </div>
      </div>
    </footer>
  );
}
