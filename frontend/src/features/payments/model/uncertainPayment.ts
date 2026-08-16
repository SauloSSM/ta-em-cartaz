import type { PaymentSimulatedOutcome } from '../api/paymentsApi';

export const UNCERTAIN_PAYMENT_STORAGE_PREFIX = 'edt.uncertain-payment.v1:';

export type StoredUncertainPayment = {
  reservationId: string;
  paymentAttemptId: string;
  simulatedOutcome: PaymentSimulatedOutcome;
  timestamp: number;
};

export function saveUncertainPayment(attempt: StoredUncertainPayment): void {
  try {
    sessionStorage.setItem(
      `${UNCERTAIN_PAYMENT_STORAGE_PREFIX}${attempt.reservationId}`,
      JSON.stringify(attempt),
    );
  } catch {
    // Silently handle environments where sessionStorage is not accessible
  }
}

export function getUncertainPayment(reservationId: string): StoredUncertainPayment | null {
  try {
    const raw = sessionStorage.getItem(`${UNCERTAIN_PAYMENT_STORAGE_PREFIX}${reservationId}`);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as unknown;
    if (!isStoredUncertainPayment(parsed)) {
      clearUncertainPayment(reservationId);
      return null;
    }
    if (parsed.reservationId !== reservationId) {
      clearUncertainPayment(reservationId);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function clearUncertainPayment(reservationId: string): void {
  try {
    sessionStorage.removeItem(`${UNCERTAIN_PAYMENT_STORAGE_PREFIX}${reservationId}`);
  } catch {
    // Silently handle environments where sessionStorage is not accessible
  }
}

function isStoredUncertainPayment(value: unknown): value is StoredUncertainPayment {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record.reservationId === 'string' &&
    typeof record.paymentAttemptId === 'string' &&
    (record.simulatedOutcome === 'APPROVED' || record.simulatedOutcome === 'DECLINED') &&
    typeof record.timestamp === 'number'
  );
}
