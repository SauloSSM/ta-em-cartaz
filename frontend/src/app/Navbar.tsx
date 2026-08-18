import { useState, useRef, useEffect } from 'react';
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
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const menuToggleRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu on Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileMenuOpen) {
        setMobileMenuOpen(false);
        menuToggleRef.current?.focus();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileMenuOpen]);

  const handleCatalogClick = () => {
    setMobileMenuOpen(false);
    onNavigateCatalog();
  };

  const handleLoginClick = () => {
    setMobileMenuOpen(false);
    onNavigateLogin();
  };

  const handleLogoutClick = () => {
    setMobileMenuOpen(false);
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

          {/* Mobile Menu Button */}
          <button
            ref={menuToggleRef}
            type="button"
            className="tc-header__menu-btn"
            onClick={() => setMobileMenuOpen((prev) => !prev)}
            aria-label={mobileMenuOpen ? 'Fechar menu' : 'Abrir menu de navegação'}
            aria-expanded={mobileMenuOpen}
            aria-controls="tc-mobile-nav"
          >
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              {mobileMenuOpen ? (
                <>
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </>
              ) : (
                <>
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </>
              )}
            </svg>
          </button>
        </div>
      </div>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div
          ref={menuRef}
          id="tc-mobile-nav"
          className="tc-mobile-menu"
          role="dialog"
          aria-label="Menu de navegação"
          aria-modal="true"
        >
          <nav className="tc-mobile-menu__nav">
            {!user ? (
              <button
                type="button"
                className="tc-mobile-menu__link"
                onClick={handleLoginClick}
              >
                Entrar na minha conta
              </button>
            ) : (
              <button
                type="button"
                className="tc-mobile-menu__link"
                onClick={handleLogoutClick}
                disabled={isLoggingOut}
                aria-busy={isLoggingOut ? 'true' : undefined}
              >
                {isLoggingOut ? 'Saindo…' : `Sair (${user.email})`}
              </button>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
