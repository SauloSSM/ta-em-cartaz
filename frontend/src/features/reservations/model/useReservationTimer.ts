import { useState, useEffect, useRef, useCallback } from 'react';
import type { ReservationStatus, ReservationResponse } from '../api/reservationsApi';

export type ReservationTimerState = 'normal' | 'warning' | 'critical' | 'expired';

export type UseReservationTimerOptions = {
  expiresAt: string;
  serverNow: string;
  status?: ReservationStatus;
  onExpire?: () => void;
  onReconcile?: () => Promise<ReservationResponse | null | void> | void;
};

export type UseReservationTimerResult = {
  remainingSeconds: number;
  formattedTime: string;
  state: ReservationTimerState;
  stateLabel: string;
  isExpired: boolean;
  announcement: string | null;
  reconcile: () => Promise<void>;
  isReconciling: boolean;
};

export function useReservationTimer({
  expiresAt,
  serverNow,
  status = 'HOLDING',
  onExpire,
  onReconcile,
}: UseReservationTimerOptions): UseReservationTimerResult {
  const onExpireRef = useRef(onExpire);
  onExpireRef.current = onExpire;

  const onReconcileRef = useRef(onReconcile);
  onReconcileRef.current = onReconcile;

  // Ancora o tempo monotônico local (performance.now()) e a duração inicial autoritativa
  const anchorRef = useRef<{
    initialRemainingMs: number;
    anchorPerfNow: number;
  }>({
    initialRemainingMs: computeInitialRemainingMs(expiresAt, serverNow),
    anchorPerfNow: typeof performance !== 'undefined' ? performance.now() : 0,
  });

  const [remainingSeconds, setRemainingSeconds] = useState<number>(() => {
    const initialMs = computeInitialRemainingMs(expiresAt, serverNow);
    return Math.max(0, Math.floor(initialMs / 1000));
  });

  const [announcement, setAnnouncement] = useState<string | null>(null);
  const [isReconciling, setIsReconciling] = useState(false);

  // Controle de anúncios únicos para marcos canônicos (3 min, 1 min, expiração)
  const announced3MinRef = useRef(false);
  const announced1MinRef = useRef(false);
  const announcedExpiredRef = useRef(false);
  const hasTriggeredExpireCallbackRef = useRef(false);

  // Atualiza a ancoragem sempre que expiresAt ou serverNow autoritativos mudarem
  useEffect(() => {
    const initialMs = computeInitialRemainingMs(expiresAt, serverNow);
    const perfNow = typeof performance !== 'undefined' ? performance.now() : 0;
    anchorRef.current = {
      initialRemainingMs: initialMs,
      anchorPerfNow: perfNow,
    };
    const sec = Math.max(0, Math.floor(initialMs / 1000));
    setRemainingSeconds(sec);

    if (sec > 180) {
      announced3MinRef.current = false;
      announced1MinRef.current = false;
      announcedExpiredRef.current = false;
      hasTriggeredExpireCallbackRef.current = false;
    } else if (sec > 60) {
      announced1MinRef.current = false;
      announcedExpiredRef.current = false;
      hasTriggeredExpireCallbackRef.current = false;
    } else if (sec > 0) {
      announcedExpiredRef.current = false;
      hasTriggeredExpireCallbackRef.current = false;
    }
  }, [expiresAt, serverNow]);

  const triggerReconcile = useCallback(async () => {
    if (!onReconcileRef.current || isReconciling) {
      return;
    }
    setIsReconciling(true);
    try {
      await onReconcileRef.current();
    } catch {
      // Falha na reconciliação mantém o estado atual de contagem até próxima oportunidade
    } finally {
      setIsReconciling(false);
    }
  }, [isReconciling]);

  // Loop de contagem regressiva baseado estritamente em performance.now() monotônico
  useEffect(() => {
    if (status === 'EXPIRED') {
      setRemainingSeconds(0);
      if (!announcedExpiredRef.current) {
        announcedExpiredRef.current = true;
        setAnnouncement('O tempo da sua reserva expirou. Seus ingressos foram liberados.');
      }
      return;
    }

    const intervalId = setInterval(() => {
      const { initialRemainingMs, anchorPerfNow } = anchorRef.current;
      const currentPerfNow = typeof performance !== 'undefined' ? performance.now() : 0;
      const elapsedPerfMs = currentPerfNow - anchorPerfNow;
      const currentRemainingMs = Math.max(0, initialRemainingMs - elapsedPerfMs);
      const sec = Math.max(0, Math.floor(currentRemainingMs / 1000));

      setRemainingSeconds(sec);

      // Marcos ARIA: 3 minutos (180s), 1 minuto (60s) e 0s (expiração)
      if (sec <= 180 && sec > 60 && !announced3MinRef.current) {
        announced3MinRef.current = true;
        setAnnouncement('Restam 3 minutos para concluir sua reserva.');
      } else if (sec <= 60 && sec > 0 && !announced1MinRef.current) {
        announced1MinRef.current = true;
        setAnnouncement('Atenção: resta 1 minuto para concluir sua reserva.');
      } else if (sec <= 0) {
        if (!announcedExpiredRef.current) {
          announcedExpiredRef.current = true;
          setAnnouncement('O tempo da sua reserva expirou. Seus ingressos foram liberados.');
        }
        if (!hasTriggeredExpireCallbackRef.current) {
          hasTriggeredExpireCallbackRef.current = true;
          onExpireRef.current?.();
          void triggerReconcile();
        }
        clearInterval(intervalId);
      }
    }, 250);

    return () => {
      clearInterval(intervalId);
    };
  }, [status, triggerReconcile]);

  // Reconciliação autoritativa ao retornar para a aba (visibilitychange)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && status !== 'EXPIRED' && remainingSeconds > 0) {
        void triggerReconcile();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [status, remainingSeconds, triggerReconcile]);

  const isExpired = remainingSeconds <= 0 || status === 'EXPIRED';

  let state: ReservationTimerState;
  let stateLabel: string;

  if (isExpired) {
    state = 'expired';
    stateLabel = 'Reserva expirada';
  } else if (remainingSeconds < 60) {
    state = 'critical';
    stateLabel = 'Tempo crítico';
  } else if (remainingSeconds <= 179) {
    state = 'warning';
    stateLabel = 'Tempo acabando';
  } else {
    state = 'normal';
    stateLabel = 'Tempo normal';
  }

  const minutes = Math.floor(Math.max(0, remainingSeconds) / 60);
  const seconds = Math.max(0, remainingSeconds) % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  return {
    remainingSeconds,
    formattedTime,
    state,
    stateLabel,
    isExpired,
    announcement,
    reconcile: triggerReconcile,
    isReconciling,
  };
}

function computeInitialRemainingMs(expiresAtIso: string, serverNowIso: string): number {
  try {
    const expiresMs = new Date(expiresAtIso).getTime();
    const serverNowMs = new Date(serverNowIso).getTime();
    if (Number.isNaN(expiresMs) || Number.isNaN(serverNowMs)) {
      return 0;
    }
    return Math.max(0, expiresMs - serverNowMs);
  } catch {
    return 0;
  }
}
