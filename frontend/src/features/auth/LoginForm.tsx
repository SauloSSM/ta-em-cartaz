import { useEffect, useRef, useState, type FormEvent } from 'react';

type LoginFormProps = {
  email: string;
  busy: boolean;
  error?: string;
  onEmailChange: (email: string) => void;
  onLogin: (password: string) => Promise<void>;
};

export function LoginForm({ email, busy, error, onEmailChange, onLogin }: LoginFormProps) {
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const errorRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (error !== undefined) {
      errorRef.current?.focus();
    }
  }, [error]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onLogin(password);
    setPassword('');
  }

  return (
    <section aria-labelledby="login-title">
      <h2 id="login-title">Entrar com conta provisionada</h2>
      <p>Use uma das contas fornecidas para a avaliação.</p>
      <form
        onSubmit={handleSubmit}
        aria-busy={busy}
        aria-describedby={error === undefined ? undefined : 'login-error'}
      >
        <div>
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => onEmailChange(event.currentTarget.value)}
            disabled={busy}
          />
        </div>
        <div>
          <label htmlFor="password">Senha</label>
          <input
            id="password"
            name="password"
            type={passwordVisible ? 'text' : 'password'}
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.currentTarget.value)}
            disabled={busy}
          />
          <button
            type="button"
            aria-pressed={passwordVisible}
            aria-label={passwordVisible ? 'Ocultar senha' : 'Mostrar senha'}
            disabled={busy}
            onClick={() => setPasswordVisible((visible) => !visible)}
          >
            {passwordVisible ? 'Ocultar senha' : 'Mostrar senha'}
          </button>
        </div>
        {error === undefined ? null : (
          <p id="login-error" role="alert" tabIndex={-1} ref={errorRef}>
            {error}
          </p>
        )}
        <button type="submit" disabled={busy}>
          {busy ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </section>
  );
}
