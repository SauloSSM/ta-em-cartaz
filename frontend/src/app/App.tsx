import { useState, useEffect } from 'react';
import { AuthenticatedSession, LoginForm } from '../features/auth';
import { PublicEventCatalog } from '../features/events';
import { useSession, type SessionState } from './session/useSession';

export type AppProps = {
  initialAnonymousView?: 'catalog' | 'login';
};

export function App({ initialAnonymousView = 'catalog' }: AppProps) {
  const { state, setEmail, authenticate, endSession, retryBootstrap } = useSession();
  const [anonymousView, setAnonymousView] = useState<'catalog' | 'login'>(initialAnonymousView);

  useEffect(() => {
    if (state.status === 'logging-out') {
      setAnonymousView('login');
    }
  }, [state.status]);

  const isAnonymousStatus =
    state.status === 'anonymous' ||
    state.status === 'authenticating' ||
    state.status === 'authentication-error';

  return (
    <div className="edt-app-root">
      <header className="edt-top-bar">
        <h1>EliteDevTicket</h1>
        {isAnonymousStatus && (
          <nav aria-label="Navegação principal" className="edt-top-nav">
            <button
              type="button"
              className={`edt-nav-link ${anonymousView === 'catalog' ? 'edt-nav-link--active' : ''}`}
              onClick={() => setAnonymousView('catalog')}
              aria-current={anonymousView === 'catalog' ? 'page' : undefined}
            >
              Catálogo de Eventos
            </button>
            <button
              type="button"
              className={`edt-nav-link ${anonymousView === 'login' ? 'edt-nav-link--active' : ''}`}
              onClick={() => setAnonymousView('login')}
              aria-current={anonymousView === 'login' ? 'page' : undefined}
            >
              Acessar conta
            </button>
          </nav>
        )}
      </header>

      <main id="main-content">
        <SessionContent
          state={state}
          anonymousView={anonymousView}
          onSelectView={setAnonymousView}
          onEmailChange={setEmail}
          onLogin={authenticate}
          onLogout={endSession}
          onRetryBootstrap={retryBootstrap}
        />
      </main>
    </div>
  );
}

type SessionContentProps = {
  state: SessionState;
  anonymousView: 'catalog' | 'login';
  onSelectView: (view: 'catalog' | 'login') => void;
  onEmailChange: (email: string) => void;
  onLogin: (password: string) => Promise<void>;
  onLogout: () => Promise<void>;
  onRetryBootstrap: () => Promise<void>;
};

function SessionContent({
  state,
  anonymousView,
  onSelectView,
  onEmailChange,
  onLogin,
  onLogout,
  onRetryBootstrap,
}: SessionContentProps) {
  switch (state.status) {
    case 'loading':
      return <p role="status">Verificando sessão…</p>;
    case 'bootstrap-error':
      return (
        <section aria-labelledby="bootstrap-error-title">
          <h2 id="bootstrap-error-title">Não foi possível verificar sua sessão</h2>
          <p role="alert">{state.message}</p>
          <button type="button" onClick={() => void onRetryBootstrap()}>
            Tentar novamente
          </button>
        </section>
      );
    case 'anonymous':
      if (anonymousView === 'catalog') {
        return <PublicEventCatalog onLoginClick={() => onSelectView('login')} />;
      }
      return <LoginForm email={state.email} busy={false} onEmailChange={onEmailChange} onLogin={onLogin} />;
    case 'authenticating':
      return <LoginForm email={state.email} busy onEmailChange={onEmailChange} onLogin={onLogin} />;
    case 'authentication-error':
      return (
        <LoginForm
          email={state.email}
          busy={false}
          error={state.message}
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
