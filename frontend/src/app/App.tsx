import { AuthenticatedSession, LoginForm } from '../features/auth';
import { useSession, type SessionState } from './session/useSession';

export function App() {
  const { state, setEmail, authenticate, endSession } = useSession();

  return (
    <main id="main-content">
      <h1>EliteDevTicket</h1>
      <SessionContent
        state={state}
        onEmailChange={setEmail}
        onLogin={authenticate}
        onLogout={endSession}
      />
    </main>
  );
}

type SessionContentProps = {
  state: SessionState;
  onEmailChange: (email: string) => void;
  onLogin: (password: string) => Promise<void>;
  onLogout: () => Promise<void>;
};

function SessionContent({ state, onEmailChange, onLogin, onLogout }: SessionContentProps) {
  switch (state.status) {
    case 'loading':
      return <p role="status">Verificando sessão…</p>;
    case 'anonymous':
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
