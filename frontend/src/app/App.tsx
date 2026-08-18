import { useState, useEffect } from 'react';
import { AuthenticatedSession, LoginForm } from '../features/auth';
import {
  PublicEventCatalog,
  PublicEventDetail,
  type PublicEventResponse,
} from '../features/events';
import { PublicSharedTicket } from '../features/tickets';
import { useSession, type SessionState } from './session/useSession';
import { Navbar } from './Navbar';
import { Footer } from './Footer';
import './App.css';

export type AppProps = {
  initialAnonymousView?: 'catalog' | 'login' | 'detail' | 'shared-ticket';
  initialShareToken?: string;
};

export function App({ initialAnonymousView, initialShareToken }: AppProps) {
  const { state, setEmail, authenticate, endSession, retryBootstrap } = useSession();
  const [anonymousView, setAnonymousView] = useState<'catalog' | 'login' | 'detail' | 'shared-ticket'>(() => {
    if (initialAnonymousView !== undefined) {
      return initialAnonymousView;
    }
    if (typeof window !== 'undefined' && window.location?.pathname) {
      const match = window.location.pathname.match(/^\/t\/([^/]+)/);
      if (match && match[1]) {
        return 'shared-ticket';
      }
    }
    return 'catalog';
  });
  const [shareToken, setShareToken] = useState<string | null>(() => {
    if (initialShareToken !== undefined) {
      return initialShareToken;
    }
    if (typeof window !== 'undefined' && window.location?.pathname) {
      const match = window.location.pathname.match(/^\/t\/([^/]+)/);
      if (match && match[1]) {
        return decodeURIComponent(match[1]);
      }
    }
    return null;
  });
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [selectedEventData, setSelectedEventData] = useState<PublicEventResponse | null>(null);
  const [loginNotice, setLoginNotice] = useState<string | null>(null);

  useEffect(() => {
    if (state.status === 'logging-out') {
      setAnonymousView('login');
      setLoginNotice(null);
      setSelectedEventId(null);
      setSelectedEventData(null);
    }
  }, [state.status]);

  const authenticatedUser =
    state.status === 'authenticated' ||
    state.status === 'logging-out' ||
    state.status === 'logout-error'
      ? state.user
      : null;

  const currentView = authenticatedUser ? 'authenticated' : anonymousView;

  const handleSelectCatalog = () => {
    setSelectedEventId(null);
    setSelectedEventData(null);
    setShareToken(null);
    setAnonymousView('catalog');
  };

  const handleSelectLogin = () => {
    setLoginNotice(null);
    setShareToken(null);
    setAnonymousView('login');
  };

  return (
    <div className="edt-app-root">
      <Navbar
        user={authenticatedUser}
        activeView={currentView}
        onNavigateCatalog={handleSelectCatalog}
        onNavigateLogin={handleSelectLogin}
        onLogout={authenticatedUser ? endSession : undefined}
      />

      <main id="main-content">
        <SessionContent
          state={state}
          anonymousView={anonymousView}
          shareToken={shareToken}
          selectedEventId={selectedEventId}
          selectedEventData={selectedEventData}
          loginNotice={loginNotice}
          onSelectCatalog={handleSelectCatalog}
          onSelectDetail={(event) => {
            setSelectedEventData(event);
            setSelectedEventId(event.id);
            setShareToken(null);
            setAnonymousView('detail');
          }}
          onProceedToLogin={(notice) => {
            setLoginNotice(notice);
            setShareToken(null);
            setAnonymousView('login');
          }}
          onSelectLogin={handleSelectLogin}
          onEmailChange={setEmail}
          onLogin={authenticate}
          onLogout={endSession}
          onRetryBootstrap={retryBootstrap}
        />
      </main>

      <Footer
        onNavigateCatalog={handleSelectCatalog}
        onNavigateLogin={handleSelectLogin}
      />
    </div>
  );
}

