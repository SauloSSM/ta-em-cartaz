export type EventStatus = 'DRAFT' | 'PUBLISHED';

export type CreateDraftEventRequest = {
  externalSource?: string;
  externalId?: string;
  title: string;
  description?: string;
  imageUrl?: string;
  category?: string;
};

export type UpdateDraftEventRequest = {
  title: string;
  description?: string;
  imageUrl?: string;
  category?: string;
  venueName?: string;
  venueAddress?: string;
  startsAt?: string;
};

export type EventResponse = {
  id: string;
  organizerId: string;
  externalSource?: string;
  externalId?: string;
  title: string;
  description?: string;
  imageUrl?: string;
  category?: string;
  status: EventStatus;
  venueName?: string;
  venueAddress?: string;
  startsAt?: string;
  createdAt: string;
  updatedAt: string;
};

export type EventListResponse = {
  events: EventResponse[];
};

export type PublicEventResponse = {
  id: string;
  title: string;
  description?: string;
  imageUrl?: string;
  category?: string;
  status: EventStatus;
  venueName?: string;
  venueAddress?: string;
  startsAt?: string;
  startingPrice: number;
  salesClosed: boolean;
  createdAt: string;
  updatedAt: string;
};

export type PublicEventListResponse = {
  events: PublicEventResponse[];
};

export type TicketSectorResponse = {
  id: string;
  eventId: string;
  name: string;
  description?: string;
  capacity: number;
  availableQuantity: number;
  price: number;
  createdAt: string;
  updatedAt: string;
};

export type TicketSectorListResponse = {
  sectors: TicketSectorResponse[];
};

export type CreateTicketSectorRequest = {
  name: string;
  description?: string;
  capacity: number;
  price: number;
};

export type UpdateTicketSectorRequest = {
  name: string;
  description?: string;
  capacity: number;
  price: number;
};

export type EventErrorCode =
  | 'EVENT_NOT_FOUND'
  | 'EVENT_FORBIDDEN'
  | 'EVENT_INVALID_REQUEST'
  | 'EVENT_CANNOT_BE_DELETED'
  | 'EVENT_CANNOT_BE_MODIFIED';

export type EventApiError = {
  code: EventErrorCode;
  message: string;
  fieldErrors?: FieldError[];
  traceId: string;
  timestamp: string;
};

export type FieldError = {
  field: string;
  message: string;
};

export type ClientEventErrorCode =
  | EventErrorCode
  | 'EVENT_INVALID_RESPONSE'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_CSRF_INVALID';

export class EventClientError extends Error {
  readonly code: ClientEventErrorCode;

  constructor(code: ClientEventErrorCode, message: string) {
    super(message);
    this.name = 'EventClientError';
    this.code = code;
  }
}

