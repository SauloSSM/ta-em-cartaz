import { Button } from '../shared/components/Button/Button';
import type { SessionUser } from './session/useSession';
import './Navbar.css';

const tcSealBlackPng = new URL('../assets/ta-em-cartaz/brand/tc-seal-black.png', import.meta.url).href;
const wordmarkBlackPng = new URL('../assets/ta-em-cartaz/brand/ta-em-cartaz-wordmark-black.png', import.meta.url).href;

export type NavbarProps = {
  user: SessionUser | null;
  activeView: 'catalog' | 'login' | 'detail' | 'authenticated' | 'shared-ticket';
  onNavigateCatalog: () => void;
  onNavigateLogin: () => void;
  onLogout?: () => void;
  isLoggingOut?: boolean;
};

export function Navbar({
  user,
  activeView: _activeView,
  onNavigateCatalog,
  onNavigateLogin,
  onLogout,
  isLoggingOut = false,
}: NavbarProps) {
  const handleCatalogClick = () => {
    onNavigateCatalog();
  };

  const handleLoginClick = () => {
    onNavigateLogin();
  };

  const handleLogoutClick = () => {
    if (onLogout) {
      onLogout();
    }
  };

  return (
    <header className="tc-header" role="banner">
      <div className="tc-header__inner">
        {/* Brand Link */}
        <button
          type="button"
          className="tc-header__brand"
          onClick={handleCatalogClick}
          aria-label="Tá em Cartaz — Página inicial de eventos"
        >
          <img
            src={tcSealBlackPng}
            alt=""
            aria-hidden="true"
            className="tc-header__seal-img"
          />
          <img
            src={wordmarkBlackPng}
            alt=""
            aria-hidden="true"
            className="tc-header__wordmark-img"
          />
          <span className="tc-visually-hidden">TÁ EM CARTAZ</span>
        </button>

        {/* Customer navigation slot for authenticated customer actions */}
        <div id="tc-header-customer-nav-slot" className="tc-header__customer-nav-slot" />

        {/* The public catalog mounts its real search form here via a React portal. */}
        <div id="tc-header-search-slot" className="tc-header__search-slot" />

        {/* Actions / Auth */}
        <div className="tc-header__actions">
          {user ? (
            onLogout ? (
              <Button
                variant="secondary"
                className="tc-header__btn-login tc-header__btn-login--logout"
                onClick={handleLogoutClick}
                loading={isLoggingOut}
                loadingText="Saindo…"
              >
                Sair
              </Button>
            ) : null
          ) : (
            <Button
              variant="primary"
              className="tc-header__btn-login edt-nav-link"
              onClick={handleLoginClick}
              aria-label="Acessar conta"
            >
              Entrar
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
