import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  saveActiveHold,
  getActiveHold,
  clearActiveHold,
  ACTIVE_HOLD_STORAGE_KEY,
  type StoredActiveHold,
} from '../activeHold';
import type { ReservationResponse } from '../../api/reservationsApi';

const sampleReservation: ReservationResponse = {
  id: 'res-456',
  customerId: 'cust-123',
  eventId: 'evt-789',
  sectorId: 'sec-101',
  quantity: 2,
  unitPrice: 150.0,
  totalAmount: 300.0,
  status: 'HOLDING',
  expiresAt: new Date(Date.now() + 600000).toISOString(),
  createdAt: new Date().toISOString(),
  serverNow: new Date().toISOString(),
};

describe('activeHold storage helpers', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('saves and retrieves an active hold from sessionStorage', () => {
    const hold: StoredActiveHold = {
      reservation: sampleReservation,
      eventTitle: 'Festival de Jazz',
      sectorName: 'Camarote',
      eventVenue: 'Teatro Municipal',
    };

    saveActiveHold(hold);
    const retrieved = getActiveHold();

    expect(retrieved).not.toBeNull();
    expect(retrieved?.reservation.id).toBe('res-456');
    expect(retrieved?.eventTitle).toBe('Festival de Jazz');
    expect(retrieved?.sectorName).toBe('Camarote');
  });

  it('returns null if nothing is stored in sessionStorage', () => {
    expect(getActiveHold()).toBeNull();
  });

  it('returns null if stored JSON is corrupt or does not conform', () => {
    sessionStorage.setItem(ACTIVE_HOLD_STORAGE_KEY, '{invalidJson}');
    expect(getActiveHold()).toBeNull();

    sessionStorage.setItem(ACTIVE_HOLD_STORAGE_KEY, JSON.stringify({ wrongField: 123 }));
    expect(getActiveHold()).toBeNull();
  });

  it('clears the stored active hold', () => {
    saveActiveHold({
      reservation: sampleReservation,
      eventTitle: 'Show de Rock',
    });
    expect(getActiveHold()).not.toBeNull();

    clearActiveHold();
    expect(getActiveHold()).toBeNull();
    expect(sessionStorage.getItem(ACTIVE_HOLD_STORAGE_KEY)).toBeNull();
  });

  it('adjusts serverNow on restore so that duration is not restarted after elapsed time', () => {
    const originalServerNow = new Date('2026-08-16T12:00:00.000Z');
    const expiresAt = new Date('2026-08-16T12:10:00.000Z');

    const res: ReservationResponse = {
      ...sampleReservation,
      serverNow: originalServerNow.toISOString(),
      expiresAt: expiresAt.toISOString(),
    };

    saveActiveHold({
      reservation: res,
      eventTitle: 'Show com tempo decorrido',
    });

    // Simula restauração após 4 minutos decorridos (240.000 ms)
    const stored = JSON.parse(sessionStorage.getItem(ACTIVE_HOLD_STORAGE_KEY)!);
    stored.savedAtClientEpochMs = Date.now() - 240000;
    sessionStorage.setItem(ACTIVE_HOLD_STORAGE_KEY, JSON.stringify(stored));

    const retrieved = getActiveHold();
    expect(retrieved).not.toBeNull();

    const retrievedRemainingMs =
      new Date(retrieved!.reservation.expiresAt).getTime() -
      new Date(retrieved!.reservation.serverNow).getTime();

    // Duração restante deve ser de 6 minutos (360.000 ms), e NÃO reiniciada para 10 minutos (600.000 ms)
    expect(retrievedRemainingMs).toBe(360000);
  });

  it('returns null and clears storage when hold has expired while stored', () => {
    const originalServerNow = new Date('2026-08-16T12:00:00.000Z');
    const expiresAt = new Date('2026-08-16T12:10:00.000Z');

    const res: ReservationResponse = {
      ...sampleReservation,
      serverNow: originalServerNow.toISOString(),
      expiresAt: expiresAt.toISOString(),
    };

    saveActiveHold({
      reservation: res,
      eventTitle: 'Show já vencido',
    });

    // Simula restauração após 11 minutos decorridos (660.000 ms)
    const stored = JSON.parse(sessionStorage.getItem(ACTIVE_HOLD_STORAGE_KEY)!);
    stored.savedAtClientEpochMs = Date.now() - 660000;
    sessionStorage.setItem(ACTIVE_HOLD_STORAGE_KEY, JSON.stringify(stored));

    const retrieved = getActiveHold();
    expect(retrieved).toBeNull();
    expect(sessionStorage.getItem(ACTIVE_HOLD_STORAGE_KEY)).toBeNull();
  });
});