type SessionContentProps = {
  state: SessionState;
  anonymousView: 'catalog' | 'login' | 'detail' | 'shared-ticket';
  shareToken: string | null;
  selectedEventId: string | null;
  selectedEventData: PublicEventResponse | null;
  loginNotice: string | null;
  onSelectCatalog: () => void;
  onSelectDetail: (event: PublicEventResponse) => void;
  onProceedToLogin: (notice: string) => void;
  onSelectLogin: () => void;
  onEmailChange: (email: string) => void;
  onLogin: (password: string) => Promise<void>;
  onLogout: () => Promise<void>;
  onRetryBootstrap: () => Promise<void>;
};

function SessionContent({
  state,
  anonymousView,
  shareToken,
  selectedEventId,
  selectedEventData,
  loginNotice,
  onSelectCatalog,
  onSelectDetail,
  onProceedToLogin,
  onSelectLogin,
  onEmailChange,
  onLogin,
  onLogout,
  onRetryBootstrap,
}: SessionContentProps) {
  if (
    anonymousView === 'shared-ticket' &&
    shareToken &&
    state.status !== 'loading' &&
    state.status !== 'bootstrap-error'
  ) {
    return (
      <PublicSharedTicket
        shareToken={shareToken}
        onBrowseCatalog={onSelectCatalog}
        onLoginClick={onSelectLogin}
      />
    );
  }

  switch (state.status) {
    case 'loading':
      return (
        <section className="tc-auth-system-state tc-auth-system-state--loading" aria-label="Verificação de sessão">
          <div className="tc-auth-system-state__card">
            <span className="tc-auth-system-state__index" aria-hidden="true">ACESSO / TC</span>
            <span className="tc-auth-system-state__spinner" aria-hidden="true" />
            <p role="status">Verificando sessão…</p>
            <small>Preparando sua experiência no Tá em Cartaz.</small>
          </div>
        </section>
      );
    case 'bootstrap-error':
      return (
        <section
          className="tc-auth-system-state tc-auth-system-state--error"
          aria-labelledby="bootstrap-error-title"
        >
          <div className="tc-auth-system-state__card">
            <span className="tc-auth-system-state__index" aria-hidden="true">CONEXÃO / ERRO</span>
            <span className="tc-auth-system-state__symbol" aria-hidden="true">!</span>
            <h2 id="bootstrap-error-title">Não foi possível verificar sua sessão</h2>
            <p role="alert">{state.message}</p>
            <button type="button" onClick={() => void onRetryBootstrap()}>
              <span>Tentar novamente</span>
              <span aria-hidden="true">→</span>
            </button>
          </div>
        </section>
      );
    case 'anonymous':
      if (anonymousView === 'detail' && selectedEventId) {
        return (
          <PublicEventDetail
            eventId={selectedEventId}
            initialEvent={selectedEventData ?? undefined}
            currentUser={null}
            onBackToCatalog={onSelectCatalog}
            onProceedToLogin={onProceedToLogin}
          />
        );
      }
      if (anonymousView === 'catalog') {
        return (
          <PublicEventCatalog
            onSelectEvent={onSelectDetail}
            onLoginClick={onSelectLogin}
          />
        );
      }
      return (
        <LoginForm
          email={state.email}
          busy={false}
          notice={loginNotice ?? undefined}
          onEmailChange={onEmailChange}
          onLogin={onLogin}
        />
      );
    case 'authenticating':
      return (
        <LoginForm
          email={state.email}
          busy
          notice={loginNotice ?? undefined}
          onEmailChange={onEmailChange}
          onLogin={onLogin}
        />
      );
    case 'authentication-error':
      return (
        <LoginForm
          email={state.email}
          busy={false}
          error={state.message}
          notice={loginNotice ?? undefined}
          onEmailChange={onEmailChange}
          onLogin={onLogin}
        />
      );
    case 'authenticated':
      return <AuthenticatedSession user={state.user} busy={false} onLogout={onLogout} />;
    case 'logging-out':
      return <AuthenticatedSession user={state.user} busy onLogout={onLogout} />;
    case 'logout-error':
      return (
        <AuthenticatedSession
          user={state.user}
          busy={false}
          error={state.message}
          onLogout={onLogout}
        />
      );
  }
}
