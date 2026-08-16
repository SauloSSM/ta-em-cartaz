export const PURCHASE_INTENTION_STORAGE_KEY = 'edt_purchase_intention';
export const PURCHASE_INTENTION_TTL_MS = 15 * 60 * 1000; // 15 minutos conforme AD-11

export type PurchaseIntention = {
  eventId: string;
  ticketSectorId: string;
  quantity: number;
  internalReturnPath: string;
  createdAt: string;
};

export type CreatePurchaseIntentionInput = {
  eventId: string;
  ticketSectorId: string;
  quantity: number;
  internalReturnPath?: string;
  createdAt?: string;
};

export function savePurchaseIntention(input: CreatePurchaseIntentionInput): PurchaseIntention {
  if (!input.eventId || typeof input.eventId !== 'string') {
    throw new Error('eventId é obrigatório para registrar intenção de compra');
  }
  if (!input.ticketSectorId || typeof input.ticketSectorId !== 'string') {
    throw new Error('ticketSectorId é obrigatório para registrar intenção de compra');
  }
  if (!Number.isInteger(input.quantity) || input.quantity < 1 || input.quantity > 6) {
    throw new Error('quantity deve ser um inteiro entre 1 e 6');
  }

  const internalReturnPath = sanitizeReturnPath(input.internalReturnPath, input.eventId);
  const createdAt = input.createdAt ?? new Date().toISOString();

  const intention: PurchaseIntention = {
    eventId: input.eventId,
    ticketSectorId: input.ticketSectorId,
    quantity: input.quantity,
    internalReturnPath,
    createdAt,
  };

  try {
    sessionStorage.setItem(PURCHASE_INTENTION_STORAGE_KEY, JSON.stringify(intention));
  } catch {
    // Falha silenciosa caso sessionStorage não esteja disponível
  }

  return intention;
}

export function getPurchaseIntention(): PurchaseIntention | null {
  try {
    const raw = sessionStorage.getItem(PURCHASE_INTENTION_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const parsed: unknown = JSON.parse(raw);
    if (!isValidIntention(parsed)) {
      clearPurchaseIntention();
      return null;
    }

    if (isPurchaseIntentionExpired(parsed.createdAt)) {
      clearPurchaseIntention();
      return null;
    }

    return parsed;
  } catch {
    clearPurchaseIntention();
    return null;
  }
}

export function clearPurchaseIntention(): void {
  try {
    sessionStorage.removeItem(PURCHASE_INTENTION_STORAGE_KEY);
  } catch {
    // Ignora erro
  }
}

export function isPurchaseIntentionExpired(createdAtIso: string, nowMs: number = Date.now()): boolean {
  try {
    const createdMs = new Date(createdAtIso).getTime();
    if (Number.isNaN(createdMs)) {
      return true;
    }
    return nowMs - createdMs > PURCHASE_INTENTION_TTL_MS;
  } catch {
    return true;
  }
}

function sanitizeReturnPath(path: string | undefined, eventId: string): string {
  if (!path || typeof path !== 'string') {
    return `/events/${encodeURIComponent(eventId)}`;
  }
  // Apenas rotas relativas permitidas dentro da aplicação
  const trimmed = path.trim();
  if (trimmed.startsWith('/') && !trimmed.startsWith('//') && !trimmed.includes('\\')) {
    return trimmed;
  }
  return `/events/${encodeURIComponent(eventId)}`;
}

function isValidIntention(value: unknown): value is PurchaseIntention {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.eventId === 'string' &&
    candidate.eventId.length > 0 &&
    typeof candidate.ticketSectorId === 'string' &&
    candidate.ticketSectorId.length > 0 &&
    typeof candidate.quantity === 'number' &&
    Number.isInteger(candidate.quantity) &&
    candidate.quantity >= 1 &&
    candidate.quantity <= 6 &&
    typeof candidate.internalReturnPath === 'string' &&
    candidate.internalReturnPath.startsWith('/') &&
    typeof candidate.createdAt === 'string' &&
    !Number.isNaN(new Date(candidate.createdAt).getTime())
  );
}
