import './Footer.css';

const tcSealPinkPng = new URL('../assets/ta-em-cartaz/brand/tc-seal-pink.png', import.meta.url).href;
const cultureConnectStampBlackPng = new URL('../assets/ta-em-cartaz/brand/culture-connect-stamp-black.png', import.meta.url).href;

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
          <img
            src={cultureConnectStampBlackPng}
            alt=""
            aria-hidden="true"
            className="tc-footer__stamp-img"
          />
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
