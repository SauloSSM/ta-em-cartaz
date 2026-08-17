// QrDecoder hook (AD-16) — camera lifecycle, BarcodeDetector progressive enhancement,
// rAF+canvas JS loop as mandatory baseline/fallback.
// No new libraries — browser APIs only.

import { useEffect, useRef, useCallback, useState } from 'react';
import jsQR from 'jsqr';

// --- Types ---

export type QrScannerStatus =
  | 'idle'
  | 'requesting'
  | 'active'
  | 'paused'
  | 'denied'
  | 'no-camera'
  | 'no-https'
  | 'unsupported';

export type QrScannerState =
  | { status: 'idle' }
  | { status: 'requesting' }
  | { status: 'active' }
  | { status: 'paused' }
  | { status: 'denied'; message: string }
  | { status: 'no-camera'; message: string }
  | { status: 'no-https'; message: string }
  | { status: 'unsupported'; message: string };

export type UseQrDecoderOptions = {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  canvasRef: React.RefObject<HTMLCanvasElement | null>;
  onDecode: (value: string) => void;
  deviceId?: string;
  /** When true, stops emitting decoded values (but keeps stream alive). */
  paused?: boolean;
};

export type UseQrDecoderReturn = {
  scannerState: QrScannerState;
  start: () => void;
  stop: () => void;
  cameras: MediaDeviceInfo[];
};

// --- BarcodeDetector feature detection ---

declare global {
  interface Window {
    BarcodeDetector?: new (opts: { formats: string[] }) => {
      detect(src: ImageBitmapSource): Promise<Array<{ rawValue: string }>>;
    };
  }
}

function hasBarcodeDetector(): boolean {
  return typeof window !== 'undefined' && typeof window.BarcodeDetector === 'function';
}

function hasSecureContext(): boolean {
  if (typeof window === 'undefined') return false;
  return window.isSecureContext;
}

function hasMediaDevices(): boolean {
  return (
    typeof navigator !== 'undefined' &&
    typeof navigator.mediaDevices !== 'undefined' &&
    typeof navigator.mediaDevices.getUserMedia === 'function'
  );
}

async function listVideoDevices(): Promise<MediaDeviceInfo[]> {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    return devices.filter((d) => d.kind === 'videoinput');
  } catch {
    return [];
  }
}

// --- Hook ---

