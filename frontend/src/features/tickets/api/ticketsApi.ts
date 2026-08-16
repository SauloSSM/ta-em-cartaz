export type TicketStatus = 'VALID' | 'USED';

export type MyTicketResponse = {
  id: string;
  reservationId: string;
  eventId: string;
  sectorId: string;
  ordinal: number;
  status: TicketStatus;
  manualCode: string;
  shareToken: string;
  validationToken: string;
  createdAt: string;
};

export type MyTicketListResponse = {
  tickets: MyTicketResponse[];
};

export type PublicTicketResponse = {
  id: string;
  eventId: string;
  sectorId: string;
  ordinal: number;
  status: TicketStatus;
  manualCode: string;
  shareToken: string;
  validationToken: string;
  createdAt: string;
};

export type TicketErrorCode =
  | 'TICKET_NOT_FOUND'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN';

export type FieldError = {
  field: string;
  message: string;
};

export type TicketApiError = {
  code: TicketErrorCode;
  fieldErrors?: FieldError[];
  message: string;
  timestamp: string;
  traceId: string;
};

export type ClientTicketErrorCode =
  | TicketErrorCode
  | 'TICKET_INVALID_RESPONSE'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_CSRF_INVALID';

export class TicketClientError extends Error {
  readonly code: ClientTicketErrorCode;
  readonly fieldErrors?: FieldError[];
  readonly traceId?: string;
  readonly timestamp?: string;

  constructor(
    code: ClientTicketErrorCode,
    message: string,
    fieldErrors?: FieldError[],
    traceId?: string,
    timestamp?: string,
  ) {
    super(message);
    this.name = 'TicketClientError';
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
    this.timestamp = timestamp;
  }
}

export async function listMyTickets(): Promise<MyTicketListResponse> {
  const payload = await requestJson('/api/v1/my-tickets', {
    method: 'GET',
  });
  if (!isMyTicketListResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function getMyTicket(ticketId: string): Promise<MyTicketResponse> {
  const payload = await requestJson(
    `/api/v1/my-tickets/${encodeURIComponent(ticketId)}`,
    {
      method: 'GET',
    },
  );
  if (!isMyTicketResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function getPublicTicket(shareToken: string): Promise<PublicTicketResponse> {
  const payload = await requestJson(
    `/api/v1/public/tickets/${encodeURIComponent(shareToken)}`,
    {
      method: 'GET',
    },
  );
  if (!isPublicTicketResponse(payload)) {
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

async function toApiError(response: Response): Promise<TicketClientError> {
  try {
    const payload: unknown = await response.json();
    if (isTicketApiError(payload)) {
      return new TicketClientError(
        payload.code,
        payload.message,
        payload.fieldErrors,
        payload.traceId,
        payload.timestamp,
      );
    }
    if (isAuthErrorPayload(payload)) {
      return new TicketClientError(payload.code, payload.message);
    }
  } catch {
    // Fallback para respostas sem JSON
  }

  if (response.status === 401) {
    return new TicketClientError('AUTH_UNAUTHENTICATED', 'Sessão expirada ou não autenticada.');
  }
  if (response.status === 403) {
    return new TicketClientError('AUTH_FORBIDDEN', 'Acesso restrito a clientes.');
  }
  if (response.status === 404) {
    return new TicketClientError('TICKET_NOT_FOUND', 'Ingresso não encontrado.');
  }
  return new TicketClientError('TICKET_INVALID_RESPONSE', 'Erro ao processar ingresso.');
}

function invalidResponse(): TicketClientError {
  return new TicketClientError('TICKET_INVALID_RESPONSE', 'Resposta do servidor de ingressos inválida.');
}

function isMyTicketResponse(value: unknown): value is MyTicketResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'reservationId',
    'eventId',
    'sectorId',
    'ordinal',
    'status',
    'manualCode',
    'shareToken',
    'validationToken',
    'createdAt',
  ])) {
    return false;
  }
  const validStatuses: TicketStatus[] = ['VALID', 'USED'];
  return typeof value.id === 'string'
    && typeof value.reservationId === 'string'
    && typeof value.eventId === 'string'
    && typeof value.sectorId === 'string'
    && typeof value.ordinal === 'number'
    && typeof value.status === 'string'
    && validStatuses.includes(value.status as TicketStatus)
    && typeof value.manualCode === 'string'
    && typeof value.shareToken === 'string'
    && typeof value.validationToken === 'string'
    && typeof value.createdAt === 'string';
}

function isMyTicketListResponse(value: unknown): value is MyTicketListResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, ['tickets'])) {
    return false;
  }
  return Array.isArray(value.tickets) && value.tickets.every(isMyTicketResponse);
}

function isPublicTicketResponse(value: unknown): value is PublicTicketResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'eventId',
    'sectorId',
    'ordinal',
    'status',
    'manualCode',
    'shareToken',
    'validationToken',
    'createdAt',
  ])) {
    return false;
  }
  const validStatuses: TicketStatus[] = ['VALID', 'USED'];
  return typeof value.id === 'string'
    && typeof value.eventId === 'string'
    && typeof value.sectorId === 'string'
    && typeof value.ordinal === 'number'
    && typeof value.status === 'string'
    && validStatuses.includes(value.status as TicketStatus)
    && typeof value.manualCode === 'string'
    && typeof value.shareToken === 'string'
    && typeof value.validationToken === 'string'
    && typeof value.createdAt === 'string';
}

function isTicketApiError(value: unknown): value is TicketApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  const validCodes: TicketErrorCode[] = [
    'TICKET_NOT_FOUND',
    'AUTH_UNAUTHENTICATED',
    'AUTH_FORBIDDEN',
  ];
  return typeof value.code === 'string'
    && validCodes.includes(value.code as TicketErrorCode)
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
