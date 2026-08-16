export type PaymentSimulatedOutcome = 'APPROVED' | 'DECLINED';

export type PaymentStatus = 'PENDING' | 'APPROVED' | 'DECLINED';

export type ProcessPaymentRequest = {
  paymentAttemptId: string;
  simulatedOutcome: PaymentSimulatedOutcome;
};

export type PaymentResponse = {
  amount: number;
  createdAt: string;
  currency: string;
  declineReason?: string;
  id: string;
  processedAt: string;
  provider: string;
  reservationId: string;
  status: PaymentStatus;
};

export type PaymentErrorCode =
  | 'RESERVATION_NOT_FOUND'
  | 'RESERVATION_EXPIRED'
  | 'IDEMPOTENCY_CONFLICT'
  | 'AUTH_INVALID_REQUEST'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_UNAUTHENTICATED';

export type FieldError = {
  field: string;
  message: string;
};

export type PaymentApiError = {
  code: PaymentErrorCode;
  fieldErrors?: FieldError[];
  message: string;
  timestamp: string;
  traceId: string;
};

export type ClientPaymentErrorCode =
  | PaymentErrorCode
  | 'PAYMENT_INVALID_RESPONSE'
  | 'AUTH_UNAUTHENTICATED'
  | 'AUTH_FORBIDDEN'
  | 'AUTH_CSRF_INVALID';

export class PaymentClientError extends Error {
  readonly code: ClientPaymentErrorCode;
  readonly fieldErrors?: FieldError[];
  readonly traceId?: string;
  readonly timestamp?: string;

  constructor(
    code: ClientPaymentErrorCode,
    message: string,
    fieldErrors?: FieldError[],
    traceId?: string,
    timestamp?: string,
  ) {
    super(message);
    this.name = 'PaymentClientError';
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
    this.timestamp = timestamp;
  }
}

export function generatePaymentAttemptId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return '00000000-0000-4000-8000-000000000000';
}

export async function processPayment(
  reservationId: string,
  data: ProcessPaymentRequest,
): Promise<PaymentResponse> {
  const request: ProcessPaymentRequest = {
    paymentAttemptId: data.paymentAttemptId,
    simulatedOutcome: data.simulatedOutcome,
  };
  const headers: HeadersInit = {
    ...csrfHeaders(),
  };
  const payload = await requestJson(
    `/api/v1/reservations/${encodeURIComponent(reservationId)}/payments`,
    {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
    },
  );
  if (!isPaymentResponse(payload)) {
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

async function toApiError(response: Response): Promise<PaymentClientError> {
  try {
    const payload: unknown = await response.json();
    if (isPaymentApiError(payload)) {
      return new PaymentClientError(
        payload.code,
        payload.message,
        payload.fieldErrors,
        payload.traceId,
        payload.timestamp,
      );
    }
    if (isAuthErrorPayload(payload)) {
      return new PaymentClientError(payload.code, payload.message);
    }
  } catch {
    // Fallback para respostas sem JSON
  }

  if (response.status === 401) {
    return new PaymentClientError('AUTH_UNAUTHENTICATED', 'Sessão expirada ou não autenticada.');
  }
  if (response.status === 403) {
    return new PaymentClientError('AUTH_FORBIDDEN', 'Acesso restrito a clientes.');
  }
  if (response.status === 404) {
    return new PaymentClientError('RESERVATION_NOT_FOUND', 'Reserva não encontrada.');
  }
  if (response.status === 409) {
    return new PaymentClientError('IDEMPOTENCY_CONFLICT', 'Conflito de idempotência no pagamento.');
  }
  if (response.status === 422) {
    return new PaymentClientError('RESERVATION_EXPIRED', 'A reserva expirou.');
  }
  return new PaymentClientError('AUTH_INVALID_REQUEST', 'Erro ao processar pagamento.');
}

function invalidResponse(): PaymentClientError {
  return new PaymentClientError('PAYMENT_INVALID_RESPONSE', 'Resposta do servidor de pagamentos inválida.');
}

function isPaymentResponse(value: unknown): value is PaymentResponse {
  if (!isRecord(value) || !hasOnlyKeys(value, [
    'id',
    'reservationId',
    'amount',
    'currency',
    'status',
    'provider',
    'declineReason',
    'createdAt',
    'processedAt',
  ])) {
    return false;
  }
  const validStatuses: PaymentStatus[] = ['PENDING', 'APPROVED', 'DECLINED'];
  return typeof value.id === 'string'
    && typeof value.reservationId === 'string'
    && typeof value.amount === 'number'
    && typeof value.currency === 'string'
    && typeof value.status === 'string'
    && validStatuses.includes(value.status as PaymentStatus)
    && typeof value.provider === 'string'
    && (value.declineReason === undefined || typeof value.declineReason === 'string')
    && typeof value.createdAt === 'string'
    && typeof value.processedAt === 'string';
}

function isPaymentApiError(value: unknown): value is PaymentApiError {
  if (!isRecord(value) || !hasOnlyKeys(value, ['code', 'message', 'fieldErrors', 'traceId', 'timestamp'])) {
    return false;
  }
  const validCodes: PaymentErrorCode[] = [
    'RESERVATION_NOT_FOUND',
    'RESERVATION_EXPIRED',
    'IDEMPOTENCY_CONFLICT',
    'AUTH_INVALID_REQUEST',
    'AUTH_FORBIDDEN',
    'AUTH_UNAUTHENTICATED',
  ];
  return typeof value.code === 'string'
    && validCodes.includes(value.code as PaymentErrorCode)
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
