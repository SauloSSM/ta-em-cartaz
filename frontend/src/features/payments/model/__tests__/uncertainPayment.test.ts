import { describe, it, expect, beforeEach } from 'vitest';
import {
  saveUncertainPayment,
  getUncertainPayment,
  clearUncertainPayment,
  UNCERTAIN_PAYMENT_STORAGE_PREFIX,
  type StoredUncertainPayment,
} from '../uncertainPayment';

describe('uncertainPayment model & sessionStorage persistence', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('saves and retrieves an uncertain payment attempt correctly', () => {
    const attempt: StoredUncertainPayment = {
      reservationId: 'res-100',
      paymentAttemptId: 'att-200',
      simulatedOutcome: 'APPROVED',
      timestamp: 1723800000000,
    };

    saveUncertainPayment(attempt);

    const retrieved = getUncertainPayment('res-100');
    expect(retrieved).toEqual(attempt);
  });

  it('returns null if no uncertain payment exists for reservationId', () => {
    const retrieved = getUncertainPayment('res-999');
    expect(retrieved).toBeNull();
  });

  it('clears uncertain payment on demand', () => {
    const attempt: StoredUncertainPayment = {
      reservationId: 'res-100',
      paymentAttemptId: 'att-200',
      simulatedOutcome: 'DECLINED',
      timestamp: 1723800000000,
    };

    saveUncertainPayment(attempt);
    expect(getUncertainPayment('res-100')).not.toBeNull();

    clearUncertainPayment('res-100');
    expect(getUncertainPayment('res-100')).toBeNull();
    expect(sessionStorage.getItem(`${UNCERTAIN_PAYMENT_STORAGE_PREFIX}res-100`)).toBeNull();
  });

  it('clears and returns null if stored data is corrupted or has invalid schema', () => {
    sessionStorage.setItem(`${UNCERTAIN_PAYMENT_STORAGE_PREFIX}res-bad`, 'invalid-json');
    expect(getUncertainPayment('res-bad')).toBeNull();

    sessionStorage.setItem(
      `${UNCERTAIN_PAYMENT_STORAGE_PREFIX}res-bad2`,
      JSON.stringify({ reservationId: 'res-bad2', paymentAttemptId: 123 }),
    );
    expect(getUncertainPayment('res-bad2')).toBeNull();

    // Mismatched reservationId
    sessionStorage.setItem(
      `${UNCERTAIN_PAYMENT_STORAGE_PREFIX}res-bad3`,
      JSON.stringify({
        reservationId: 'other-res',
        paymentAttemptId: 'att-1',
        simulatedOutcome: 'APPROVED',
        timestamp: 123,
      }),
    );
    expect(getUncertainPayment('res-bad3')).toBeNull();
  });
});
