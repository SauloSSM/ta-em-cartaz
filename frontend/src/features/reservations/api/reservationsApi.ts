export type ReservationStatus = 'HOLDING' | 'CONFIRMED' | 'EXPIRED';

export type CreateReservationRequest = {
  quantity: number;
};

export type ReservationResponse = {
  id: string;
  customerId: string;
  eventId: string;
  sectorId: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: ReservationStatus;
  expiresAt: string;
  createdAt: string;
  serverNow: string;
};

export type ReservationErrorCode =
  | 'INSUFFICIENT_AVAILABILITY'
  | 'SALES_CLOSED'
  | 'EVENT_NOT_PUBLISHED'
  | 'EVENT_NOT_FOUND'
  | 'SECTOR_NOT_FOUND'
  | 'RESERVATION_NOT_FOUND'
  | 'RESERVATION_EXPIRED'
  | 'AUTH_INVALID_REQUEST'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_UNAUTHENTICATED';

export type FieldError = {
  field: string;
  message: string;
};

export type ReservationApiError = {
  code: ReservationErrorCode;
  message: string;
  fieldErrors?: FieldError[];
  traceId: string;
  timestamp: string;
};

export type ClientReservationErrorCode =
  | ReservationErrorCode
  | 'RESERVATION_INVALID_RESPONSE'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_CSRF_INVALID';

export class ReservationClientError extends Error {
  readonly code: ClientReservationErrorCode;
  readonly fieldErrors?: FieldError[];
  readonly traceId?: string;
  readonly timestamp?: string;

  constructor(
    code: ClientReservationErrorCode,
    message: string,
    fieldErrors?: FieldError[],
    traceId?: string,
    timestamp?: string,
  ) {
    super(message);
    this.name = 'ReservationClientError';
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
    this.timestamp = timestamp;
  }
}

export async function createReservation(
  eventId: string,
  sectorId: string,
  data: CreateReservationRequest,
): Promise<ReservationResponse> {
  const request: CreateReservationRequest = {
    quantity: data.quantity,
  };
  const payload = await requestJson(
    `/api/v1/events/${encodeURIComponent(eventId)}/sectors/${encodeURIComponent(sectorId)}/reservations`,
    {
      method: 'POST',
      headers: csrfHeaders(),
      body: JSON.stringify(request),
    },
  );
  if (!isReservationResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

async function requestJson(url: string, init?: RequestInit): Promise<unknown> {
  const headers = new Headers(init?.headers);
  if (!headers.has('Content-Type') && init?.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...init,
    headers,
    credentials: 'same-origin',
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

async function toApiError(response: Response): Promise<ReservationClientError> {
  try {
    const payload: unknown = await response.json();
    if (isReservationApiError(payload)) {
      return new ReservationClientError(
        payload.code,
        payload.message,
        payload.fieldErrors,
        payload.traceId,
        payload.timestamp,
      );
    }
    if (isAuthErrorPayload(payload)) {
      return new ReservationClientError(payload.code, payload.message);
    }
  } catch {
    // Fallback para respostas sem JSON
  }

  if (response.status === 401) {
    return new ReservationClientError('AUTH_UNAUTHENTICATED', 'Sessão expirada ou não autenticada.');
  }
  if (response.status === 403) {
    return new ReservationClientError('AUTH_FORBIDDEN', 'Acesso restrito a clientes.');
  }
  if (response.status === 404) {
    return new ReservationClientError('EVENT_NOT_FOUND', 'Evento ou setor não encontrado.');
  }
  if (response.status === 422) {
    return new ReservationClientError('INSUFFICIENT_AVAILABILITY', 'Não foi possível reservar os ingressos solicitados.');
  }
  return new ReservationClientError('AUTH_INVALID_REQUEST', 'Erro ao processar reserva.');
}

function invalidResponse(): ReservationClientError {
  return new ReservationClientError('RESERVATION_INVALID_RESPONSE', 'Resposta do servidor de reservas inválida.');
}

function isReservationResponse(value: unknown): value is ReservationResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'customerId',
    'eventId',
    'sectorId',
    'quantity',
    'unitPrice',
    'totalAmount',
    'status',
    'expiresAt',
    'createdAt',
    'serverNow',
  ])) {
    return false;
  }
  const validStatuses: ReservationStatus[] = ['HOLDING', 'CONFIRMED', 'EXPIRED'];
  return typeof value.id === 'string'
    && typeof value.customerId === 'string'
    && typeof value.eventId === 'string'
    && typeof value.sectorId === 'string'
    && typeof value.quantity === 'number'
    && typeof value.unitPrice === 'number'
    && typeof value.totalAmount === 'number'
    && typeof value.status === 'string'
    && validStatuses.includes(value.status as ReservationStatus)
    && typeof value.expiresAt === 'string'
    && typeof value.createdAt === 'string'
    && typeof value.serverNow === 'string';
}

function isReservationApiError(value: unknown): value is ReservationApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  const validCodes: ReservationErrorCode[] = [
    'INSUFFICIENT_AVAILABILITY',
    'SALES_CLOSED',
    'EVENT_NOT_PUBLISHED',
    'EVENT_NOT_FOUND',
    'SECTOR_NOT_FOUND',
    'RESERVATION_NOT_FOUND',
    'RESERVATION_EXPIRED',
    'AUTH_INVALID_REQUEST',
    'AUTH_FORBIDDEN',
    'AUTH_UNAUTHENTICATED',
  ];
  return typeof value.code === 'string'
    && validCodes.includes(value.code as ReservationErrorCode)
    && typeof value.message === 'string'
    && typeof value.traceId === 'string'
    && typeof value.timestamp === 'string'
    && (value.fieldErrors === undefined
      || (Array.isArray(value.fieldErrors) && value.fieldErrors.every(isFieldError)));
}

function isAuthErrorPayload(value: unknown): value is { code: 'AUTH_UNAUTHENTICATED' | 'AUTH_FORBIDDEN' | 'AUTH_CSRF_INVALID'; message: string } {
  return isRecord(value)
    && (value.code === 'AUTH_UNAUTHENTICATED' || value.code === 'AUTH_FORBIDDEN' || value.code === 'AUTH_CSRF_INVALID')
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
