export type CatalogErrorCode = 'CATALOG_UNAVAILABLE' | 'CATALOG_INVALID_REQUEST';

export type CatalogEventReference = {
  externalId: string;
  title: string;
  description?: string;
  imageUrl?: string;
  category?: string;
};

export type CatalogSearchResponse = {
  events: CatalogEventReference[];
};

export type FieldError = {
  field: string;
  message: string;
};

export type CatalogApiError = {
  code: CatalogErrorCode;
  message: string;
  fieldErrors?: FieldError[];
  traceId: string;
  timestamp: string;
};

export type ClientCatalogErrorCode =
  | CatalogErrorCode
  | 'CATALOG_INVALID_RESPONSE'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN';

export class CatalogClientError extends Error {
  readonly code: ClientCatalogErrorCode;

  constructor(code: ClientCatalogErrorCode, message: string) {
    super(message);
    this.name = 'CatalogClientError';
    this.code = code;
  }
}

export async function searchCatalogEvents(keyword?: string): Promise<CatalogSearchResponse> {
  const trimmed = keyword?.trim() ?? '';
  const query = trimmed.length > 0 ? `?keyword=${encodeURIComponent(trimmed)}` : '';
  const payload = await requestJson(`/api/v1/catalog/events${query}`, { method: 'GET' });
  if (!isCatalogSearchResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

async function requestJson(path: string, init: RequestInit): Promise<unknown> {
  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
  });
  if (!response.ok) {
    throw await toApiError(response);
  }
  return response.json() as Promise<unknown>;
}

async function toApiError(response: Response): Promise<CatalogClientError> {
  try {
    const payload: unknown = await response.json();
    if (isCatalogApiError(payload)) {
      return new CatalogClientError(payload.code, payload.message);
    }
    if (isAuthErrorPayload(payload)) {
      return new CatalogClientError(payload.code, payload.message);
    }
  } catch {
    // Resposta sem JSON válido cai no fallback seguro
  }
  if (response.status === 401) {
    return new CatalogClientError('AUTH_UNAUTHENTICATED', 'Sessão expirada ou não autenticada.');
  }
  if (response.status === 403) {
    return new CatalogClientError('AUTH_FORBIDDEN', 'Acesso negado.');
  }
  return new CatalogClientError(
    'CATALOG_UNAVAILABLE',
    'Catálogo Ticketmaster temporariamente indisponível.',
  );
}

function invalidResponse() {
  return new CatalogClientError(
    'CATALOG_INVALID_RESPONSE',
    'Resposta do catálogo inválida.',
  );
}

function isCatalogSearchResponse(value: unknown): value is CatalogSearchResponse {
  return isRecord(value)
    && hasExactKeys(value, ['events'])
    && Array.isArray(value.events)
    && value.events.every(isCatalogEventReference);
}

function isCatalogEventReference(value: unknown): value is CatalogEventReference {
  if (!isRecord(value) || !hasOnlyKeys(value, ['externalId', 'title', 'description', 'imageUrl', 'category'])) {
    return false;
  }
  return typeof value.externalId === 'string'
    && typeof value.title === 'string'
    && (value.description === undefined || typeof value.description === 'string')
    && (value.imageUrl === undefined || typeof value.imageUrl === 'string')
    && (value.category === undefined || typeof value.category === 'string');
}

function isCatalogApiError(value: unknown): value is CatalogApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  return (value.code === 'CATALOG_UNAVAILABLE' || value.code === 'CATALOG_INVALID_REQUEST')
    && typeof value.message === 'string'
    && typeof value.traceId === 'string'
    && typeof value.timestamp === 'string'
    && (value.fieldErrors === undefined
      || (Array.isArray(value.fieldErrors) && value.fieldErrors.every(isFieldError)));
}

function isAuthErrorPayload(value: unknown): value is { code: 'AUTH_UNAUTHENTICATED' | 'AUTH_FORBIDDEN'; message: string } {
  return isRecord(value)
    && (value.code === 'AUTH_UNAUTHENTICATED' || value.code === 'AUTH_FORBIDDEN')
    && typeof value.message === 'string';
}

function isFieldError(value: unknown): value is FieldError {
  return isRecord(value)
    && hasExactKeys(value, ['field', 'message'])
    && typeof value.field === 'string'
    && typeof value.message === 'string';
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
