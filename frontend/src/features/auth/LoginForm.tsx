import { useEffect, useRef, useState, type FormEvent } from 'react';
import './LoginForm.css';

type LoginFormProps = {
  email: string;
  busy: boolean;
  error?: string;
  notice?: string;
  onEmailChange: (email: string) => void;
  onLogin: (password: string) => Promise<void>;
};

const crowdArtwork = new URL(
  '../../assets/ta-em-cartaz/decorative/hero-crowd-torn.png',
  import.meta.url,
).href;
const vivaAgoraSticker = new URL(
  '../../assets/ta-em-cartaz/decorative/sticker-viva-agora-green-square.png',
  import.meta.url,
).href;
const tcSealPink = new URL(
  '../../assets/ta-em-cartaz/brand/tc-seal-pink.png',
  import.meta.url,
).href;
const pinkScribble = new URL(
  '../../assets/ta-em-cartaz/decorative/scribble-pink-zigzag.png',
  import.meta.url,
).href;

export function LoginForm({ email, busy, error, notice, onEmailChange, onLogin }: LoginFormProps) {
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const errorRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (error !== undefined) {
      errorRef.current?.focus();
    }
  }, [error]);

  const isSubmittingRef = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || isSubmittingRef.current) return;
    isSubmittingRef.current = true;
    try {
      await onLogin(password);
      setPassword('');
    } finally {
      isSubmittingRef.current = false;
    }
  }

  return (
    <section className="tc-auth-login" aria-labelledby="login-title">
      <div className="tc-auth-login__frame">
        <aside className="tc-auth-login__poster" aria-hidden="true">
          <span className="tc-auth-login__poster-index">ACESSO / 2026</span>
          <div className="tc-auth-login__poster-copy">
            <span>A CULTURA</span>
            <span>MOVE.</span>
            <strong>A GENTE</strong>
            <strong>CONECTA.</strong>
          </div>

          <img className="tc-auth-login__scribble" src={pinkScribble} alt="" />
          <span className="tc-auth-login__sun" />
          <span className="tc-auth-login__black-cut" />
          <span className="tc-auth-login__pink-cut" />
          <img className="tc-auth-login__crowd" src={crowdArtwork} alt="" />
          <img className="tc-auth-login__sticker" src={vivaAgoraSticker} alt="" />
          <img className="tc-auth-login__seal" src={tcSealPink} alt="" />
        </aside>

        <div className="tc-auth-login__content">
          <div className="tc-auth-login__eyebrow">
            <span>CONTA DEMO</span>
            <span>AVALIAÇÃO</span>
          </div>

          <h2 id="login-title" className="tc-visually-hidden">
            Entrar com conta provisionada
          </h2>
          <div className="tc-auth-login__display-title" aria-hidden="true">
            ENTRAR<span>.</span>
          </div>
          <p className="tc-auth-login__intro">Use uma das contas fornecidas para a avaliação.</p>

          {notice && (
            <div
              className="tc-auth-login__notice edt-alert edt-alert--info"
              role="status"
              data-testid="login-notice-banner"
            >
              <span className="tc-auth-login__notice-mark" aria-hidden="true">!</span>
              <p>{notice}</p>
            </div>
          )}

          <form
            className="tc-auth-login__form"
            onSubmit={handleSubmit}
            aria-busy={busy}
            aria-describedby={error === undefined ? undefined : 'login-error'}
          >
            <div className="tc-auth-login__field">
              <label htmlFor="email">E-mail</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                required
                value={email}
                placeholder="voce@demo.elitedevticket.local"
                onChange={(event) => onEmailChange(event.currentTarget.value)}
                disabled={busy}
              />
            </div>

            <div className="tc-auth-login__field">
              <label htmlFor="password">Senha</label>
              <div className="tc-auth-login__password-control">
                <input
                  id="password"
                  name="password"
                  type={passwordVisible ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  placeholder="Sua senha"
                  onChange={(event) => setPassword(event.currentTarget.value)}
                  disabled={busy}
                />
                <button
                  className="tc-auth-login__password-toggle"
                  type="button"
                  aria-pressed={passwordVisible}
                  aria-label={passwordVisible ? 'Ocultar senha' : 'Mostrar senha'}
                  disabled={busy}
                  onClick={() => setPasswordVisible((visible) => !visible)}
                >
                  {passwordVisible ? 'OCULTAR' : 'VER'}
                </button>
              </div>
            </div>

            {error === undefined ? null : (
              <p
                className="tc-auth-login__error"
                id="login-error"
                role="alert"
                tabIndex={-1}
                ref={errorRef}
              >
                <span aria-hidden="true">×</span>
                {error}
              </p>
            )}

            <button className="tc-auth-login__submit" type="submit" disabled={busy}>
              <span>{busy ? 'Entrando…' : 'Entrar'}</span>
              <span aria-hidden="true">→</span>
            </button>
          </form>

          <footer className="tc-auth-login__helper">
            <span>CLIENTE</span>
            <span>ORGANIZADOR</span>
            <span>PORTARIA</span>
            <p>Sem cadastro público nesta versão.</p>
          </footer>
        </div>
      </div>
    </section>
  );
}
