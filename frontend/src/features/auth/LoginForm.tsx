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

type DemoAccountId = 'customer-one' | 'customer-two' | 'organizer' | 'gate';
type DemoAccountTone = 'green' | 'blue' | 'yellow' | 'pink';

type DemoAccount = {
  id: DemoAccountId;
  label: string;
  email: string;
  password: string;
  tone: DemoAccountTone;
};

const DEMO_ACCOUNTS: DemoAccount[] = [
  {
    id: 'customer-one',
    label: 'Cliente 1',
    email: 'customer.one@demo.elitedevticket.local',
    password: 'password',
    tone: 'green',
  },
  {
    id: 'customer-two',
    label: 'Cliente 2',
    email: 'customer.two@demo.elitedevticket.local',
    password: 'password',
    tone: 'blue',
  },
  {
    id: 'organizer',
    label: 'Organizador',
    email: 'organizer@demo.elitedevticket.local',
    password: 'password',
    tone: 'yellow',
  },
  {
    id: 'gate',
    label: 'Portaria',
    email: 'gate@demo.elitedevticket.local',
    password: 'password',
    tone: 'pink',
  },
];

const posterCollage = new URL(
  '../../assets/ta-em-cartaz/auth/login-poster-collage.webp',
  import.meta.url,
).href;
const pinkStarburst = new URL(
  '../../assets/ta-em-cartaz/auth/login-pink-starburst.png',
  import.meta.url,
).href;

export function LoginForm({ email, busy, error, notice, onEmailChange, onLogin }: LoginFormProps) {
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [selectedDemoAccount, setSelectedDemoAccount] = useState<DemoAccountId | null>(null);
  const [demoStatus, setDemoStatus] = useState<string | null>(null);
  const errorRef = useRef<HTMLParagraphElement>(null);
  const isSubmittingRef = useRef(false);

  useEffect(() => {
    if (error !== undefined) {
      errorRef.current?.focus();
    }
  }, [error]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || isSubmittingRef.current) return;
    isSubmittingRef.current = true;
    try {
      await onLogin(password);
      setPassword('');
      setSelectedDemoAccount(null);
      setDemoStatus(null);
    } finally {
      isSubmittingRef.current = false;
    }
  }

  function handleDemoAccountSelect(account: DemoAccount) {
    if (busy) return;
    onEmailChange(account.email);
    setPassword(account.password);
    setPasswordVisible(false);
    setSelectedDemoAccount(account.id);
    setDemoStatus(`${account.label}: credenciais preenchidas automaticamente.`);
  }

  function handleEmailChange(nextEmail: string) {
    onEmailChange(nextEmail);
    const selectedAccount = DEMO_ACCOUNTS.find((account) => account.id === selectedDemoAccount);
    if (selectedAccount?.email !== nextEmail) {
      setSelectedDemoAccount(null);
      setDemoStatus(null);
    }
  }

  function handlePasswordChange(nextPassword: string) {
    setPassword(nextPassword);
    const selectedAccount = DEMO_ACCOUNTS.find((account) => account.id === selectedDemoAccount);
    if (selectedAccount?.password !== nextPassword) {
      setSelectedDemoAccount(null);
      setDemoStatus(null);
    }
  }

  return (
    <section className="tc-auth-login" aria-labelledby="login-title">
      <div className="tc-auth-login__frame">
        <aside className="tc-auth-login__poster" aria-hidden="true">
          <img className="tc-auth-login__poster-art" src={posterCollage} alt="" />
          <span className="tc-auth-login__poster-index">ACESSO / 2026</span>
        </aside>

        <div className="tc-auth-login__content">
          <div className="tc-auth-login__eyebrow" aria-hidden="true">
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
              <div className="tc-auth-login__input-shell">
                <span className="tc-auth-login__input-icon tc-auth-login__input-icon--user" aria-hidden="true" />
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  required
                  value={email}
                  placeholder="voce@demo.elitedevticket.local"
                  onChange={(event) => handleEmailChange(event.currentTarget.value)}
                  disabled={busy}
                />
                {selectedDemoAccount !== null ? (
                  <span className="tc-auth-login__input-check" aria-hidden="true">✓</span>
                ) : null}
              </div>
            </div>

            <div className="tc-auth-login__field">
              <label htmlFor="password">Senha</label>
              <div className="tc-auth-login__password-control">
                <div className="tc-auth-login__input-shell tc-auth-login__input-shell--password">
                  <span className="tc-auth-login__input-icon tc-auth-login__input-icon--lock" aria-hidden="true" />
                  <input
                    id="password"
                    name="password"
                    type={passwordVisible ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    value={password}
                    placeholder="Sua senha"
                    onChange={(event) => handlePasswordChange(event.currentTarget.value)}
                    disabled={busy}
                  />
                </div>
                <button
                  className="tc-auth-login__password-toggle"
                  type="button"
                  aria-pressed={passwordVisible}
                  aria-label={passwordVisible ? 'Ocultar senha' : 'Mostrar senha'}
                  disabled={busy}
                  onClick={() => setPasswordVisible((visible) => !visible)}
                >
                  <span className="tc-auth-login__eye" aria-hidden="true">◉</span>
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

          <section className="tc-auth-login__demo" aria-labelledby="demo-accounts-title">
            <div className="tc-auth-login__demo-heading">
              <div>
                <h3 id="demo-accounts-title">CONTAS DE AVALIAÇÃO <span>(PREENCHIMENTO AUTOMÁTICO)</span></h3>
                <p>Clique em uma opção para preencher os dados.</p>
              </div>
              <span className="tc-auth-login__demo-index" aria-hidden="true">04 / DEMO</span>
            </div>

            <div className="tc-auth-login__demo-grid">
              {DEMO_ACCOUNTS.map((account) => (
                <button
                  key={account.id}
                  className={`tc-auth-login__demo-account tc-auth-login__demo-account--${account.tone}`}
                  type="button"
                  aria-pressed={selectedDemoAccount === account.id}
                  disabled={busy}
                  onClick={() => handleDemoAccountSelect(account)}
                >
                  <span className="tc-auth-login__demo-icon" aria-hidden="true" />
                  <span>{account.label}</span>
                </button>
              ))}
            </div>
          </section>

          <div
            className={`tc-auth-login__demo-status${demoStatus ? ' tc-auth-login__demo-status--visible' : ''}`}
            role="status"
            aria-live="polite"
          >
            <span className="tc-auth-login__demo-status-mark" aria-hidden="true">✓</span>
            <p>
              <strong>{demoStatus ?? 'Credenciais de demonstração.'}</strong>
              <span>{demoStatus ? 'Você pode ajustar se quiser. As credenciais são apenas para teste.' : 'Escolha uma das quatro contas acima para preencher e entrar.'}</span>
            </p>
          </div>

          <p className="tc-auth-login__no-signup">Sem cadastro público nesta versão.</p>
          <img className="tc-auth-login__starburst" src={pinkStarburst} alt="" aria-hidden="true" />
        </div>
      </div>
    </section>
  );
}