export function useQrDecoder({
  videoRef,
  canvasRef,
  onDecode,
  deviceId,
  paused = false,
}: UseQrDecoderOptions): UseQrDecoderReturn {
  const [scannerState, setScannerState] = useState<QrScannerState>({ status: 'idle' });
  const [cameras, setCameras] = useState<MediaDeviceInfo[]>([]);

  // Ref to track active stream — ensures tracks are stopped on switch/unmount
  const streamRef = useRef<MediaStream | null>(null);
  // Ref for the rAF loop handle
  const rafHandleRef = useRef<number | null>(null);
  // Deduplication guard: non-null while a decoded value is being processed
  const inFlightValueRef = useRef<string | null>(null);
  // Mounted guard — prevents setState after unmount
  const mountedRef = useRef(true);
  // Stable ref to latest paused prop to avoid stale closure in rAF loop
  const pausedRef = useRef(paused);
  pausedRef.current = paused;

  const stopTracks = useCallback(() => {
    if (rafHandleRef.current !== null) {
      cancelAnimationFrame(rafHandleRef.current);
      rafHandleRef.current = null;
    }
    if (streamRef.current) {
      const tracks = streamRef.current.getTracks?.();
      if (Array.isArray(tracks)) {
        tracks.forEach((t) => t.stop?.());
      }
      streamRef.current = null;
    }
    const video = videoRef.current;
    if (video) {
      video.srcObject = null;
    }
  }, [videoRef]);

  // Stable ref for onDecode to avoid rAF closure becoming stale
  const onDecodeRef = useRef(onDecode);
  onDecodeRef.current = onDecode;

  const startDecodeLoop = useCallback(
    (stream: MediaStream) => {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (!video || !canvas) return;

      video.srcObject = stream;

      if (typeof video.play === 'function') {
        try {
          const playPromise = video.play();
          if (playPromise && typeof playPromise.catch === 'function') {
            playPromise.catch(() => {
              // autoplay may require user gesture in some browsers — non-fatal
            });
          }
        } catch {
          // non-fatal in test/headless environments
        }
      }

      if (hasBarcodeDetector()) {
        // Progressive enhancement: BarcodeDetector
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        const detector = new window.BarcodeDetector!({ formats: ['qr_code'] });

        const detectFrame = () => {
          if (!mountedRef.current) return;
          if (pausedRef.current || inFlightValueRef.current !== null) {
            rafHandleRef.current = requestAnimationFrame(detectFrame);
            return;
          }
          if (video.readyState >= 2) {
            detector
              .detect(video)
              .then((results) => {
                if (!mountedRef.current) return;
                const first = results[0];
                if (first && first.rawValue && inFlightValueRef.current === null) {
                  inFlightValueRef.current = first.rawValue;
                  onDecodeRef.current(first.rawValue);
                }
              })
              .catch(() => {
                // frame decode error — non-fatal, continue loop
              })
              .finally(() => {
                if (mountedRef.current) {
                  rafHandleRef.current = requestAnimationFrame(detectFrame);
                }
              });
          } else {
            rafHandleRef.current = requestAnimationFrame(detectFrame);
          }
        };
        rafHandleRef.current = requestAnimationFrame(detectFrame);
      } else {
        // Baseline/fallback (AD-16): rAF + canvas 2D context + jsQR pure-JS decoder
        let ctx: CanvasRenderingContext2D | null = null;
        try {
          ctx = canvas.getContext('2d', { willReadFrequently: true });
        } catch {
          // context unavailable
        }
        if (!ctx) return;

        const detectFrame = () => {
          if (!mountedRef.current) return;
          if (pausedRef.current || inFlightValueRef.current !== null) {
            rafHandleRef.current = requestAnimationFrame(detectFrame);
            return;
          }
          if (video.readyState >= 2 && ctx) {
            const width = video.videoWidth || 320;
            const height = video.videoHeight || 240;
            canvas.width = width;
            canvas.height = height;
            ctx.drawImage(video, 0, 0, width, height);

            try {
              const imageData = ctx.getImageData(0, 0, width, height);
              if (imageData && imageData.data) {
                const code = jsQR(imageData.data, imageData.width, imageData.height);
                if (code && code.data && inFlightValueRef.current === null) {
                  inFlightValueRef.current = code.data;
                  onDecodeRef.current(code.data);
                }
              }
            } catch {
              // frame pixel read error — non-fatal, continue loop
            }
          }
          rafHandleRef.current = requestAnimationFrame(detectFrame);
        };
        rafHandleRef.current = requestAnimationFrame(detectFrame);
      }
    },
    [videoRef, canvasRef],
  );

  const start = useCallback(async () => {
    stopTracks();
    inFlightValueRef.current = null;

    if (!hasSecureContext()) {
      if (mountedRef.current) {
        setScannerState({
          status: 'no-https',
          message:
            'O scanner de câmera requer conexão segura (HTTPS). Use o formulário de código manual abaixo.',
        });
      }
      return;
    }

    if (!hasMediaDevices()) {
      if (mountedRef.current) {
        setScannerState({
          status: 'unsupported',
          message:
            'Este navegador não suporta acesso à câmera. Use o formulário de código manual abaixo.',
        });
      }
      return;
    }

    if (mountedRef.current) setScannerState({ status: 'requesting' });

    try {
      const constraints: MediaStreamConstraints = {
        video: deviceId
          ? { deviceId: { exact: deviceId } }
          : { facingMode: 'environment' }, // prefer rear camera per AD-16
        audio: false,
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;

      // Enumerate cameras after permission is granted
      const videoDevices = await listVideoDevices();
      if (mountedRef.current) setCameras(videoDevices);

      if (mountedRef.current) setScannerState({ status: 'active' });
      startDecodeLoop(stream);
    } catch (err: unknown) {
      const error = err as { name?: string };
      if (!mountedRef.current) return;

      if (
        error.name === 'NotAllowedError' ||
        error.name === 'PermissionDeniedError' ||
        error.name === 'SecurityError'
      ) {
        setScannerState({
          status: 'denied',
          message:
            'Permissão de câmera negada. Autorize o acesso na barra do navegador ou use o formulário de código manual abaixo.',
        });
      } else if (
        error.name === 'NotFoundError' ||
        error.name === 'DevicesNotFoundError' ||
        error.name === 'SourceUnavailableError'
      ) {
        setScannerState({
          status: 'no-camera',
          message:
            'Nenhuma câmera encontrada neste dispositivo. Use o formulário de código manual abaixo.',
        });
      } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
        setScannerState({
          status: 'no-camera',
          message:
            'A câmera está em uso por outro aplicativo. Feche-o ou use o formulário de código manual abaixo.',
        });
      } else {
        setScannerState({
          status: 'no-camera',
          message: 'Não foi possível acessar a câmera. Use o formulário de código manual abaixo.',
        });
      }
    }
  }, [deviceId, stopTracks, startDecodeLoop]);

  const stop = useCallback(() => {
    stopTracks();
    if (mountedRef.current) setScannerState({ status: 'idle' });
  }, [stopTracks]);

  /** Called by parent after a validation request completes (success or error),
   *  to allow the next QR frame to be decoded. */
  const clearInFlight = useCallback(() => {
    inFlightValueRef.current = null;
  }, []);

  // Expose clearInFlight via a stable ref on the hook return
  // (parent accesses via ref pattern in GateValidationArea)
  const clearInFlightRef = useRef(clearInFlight);
  clearInFlightRef.current = clearInFlight;

  // When paused becomes false, clear the in-flight guard so decoding can resume
  useEffect(() => {
    if (!paused) {
      inFlightValueRef.current = null;
    }
  }, [paused]);

  // Cleanup on unmount — stops all tracks and cancels rAF
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (rafHandleRef.current !== null) {
        cancelAnimationFrame(rafHandleRef.current);
        rafHandleRef.current = null;
      }
      if (streamRef.current) {
        const tracks = streamRef.current.getTracks?.();
        if (Array.isArray(tracks)) {
          tracks.forEach((t) => t.stop?.());
        }
        streamRef.current = null;
      }
    };
  }, []);

  return { scannerState, start, stop, cameras };
}
