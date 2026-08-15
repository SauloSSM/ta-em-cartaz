export type Role = 'ORGANIZER' | 'CUSTOMER' | 'GATE';

export type AuthErrorCode =
  | 'AUTH_INVALID_REQUEST'
  | 'AUTH_INVALID_CREDENTIALS'
  | 'AUTH_CSRF_INVALID'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN';

export type LoginRequest = {
  email: string;
  password: string;
};

export type SessionUser = {
  id: string;
  email: string;
  role: Role;
};

export type AnonymousSessionResponse = { authenticated: false };

export type AuthenticatedSessionResponse = {
  authenticated: true;
  user: SessionUser;
};

export type SessionResponse = AnonymousSessionResponse | AuthenticatedSessionResponse;

export type FieldError = {
  field: string;
  message: string;
};

export type ApiError = {
  code: AuthErrorCode;
  message: string;
  fieldErrors?: FieldError[];
  traceId: string;
  timestamp: string;
};

type ClientAuthErrorCode = AuthErrorCode | 'AUTH_INVALID_RESPONSE' | 'AUTH_UNAVAILABLE';

export class AuthApiError extends Error {
  readonly code: ClientAuthErrorCode;

  constructor(code: ClientAuthErrorCode, message: string) {
    super(message);
    this.name = 'AuthApiError';
    this.code = code;
  }
}

export async function getSession(): Promise<SessionResponse> {
  const payload = await requestJson('/api/v1/auth/session', { method: 'GET' });
  if (!isSessionResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function login(email: string, password: string): Promise<AuthenticatedSessionResponse> {
  const request: LoginRequest = { email, password };
  const payload = await requestJson('/api/v1/auth/login', {
    method: 'POST',
    headers: csrfHeaders(),
    body: JSON.stringify(request),
  });
  if (!isAuthenticatedSession(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers: csrfHeaders(),
  });
  if (response.status !== 204) {
    throw await toApiError(response);
  }
}

async function requestJson(path: string, init: RequestInit): Promise<unknown> {
  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...(init.body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...init.headers,
    },
  });
  if (!response.ok) {
    throw await toApiError(response);
  }
  return response.json() as Promise<unknown>;
}

function csrfHeaders(): HeadersInit {
  const token = readCookie('XSRF-TOKEN');
  return token === null ? {} : { 'X-XSRF-TOKEN': token };
}

function readCookie(name: string): string | null {
  const prefix = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));
  return cookie === undefined ? null : decodeURIComponent(cookie.slice(prefix.length));
}

async function toApiError(response: Response): Promise<AuthApiError> {
  try {
    const payload: unknown = await response.json();
    if (isApiError(payload)) {
      return new AuthApiError(payload.code, payload.message);
    }
  } catch {
    // Uma resposta sem JSON válido é reduzida a uma mensagem segura.
  }
  return new AuthApiError('AUTH_UNAVAILABLE', 'Não foi possível concluir a autenticação.');
}

function invalidResponse() {
  return new AuthApiError('AUTH_INVALID_RESPONSE', 'Resposta de autenticação inválida.');
}

function isApiError(value: unknown): value is ApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  return isAuthErrorCode(value.code)
    && typeof value.message === 'string'
    && typeof value.traceId === 'string'
    && typeof value.timestamp === 'string'
    && (value.fieldErrors === undefined
      || (Array.isArray(value.fieldErrors) && value.fieldErrors.every(isFieldError)));
}

function isFieldError(value: unknown): value is FieldError {
  return isRecord(value)
    && hasExactKeys(value, ['field', 'message'])
    && typeof value.field === 'string'
    && typeof value.message === 'string';
}

function isSessionResponse(value: unknown): value is SessionResponse {
  return isAnonymousSession(value) || isAuthenticatedSession(value);
}

function isAnonymousSession(value: unknown): value is AnonymousSessionResponse {
  return isRecord(value)
    && hasExactKeys(value, ['authenticated'])
    && value.authenticated === false;
}

function isAuthenticatedSession(value: unknown): value is AuthenticatedSessionResponse {
  return isRecord(value)
    && hasExactKeys(value, ['authenticated', 'user'])
    && value.authenticated === true
    && isSessionUser(value.user);
}

function isSessionUser(value: unknown): value is SessionUser {
  return isRecord(value)
    && hasExactKeys(value, ['id', 'email', 'role'])
    && typeof value.id === 'string'
    && typeof value.email === 'string'
    && (value.role === 'ORGANIZER' || value.role === 'CUSTOMER' || value.role === 'GATE');
}

function isAuthErrorCode(value: unknown): value is AuthErrorCode {
  return value === 'AUTH_INVALID_REQUEST'
    || value === 'AUTH_INVALID_CREDENTIALS'
    || value === 'AUTH_CSRF_INVALID'
    || value === 'AUTH_UNAUTHENTICATED'
    || value === 'AUTH_FORBIDDEN';
}

function hasExactKeys(value: Record<string, unknown>, expected: string[]): boolean {
  return Object.keys(value).length === expected.length && expected.every((key) => key in value);
}

function hasOnlyKeys(value: Record<string, unknown>, allowed: string[]): boolean {
  return Object.keys(value).every((key) => allowed.includes(key));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
