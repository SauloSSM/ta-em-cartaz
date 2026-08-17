import { listPublicEvents, type PublicEventResponse } from '../../events';

export type GateEvent = PublicEventResponse;

export type ValidationMethod = 'MANUAL' | 'QR';

export type GateValidationResult =
  | 'VALID'
  | 'INVALID'
  | 'ALREADY_USED'
  | 'WRONG_EVENT';

export type ValidateTicketRequest = {
  manualCode: string;
  method: ValidationMethod;
  selectedEventId: string;
  validationAttemptId: string;
};

export type ValidateTicketResponse = {
  method: ValidationMethod;
  processedAt: string;
  result: GateValidationResult;
  selectedEventId: string;
  ticketId?: string;
  validationAttemptId: string;
};

export type GateErrorCode =
  | 'GATE_INVALID_REQUEST'
  | 'GATE_ATTEMPT_CONFLICT'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN';

export type FieldError = {
  field: string;
  message: string;
};

export type GateApiError = {
  code: GateErrorCode;
  fieldErrors?: FieldError[];
  message: string;
  timestamp: string;
  traceId: string;
};

export type ClientGateErrorCode =
  | GateErrorCode
  | 'GATE_INVALID_RESPONSE'
  | 'AUTH_CSRF_INVALID';

export class GateClientError extends Error {
  readonly code: ClientGateErrorCode;
  readonly fieldErrors?: FieldError[];
  readonly traceId?: string;
  readonly timestamp?: string;

  constructor(
    code: ClientGateErrorCode,
    message: string,
    fieldErrors?: FieldError[],
    traceId?: string,
    timestamp?: string,
  ) {
    super(message);
    this.name = 'GateClientError';
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
    this.timestamp = timestamp;
  }
}

export async function listGateEvents(search?: string): Promise<GateEvent[]> {
  const response = await listPublicEvents(search);
  return response.events;
}

export async function validateTicket(
  input: ValidateTicketRequest,
): Promise<ValidateTicketResponse> {
  const request: ValidateTicketRequest = {
    validationAttemptId: input.validationAttemptId,
    selectedEventId: input.selectedEventId,
    manualCode: input.manualCode.trim(),
    method: input.method,
  };

  const payload = await requestJson('/api/v1/gate/validations', {
    method: 'POST',
    headers: {
      ...csrfHeaders(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!isValidateTicketResponse(payload)) {
    throw invalidResponse();
  }
  return payload;
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

async function toApiError(response: Response): Promise<GateClientError> {
  try {
    const payload: unknown = await response.json();
    if (isGateApiError(payload)) {
      return new GateClientError(
        payload.code,
        payload.message,
        payload.fieldErrors,
        payload.traceId,
        payload.timestamp,
      );
    }
  } catch {
    // Fallback para respostas sem JSON
  }

  if (response.status === 401) {
    return new GateClientError(
      'AUTH_UNAUTHENTICATED',
      'Sessão expirada ou não autenticada.',
    );
  }
  if (response.status === 403) {
    return new GateClientError(
      'AUTH_FORBIDDEN',
      'Acesso restrito a operadores de portaria.',
    );
  }
  if (response.status === 409) {
    return new GateClientError(
      'GATE_ATTEMPT_CONFLICT',
      'Conflito na tentativa de validacão.',
    );
  }
  return new GateClientError(
    'GATE_INVALID_RESPONSE',
    'Erro ao validar ingresso.',
  );
}

function invalidResponse(): GateClientError {
  return new GateClientError(
    'GATE_INVALID_RESPONSE',
    'Resposta do servidor de validação inválida.',
  );
}

function isValidateTicketResponse(value: unknown): value is ValidateTicketResponse {
  if (
    !isRecord(value) ||
    !hasOnlyKeys(value, [
      'result',
      'validationAttemptId',
      'selectedEventId',
      'ticketId',
      'method',
      'processedAt',
    ])
  ) {
    return false;
  }
  const validResults: GateValidationResult[] = [
    'VALID',
    'INVALID',
    'ALREADY_USED',
    'WRONG_EVENT',
  ];
  const validMethods: ValidationMethod[] = ['MANUAL', 'QR'];

  return (
    typeof value.result === 'string' &&
    validResults.includes(value.result as GateValidationResult) &&
    typeof value.validationAttemptId === 'string' &&
    typeof value.selectedEventId === 'string' &&
    (value.ticketId === undefined || typeof value.ticketId === 'string') &&
    typeof value.method === 'string' &&
    validMethods.includes(value.method as ValidationMethod) &&
    typeof value.processedAt === 'string'
  );
}

function isGateApiError(value: unknown): value is GateApiError {
  if (
    !isRecord(value) ||
    !hasOnlyKeys(value, [
      'code',
      'message',
      'fieldErrors',
      'traceId',
      'timestamp',
    ])
  ) {
    return false;
  }
  const validCodes: GateErrorCode[] = [
    'GATE_INVALID_REQUEST',
    'GATE_ATTEMPT_CONFLICT',
    'AUTH_UNAUTHENTICATED',
    'AUTH_FORBIDDEN',
  ];
  return (
    typeof value.code === 'string' &&
    validCodes.includes(value.code as GateErrorCode) &&
    typeof value.message === 'string' &&
    typeof value.traceId === 'string' &&
    typeof value.timestamp === 'string' &&
    (value.fieldErrors === undefined ||
      (Array.isArray(value.fieldErrors) &&
        value.fieldErrors.every(isFieldError)))
  );
}

function isFieldError(value: unknown): value is FieldError {
  return (
    isRecord(value) &&
    hasExactKeys(value, ['field', 'message']) &&
    typeof value.field === 'string' &&
    typeof value.message === 'string'
  );
}

function hasExactKeys(
  value: Record<string, unknown>,
  expected: string[],
): boolean {
  return (
    Object.keys(value).length === expected.length &&
    expected.every((key) => key in value)
  );
}

function hasOnlyKeys(
  value: Record<string, unknown>,
  allowed: string[],
): boolean {
  return Object.keys(value).every((key) => allowed.includes(key));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
