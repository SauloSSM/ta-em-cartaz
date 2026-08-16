import { useEffect, useReducer } from 'react';
import {
  AuthApiError,
  getSession,
  login as loginRequest,
  logout as logoutRequest,
  type SessionUser,
} from '../api/authApi';

export type { SessionUser };
import { clearPurchaseIntention } from '../../features/events/model/purchaseIntention';

const PURCHASE_INTENT_KEY = 'edt.purchase-intent.v1';

export type SessionState =
  | { status: 'loading' }
  | { status: 'bootstrap-error'; message: string }
  | { status: 'anonymous'; email: string }
  | { status: 'authenticating'; email: string }
  | { status: 'authentication-error'; email: string; message: string }
  | { status: 'authenticated'; user: SessionUser }
  | { status: 'logging-out'; user: SessionUser }
  | { status: 'logout-error'; user: SessionUser; message: string };

type SessionAction =
  | { type: 'BOOTSTRAP_STARTED' }
  | { type: 'BOOTSTRAP_ANONYMOUS' }
  | { type: 'BOOTSTRAP_AUTHENTICATED'; user: SessionUser }
  | { type: 'BOOTSTRAP_FAILED'; message: string }
  | { type: 'EDIT_EMAIL'; email: string }
  | { type: 'LOGIN_STARTED' }
  | { type: 'LOGIN_SUCCEEDED'; user: SessionUser }
  | { type: 'LOGIN_FAILED'; message: string }
  | { type: 'LOGOUT_STARTED' }
  | { type: 'LOGOUT_SUCCEEDED' }
  | { type: 'LOGOUT_FAILED'; message: string };

export function useSession() {
  const [state, dispatch] = useReducer(reduceSession, { status: 'loading' });

  useEffect(() => {
    let active = true;
    void getSession()
      .then((session) => {
        if (!active) return;
        dispatch(session.authenticated
          ? { type: 'BOOTSTRAP_AUTHENTICATED', user: session.user }
          : { type: 'BOOTSTRAP_ANONYMOUS' });
      })
      .catch(() => {
        if (active) dispatch({
          type: 'BOOTSTRAP_FAILED',
          message: 'Não foi possível verificar sua sessão. Tente novamente.',
        });
      });
    return () => {
      active = false;
    };
  }, []);

  async function retryBootstrap() {
    dispatch({ type: 'BOOTSTRAP_STARTED' });
    try {
      const session = await getSession();
      dispatch(session.authenticated
        ? { type: 'BOOTSTRAP_AUTHENTICATED', user: session.user }
        : { type: 'BOOTSTRAP_ANONYMOUS' });
    } catch {
      dispatch({
        type: 'BOOTSTRAP_FAILED',
        message: 'Não foi possível verificar sua sessão. Tente novamente.',
      });
    }
  }

  async function authenticate(password: string) {
    if (!hasEmail(state)) return;
    dispatch({ type: 'LOGIN_STARTED' });
    try {
      const session = await loginRequest(state.email, password);
      dispatch({ type: 'LOGIN_SUCCEEDED', user: session.user });
    } catch (error) {
      dispatch({ type: 'LOGIN_FAILED', message: loginMessage(error) });
    }
  }

  async function endSession() {
    if (!hasUser(state)) return;
    dispatch({ type: 'LOGOUT_STARTED' });
    try {
      await logoutRequest();
    } catch {
      dispatch({ type: 'LOGOUT_FAILED', message: 'Não foi possível encerrar a sessão. Tente novamente.' });
      return;
    }
    clearPurchaseIntention();
    try {
      sessionStorage.removeItem(PURCHASE_INTENT_KEY);
    } catch {
      // O servidor já encerrou a sessão; indisponibilidade do storage não restaura autenticação.
    }
    dispatch({ type: 'LOGOUT_SUCCEEDED' });
  }

  return {
    state,
    setEmail: (email: string) => dispatch({ type: 'EDIT_EMAIL', email }),
    authenticate,
    endSession,
    retryBootstrap,
  };
}

export function reduceSession(state: SessionState, action: SessionAction): SessionState {
  switch (action.type) {
    case 'BOOTSTRAP_STARTED':
      return { status: 'loading' };
    case 'BOOTSTRAP_ANONYMOUS':
      return { status: 'anonymous', email: '' };
    case 'BOOTSTRAP_AUTHENTICATED':
    case 'LOGIN_SUCCEEDED':
      return { status: 'authenticated', user: action.user };
    case 'BOOTSTRAP_FAILED':
      return { status: 'bootstrap-error', message: action.message };
    case 'EDIT_EMAIL':
      return hasEmail(state) ? { status: 'anonymous', email: action.email } : state;
    case 'LOGIN_STARTED':
      return hasEmail(state) ? { status: 'authenticating', email: state.email } : state;
    case 'LOGIN_FAILED':
      return hasEmail(state)
        ? { status: 'authentication-error', email: state.email, message: action.message }
        : state;
    case 'LOGOUT_STARTED':
      return hasUser(state) ? { status: 'logging-out', user: state.user } : state;
    case 'LOGOUT_SUCCEEDED':
      return { status: 'anonymous', email: '' };
    case 'LOGOUT_FAILED':
      return hasUser(state) ? { status: 'logout-error', user: state.user, message: action.message } : state;
  }
}

function hasEmail(state: SessionState): state is Extract<SessionState, { email: string }> {
  return 'email' in state;
}

function hasUser(state: SessionState): state is Extract<SessionState, { user: SessionUser }> {
  return 'user' in state;
}

function loginMessage(error: unknown): string {
  if (error instanceof AuthApiError && error.code === 'AUTH_INVALID_CREDENTIALS') {
    return 'E-mail ou senha inválidos.';
  }
  return 'Não foi possível entrar. Tente novamente.';
}
