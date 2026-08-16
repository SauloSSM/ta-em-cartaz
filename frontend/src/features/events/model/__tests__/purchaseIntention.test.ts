import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import {
  savePurchaseIntention,
  getPurchaseIntention,
  clearPurchaseIntention,
  isPurchaseIntentionExpired,
  PURCHASE_INTENTION_STORAGE_KEY,
} from '../purchaseIntention';

describe('purchaseIntention storage model (AD-11)', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('salva e recupera uma intenção de compra válida', () => {
    const saved = savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 2,
    });

    expect(saved.eventId).toBe('evt-123');
    expect(saved.ticketSectorId).toBe('sec-456');
    expect(saved.quantity).toBe(2);
    expect(saved.internalReturnPath).toBe('/events/evt-123');
    expect(saved.createdAt).toBeDefined();

    const retrieved = getPurchaseIntention();
    expect(retrieved).not.toBeNull();
    expect(retrieved?.eventId).toBe('evt-123');
    expect(retrieved?.ticketSectorId).toBe('sec-456');
    expect(retrieved?.quantity).toBe(2);
  });

  it('valida que a quantidade deve ser entre 1 e 6', () => {
    expect(() =>
      savePurchaseIntention({
        eventId: 'evt-123',
        ticketSectorId: 'sec-456',
        quantity: 0,
      }),
    ).toThrow('quantity deve ser um inteiro entre 1 e 6');

    expect(() =>
      savePurchaseIntention({
        eventId: 'evt-123',
        ticketSectorId: 'sec-456',
        quantity: 7,
      }),
    ).toThrow('quantity deve ser um inteiro entre 1 e 6');

    expect(() =>
      savePurchaseIntention({
        eventId: 'evt-123',
        ticketSectorId: 'sec-456',
        quantity: 2.5,
      }),
    ).toThrow('quantity deve ser um inteiro entre 1 e 6');
  });

  it('sanitiza e aceita rota interna permitida', () => {
    const saved = savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 4,
      internalReturnPath: '/events/custom-path-123',
    });

    expect(saved.internalReturnPath).toBe('/events/custom-path-123');

    // Rejeita rota absoluta externa
    const malicious = savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 4,
      internalReturnPath: 'https://evil.com/phishing',
    });
    expect(malicious.internalReturnPath).toBe('/events/evt-123');
  });

  it('expira e limpa a intenção após 15 minutos', () => {
    const sixteenMinutesAgo = new Date(Date.now() - 16 * 60 * 1000).toISOString();
    expect(isPurchaseIntentionExpired(sixteenMinutesAgo)).toBe(true);

    savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 1,
      createdAt: sixteenMinutesAgo,
    });

    const retrieved = getPurchaseIntention();
    expect(retrieved).toBeNull();
    expect(sessionStorage.getItem(PURCHASE_INTENTION_STORAGE_KEY)).toBeNull();
  });

  it('mantém a intenção quando dentro do prazo de 15 minutos', () => {
    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString();
    expect(isPurchaseIntentionExpired(tenMinutesAgo)).toBe(false);

    savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 3,
      createdAt: tenMinutesAgo,
    });

    const retrieved = getPurchaseIntention();
    expect(retrieved).not.toBeNull();
    expect(retrieved?.quantity).toBe(3);
  });

  it('limpa intenção quando clearPurchaseIntention é invocado', () => {
    savePurchaseIntention({
      eventId: 'evt-123',
      ticketSectorId: 'sec-456',
      quantity: 2,
    });

    expect(getPurchaseIntention()).not.toBeNull();
    clearPurchaseIntention();
    expect(getPurchaseIntention()).toBeNull();
  });

  it('retorna null e limpa storage quando dados estão corrompidos', () => {
    sessionStorage.setItem(PURCHASE_INTENTION_STORAGE_KEY, '{invalidJson}');
    expect(getPurchaseIntention()).toBeNull();
    expect(sessionStorage.getItem(PURCHASE_INTENTION_STORAGE_KEY)).toBeNull();

    sessionStorage.setItem(PURCHASE_INTENTION_STORAGE_KEY, JSON.stringify({ eventId: 'evt-123' }));
    expect(getPurchaseIntention()).toBeNull();
  });
});
