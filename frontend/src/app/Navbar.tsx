import { useState, useRef, useEffect } from 'react';
import { BrandSeal } from '../shared/components/Brand/BrandSeal';
import { Button } from '../shared/components/Button/Button';
import type { SessionUser } from './session/useSession';
import './Navbar.css';

export type NavbarProps = {
  user: SessionUser | null;
  activeView: 'catalog' | 'login' | 'detail' | 'authenticated' | 'shared-ticket';
  onNavigateCatalog: () => void;
  onNavigateLogin: () => void;
  onLogout?: () => void;
};

export function Navbar({
  user,
  activeView,
  onNavigateCatalog,
  onNavigateLogin,
  onLogout,
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
          <BrandSeal variant="primary" size={38} />
          <span className="tc-header__logo-text">TÁ EM CARTAZ</span>
        </button>

        {/* Desktop Navigation */}
        <nav aria-label="Navegação principal" className="tc-header__nav-desktop edt-top-nav">
          <button
            type="button"
            className={`tc-header__nav-link edt-nav-link ${
              activeView === 'catalog' || activeView === 'detail'
                ? 'tc-header__nav-link--active edt-nav-link--active'
                : ''
            }`}
            onClick={handleCatalogClick}
            aria-label="Catálogo de Eventos"
            aria-current={
              activeView === 'catalog' || activeView === 'detail'
                ? 'page'
                : undefined
            }
          >
            Eventos
          </button>
        </nav>

        {/* Actions / Auth */}
        <div className="tc-header__actions">
          {user ? (
            <div className="tc-header__user-badge">
              <span className="tc-header__role-tag">
                Minha Conta
              </span>
              {onLogout && (
                <Button
                  variant="secondary"
                  className="tc-header__btn-login"
                  onClick={handleLogoutClick}
                >
                  Sair
                </Button>
              )}
            </div>
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
            <button
              type="button"
              className="tc-mobile-menu__link"
              onClick={handleCatalogClick}
            >
              Eventos
            </button>
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
              >
                Sair ({user.email})
              </button>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
