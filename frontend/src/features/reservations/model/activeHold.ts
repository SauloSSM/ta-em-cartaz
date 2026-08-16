import type { ReservationResponse } from '../api/reservationsApi';

export const ACTIVE_HOLD_STORAGE_KEY = 'edt.active-hold.v1';

export type StoredActiveHold = {
  reservation: ReservationResponse;
  eventTitle?: string;
  eventDate?: string;
  eventVenue?: string;
  sectorName?: string;
  savedAtClientEpochMs?: number;
};

export function saveActiveHold(hold: StoredActiveHold): void {
  try {
    const toSave: StoredActiveHold = {
      ...hold,
      savedAtClientEpochMs: Date.now(),
    };
    sessionStorage.setItem(ACTIVE_HOLD_STORAGE_KEY, JSON.stringify(toSave));
  } catch {
    // Silently handle environments where sessionStorage is not accessible
  }
}

export function getActiveHold(): StoredActiveHold | null {
  try {
    const raw = sessionStorage.getItem(ACTIVE_HOLD_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as unknown;
    if (!isStoredActiveHold(parsed)) {
      return null;
    }

    if (parsed.reservation.status !== 'HOLDING') {
      clearActiveHold();
      return null;
    }

    const expiresMs = new Date(parsed.reservation.expiresAt).getTime();
    const originalServerNowMs = new Date(parsed.reservation.serverNow).getTime();

    if (Number.isNaN(expiresMs) || Number.isNaN(originalServerNowMs)) {
      clearActiveHold();
      return null;
    }

    const originalRemainingMs = Math.max(0, expiresMs - originalServerNowMs);
    const elapsedClientMs =
      parsed.savedAtClientEpochMs !== undefined
        ? Math.max(0, Date.now() - parsed.savedAtClientEpochMs)
        : 0;

    const remainingMs = Math.max(0, originalRemainingMs - elapsedClientMs);

    // Se o tempo restante se esgotou ou o timestamp absoluto já passou:
    if (remainingMs <= 0) {
      clearActiveHold();
      return null;
    }

    // Ajusta o serverNow da reserva restaurada de modo que (expiresAt - serverNow)
    // reflita exatamente a duração restante real pós-reload, sem estender nem reiniciar os 10 minutos
    const adjustedServerNowMs = expiresMs - remainingMs;
    const adjustedReservation: ReservationResponse = {
      ...parsed.reservation,
      serverNow: new Date(adjustedServerNowMs).toISOString(),
    };

    return {
      ...parsed,
      reservation: adjustedReservation,
    };
  } catch {
    return null;
  }
}

export function clearActiveHold(): void {
  try {
    sessionStorage.removeItem(ACTIVE_HOLD_STORAGE_KEY);
  } catch {
    // Silently handle environments where sessionStorage is not accessible
  }
}

function isStoredActiveHold(value: unknown): value is StoredActiveHold {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  if (typeof record.reservation !== 'object' || record.reservation === null) {
    return false;
  }
  const res = record.reservation as Record<string, unknown>;
  return (
    typeof res.id === 'string' &&
    typeof res.customerId === 'string' &&
    typeof res.eventId === 'string' &&
    typeof res.sectorId === 'string' &&
    typeof res.quantity === 'number' &&
    typeof res.unitPrice === 'number' &&
    typeof res.totalAmount === 'number' &&
    typeof res.status === 'string' &&
    typeof res.expiresAt === 'string' &&
    typeof res.createdAt === 'string' &&
    typeof res.serverNow === 'string' &&
    (record.savedAtClientEpochMs === undefined || typeof record.savedAtClientEpochMs === 'number')
  );
}