export async function createDraftEvent(
  title: string,
  externalId?: string,
  description?: string,
  imageUrl?: string,
  category?: string,
  externalSource?: string,
): Promise<EventResponse> {
  const request: CreateDraftEventRequest = {
    title,
    ...(externalId === undefined ? {} : { externalId }),
    ...(externalSource === undefined ? (externalId ? { externalSource: 'TICKETMASTER' } : {}) : { externalSource }),
    ...(description === undefined ? {} : { description }),
    ...(imageUrl === undefined ? {} : { imageUrl }),
    ...(category === undefined ? {} : { category }),
  };
  const payload = await requestJson('/api/v1/events/drafts', {
    method: 'POST',
    headers: csrfHeaders(),
    body: JSON.stringify(request),
  });
  if (!isEventResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function listMyEvents(): Promise<EventListResponse> {
  const payload = await requestJson('/api/v1/events/mine', {
    method: 'GET',
  });
  if (!isEventListResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function getEvent(id: string): Promise<EventResponse> {
  const payload = await requestJson(`/api/v1/events/${encodeURIComponent(id)}`, {
    method: 'GET',
  });
  if (!isEventResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function updateDraftEvent(
  id: string,
  data: UpdateDraftEventRequest,
): Promise<EventResponse> {
  const request: UpdateDraftEventRequest = {
    title: data.title,
    ...(data.description === undefined ? {} : { description: data.description }),
    ...(data.imageUrl === undefined ? {} : { imageUrl: data.imageUrl }),
    ...(data.category === undefined ? {} : { category: data.category }),
    ...(data.venueName === undefined ? {} : { venueName: data.venueName }),
    ...(data.venueAddress === undefined ? {} : { venueAddress: data.venueAddress }),
    ...(data.startsAt === undefined ? {} : { startsAt: data.startsAt }),
  };
  const payload = await requestJson(`/api/v1/events/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: csrfHeaders(),
    body: JSON.stringify(request),
  });
  if (!isEventResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function deleteDraftEvent(id: string): Promise<void> {
  const response = await fetch(`/api/v1/events/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...csrfHeaders(),
    },
  });
  if (response.status !== 204) {
    throw await toApiError(response);
  }
}

export async function listPublicEvents(search?: string): Promise<PublicEventListResponse> {
  const query = search && search.trim() ? `?search=${encodeURIComponent(search.trim())}` : '';
  const payload = await requestJson(`/api/v1/events${query}`, {
    method: 'GET',
  });
  if (!isPublicEventListResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function publishEvent(id: string): Promise<EventResponse> {
  const payload = await requestJson(`/api/v1/events/${encodeURIComponent(id)}/publish`, {
    method: 'POST',
    headers: csrfHeaders(),
  });
  if (!isEventResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function listTicketSectors(eventId: string): Promise<TicketSectorListResponse> {
  const payload = await requestJson(`/api/v1/events/${encodeURIComponent(eventId)}/sectors`, {
    method: 'GET',
  });
  if (!isTicketSectorListResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function createTicketSector(
  eventId: string,
  data: CreateTicketSectorRequest,
): Promise<TicketSectorResponse> {
  const request: CreateTicketSectorRequest = {
    name: data.name,
    ...(data.description === undefined ? {} : { description: data.description }),
    capacity: data.capacity,
    price: data.price,
  };
  const payload = await requestJson(`/api/v1/events/${encodeURIComponent(eventId)}/sectors`, {
    method: 'POST',
    headers: csrfHeaders(),
    body: JSON.stringify(request),
  });
  if (!isTicketSectorResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function updateTicketSector(
  eventId: string,
  sectorId: string,
  data: UpdateTicketSectorRequest,
): Promise<TicketSectorResponse> {
  const request: UpdateTicketSectorRequest = {
    name: data.name,
    ...(data.description === undefined ? {} : { description: data.description }),
    capacity: data.capacity,
    price: data.price,
  };
  const payload = await requestJson(
    `/api/v1/events/${encodeURIComponent(eventId)}/sectors/${encodeURIComponent(sectorId)}`,
    {
      method: 'PUT',
      headers: csrfHeaders(),
      body: JSON.stringify(request),
    },
  );
  if (!isTicketSectorResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
}

export async function deleteTicketSector(eventId: string, sectorId: string): Promise<void> {
  const response = await fetch(
    `/api/v1/events/${encodeURIComponent(eventId)}/sectors/${encodeURIComponent(sectorId)}`,
    {
      method: 'DELETE',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        ...csrfHeaders(),
      },
    },
  );
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
  if (typeof document === 'undefined') {
    return null;
  }
  const prefix = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));
  return cookie === undefined ? null : decodeURIComponent(cookie.slice(prefix.length));
}

async function toApiError(response: Response): Promise<EventClientError> {
  try {
    const payload: unknown = await response.json();
    if (isEventApiError(payload)) {
      return new EventClientError(payload.code, payload.message);
    }
    if (isAuthErrorPayload(payload)) {
      return new EventClientError(payload.code, payload.message);
    }
  } catch {
    // Fallback para respostas sem JSON
  }
  if (response.status === 401) {
    return new EventClientError('AUTH_UNAUTHENTICATED', 'Sessão expirada ou não autenticada.');
  }
  if (response.status === 403) {
    return new EventClientError('AUTH_FORBIDDEN', 'Acesso negado.');
  }
  if (response.status === 404) {
    return new EventClientError('EVENT_NOT_FOUND', 'Evento não encontrado.');
  }
  if (response.status === 409) {
    return new EventClientError('EVENT_CANNOT_BE_MODIFIED', 'Operação conflitante com o estado do evento.');
  }
  return new EventClientError('EVENT_INVALID_REQUEST', 'Erro ao processar evento.');
}

function invalidResponse(): EventClientError {
  return new EventClientError('EVENT_INVALID_RESPONSE', 'Resposta do servidor de eventos inválida.');
}

function isEventResponse(value: unknown): value is EventResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'organizerId',
    'externalSource',
    'externalId',
    'title',
    'description',
    'imageUrl',
    'category',
    'status',
    'venueName',
    'venueAddress',
    'startsAt',
    'createdAt',
    'updatedAt',
  ])) {
    return false;
  }
  return typeof value.id === 'string'
    && typeof value.organizerId === 'string'
    && typeof value.title === 'string'
    && (value.status === 'DRAFT' || value.status === 'PUBLISHED')
    && typeof value.createdAt === 'string'
    && typeof value.updatedAt === 'string'
    && (value.externalSource === undefined || typeof value.externalSource === 'string')
    && (value.externalId === undefined || typeof value.externalId === 'string')
    && (value.description === undefined || typeof value.description === 'string')
    && (value.imageUrl === undefined || typeof value.imageUrl === 'string')
    && (value.category === undefined || typeof value.category === 'string')
    && (value.venueName === undefined || typeof value.venueName === 'string')
    && (value.venueAddress === undefined || typeof value.venueAddress === 'string')
    && (value.startsAt === undefined || typeof value.startsAt === 'string');
}

function isEventListResponse(value: unknown): value is EventListResponse {
  if (!isRecord(value) || !hasExactKeys(value, ['events'])) {
    return false;
  }
  return Array.isArray(value.events) && value.events.every(isEventResponse);
}

export function isPublicEventResponse(value: unknown): value is PublicEventResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'title',
    'description',
    'imageUrl',
    'category',
    'status',
    'venueName',
    'venueAddress',
    'startsAt',
    'startingPrice',
    'salesClosed',
    'createdAt',
    'updatedAt',
  ])) {
    return false;
  }
  return typeof value.id === 'string'
    && typeof value.title === 'string'
    && (value.status === 'DRAFT' || value.status === 'PUBLISHED')
    && typeof value.startingPrice === 'number'
    && typeof value.salesClosed === 'boolean'
    && typeof value.createdAt === 'string'
    && typeof value.updatedAt === 'string'
    && (value.description === undefined || typeof value.description === 'string')
    && (value.imageUrl === undefined || typeof value.imageUrl === 'string')
    && (value.category === undefined || typeof value.category === 'string')
    && (value.venueName === undefined || typeof value.venueName === 'string')
    && (value.venueAddress === undefined || typeof value.venueAddress === 'string')
    && (value.startsAt === undefined || typeof value.startsAt === 'string');
}

export function isPublicEventListResponse(value: unknown): value is PublicEventListResponse {
  if (!isRecord(value) || !hasExactKeys(value, ['events'])) {
    return false;
  }
  return Array.isArray(value.events) && value.events.every(isPublicEventResponse);
}

function isTicketSectorResponse(value: unknown): value is TicketSectorResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'eventId',
    'name',
    'description',
    'capacity',
    'availableQuantity',
    'price',
    'createdAt',
    'updatedAt',
  ])) {
    return false;
  }
  return typeof value.id === 'string'
    && typeof value.eventId === 'string'
    && typeof value.name === 'string'
    && (value.description === undefined || typeof value.description === 'string')
    && typeof value.capacity === 'number'
    && typeof value.availableQuantity === 'number'
    && typeof value.price === 'number'
    && typeof value.createdAt === 'string'
    && typeof value.updatedAt === 'string';
}

function isTicketSectorListResponse(value: unknown): value is TicketSectorListResponse {
  if (!isRecord(value) || !hasExactKeys(value, ['sectors'])) {
    return false;
  }
  return Array.isArray(value.sectors) && value.sectors.every(isTicketSectorResponse);
}

function isEventApiError(value: unknown): value is EventApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  const validCodes = [
    'EVENT_NOT_FOUND',
    'EVENT_FORBIDDEN',
    'EVENT_INVALID_REQUEST',
    'EVENT_CANNOT_BE_DELETED',
    'EVENT_CANNOT_BE_MODIFIED',
  ];
  return typeof value.code === 'string'
    && validCodes.includes(value.code)
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
