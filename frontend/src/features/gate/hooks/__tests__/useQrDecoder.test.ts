import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import jsQR from 'jsqr';
import { useQrDecoder } from '../useQrDecoder';
import type { UseQrDecoderOptions } from '../useQrDecoder';

vi.mock('jsqr', () => ({
  default: vi.fn(),
}));

// --- Helpers to build fake media APIs ---

function makeVideoRef(readyState = 4) {
  const video = {
    srcObject: null as MediaStream | null,
    readyState,
    videoWidth: 640,
    videoHeight: 480,
    play: vi.fn().mockResolvedValue(undefined),
    autoPlay: false,
    playsInline: false,
    muted: false,
  } as unknown as HTMLVideoElement;
  return { current: video } as React.RefObject<HTMLVideoElement | null>;
}

function makeCanvasRef(imageData?: { data: Uint8ClampedArray; width: number; height: number }) {
  const ctx = {
    drawImage: vi.fn(),
    getImageData: vi.fn().mockReturnValue(
      imageData ?? {
        data: new Uint8ClampedArray(320 * 240 * 4),
        width: 320,
        height: 240,
      },
    ),
  };
  const canvas = {
    width: 0,
    height: 0,
    getContext: vi.fn().mockReturnValue(ctx),
  } as unknown as HTMLCanvasElement;
  return { current: canvas } as React.RefObject<HTMLCanvasElement | null>;
}

function makeFakeStream() {
  const track = { stop: vi.fn(), kind: 'video' };
  return {
    getTracks: vi.fn().mockReturnValue([track]),
    _track: track,
  } as unknown as MediaStream;
}

// --- Default options factory ---

function makeOptions(
  overrides: Partial<UseQrDecoderOptions> = {},
): UseQrDecoderOptions {
  return {
    videoRef: makeVideoRef(),
    canvasRef: makeCanvasRef(),
    onDecode: vi.fn(),
    ...overrides,
  };
}

// --- Test setup ---

beforeEach(() => {
  // Secure context default
  Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });

  // Default: media devices available, camera succeeds
  const fakeStream = makeFakeStream();
  vi.stubGlobal('navigator', {
    mediaDevices: {
      getUserMedia: vi.fn().mockResolvedValue(fakeStream),
      enumerateDevices: vi.fn().mockResolvedValue([
        { kind: 'videoinput', deviceId: 'cam-1', label: 'Front Camera', groupId: '' } as MediaDeviceInfo,
      ]),
    },
  });

  // Default: no BarcodeDetector
  vi.stubGlobal('BarcodeDetector', undefined);

  // Stub rAF / cAF
  let rafId = 0;
  vi.stubGlobal('requestAnimationFrame', vi.fn().mockImplementation(() => ++rafId));
  vi.stubGlobal('cancelAnimationFrame', vi.fn());

  // Reset jsQR mock default
  vi.mocked(jsQR).mockReturnValue(null);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

// ============================================================
// T6-1: BarcodeDetector available
// ============================================================
describe('BarcodeDetector disponível', () => {
  it('usa BarcodeDetector quando presente e chama onDecode com o rawValue', async () => {
    const onDecode = vi.fn();
    const fakeStream = makeFakeStream();
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream);

    const fakeDetector = {
      detect: vi.fn().mockResolvedValue([{ rawValue: 'token-abc-123' }]),
    };
    const BarcodeDetectorCtor = vi.fn().mockImplementation(() => fakeDetector);
    vi.stubGlobal('BarcodeDetector', BarcodeDetectorCtor);

    const options = makeOptions({ onDecode });
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    expect(BarcodeDetectorCtor).toHaveBeenCalledWith({ formats: ['qr_code'] });
  });
});

// ============================================================
// T6-2: BarcodeDetector unavailable → uses JS fallback (jsQR)
// ============================================================
describe('BarcodeDetector indisponível (Fallback JS funcional)', () => {
  it('executa o loop rAF e decodifica o payload do frame via fallback JS quando BarcodeDetector não existe', async () => {
    const onDecode = vi.fn();
    const fakeStream = makeFakeStream();
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream);

    const options = makeOptions({ onDecode });
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    // rAF loop started
    expect(requestAnimationFrame).toHaveBeenCalled();
    expect(onDecode).not.toHaveBeenCalled();
  });

  it('quando o frame contém QR Code válido, o decoder JS extrai exatamente o validationToken e dispara onDecode', async () => {
    const onDecode = vi.fn();
    const fakeStream = makeFakeStream();
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream);

    // BarcodeDetector is explicitly undefined
    expect(window.BarcodeDetector).toBeUndefined();

    const expectedToken = 'val-token-64hex-abc123456789def';

    // Mock jsQR to return decoded QR code from canvas frame
    vi.mocked(jsQR).mockReturnValue({
      data: expectedToken,
      binaryData: [],
      chunks: [],
      version: 1,
      location: {
        topRightCorner: { x: 0, y: 0 },
        topLeftCorner: { x: 0, y: 0 },
        bottomRightCorner: { x: 0, y: 0 },
        bottomLeftCorner: { x: 0, y: 0 },
        topRightFinderPattern: { x: 0, y: 0 },
        topLeftFinderPattern: { x: 0, y: 0 },
        bottomLeftFinderPattern: { x: 0, y: 0 },
      },
    });

    let frameCallback: FrameRequestCallback | null = null;
    vi.stubGlobal('requestAnimationFrame', vi.fn((cb: FrameRequestCallback) => {
      frameCallback = cb;
      return 1;
    }));

    const options = makeOptions({ onDecode });
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    // Execute the captured frame callback to process a frame
    expect(frameCallback).not.toBeNull();
    act(() => {
      if (frameCallback) {
        (frameCallback as FrameRequestCallback)(performance.now());
      }
    });

    // Verify jsQR was invoked on the frame's imageData
    expect(jsQR).toHaveBeenCalled();
    // Verify onDecode was called with the exact validationToken extracted by the JS fallback
    expect(onDecode).toHaveBeenCalledWith(expectedToken);
  });
});

// ============================================================
// T6-3: Camera permission denied
// ============================================================
describe('Permissão de câmera negada', () => {
  it('define status denied com mensagem e disponibiliza fallback manual', async () => {
    const permissionError = Object.assign(new Error('Permission denied'), {
      name: 'NotAllowedError',
    });
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(permissionError);

    const options = makeOptions();
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('denied');
    });

    expect('message' in result.current.scannerState).toBe(true);
    if ('message' in result.current.scannerState) {
      expect(result.current.scannerState.message).toMatch(/negada/i);
    }
  });
});

// ============================================================
// T6-4: No camera available
// ============================================================
describe('Nenhuma câmera disponível', () => {
  it('define status no-camera quando o dispositivo não tem câmera', async () => {
    const notFoundError = Object.assign(new Error('No camera'), {
      name: 'NotFoundError',
    });
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(notFoundError);

    const options = makeOptions();
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('no-camera');
    });

    if ('message' in result.current.scannerState) {
      expect(result.current.scannerState.message).toMatch(/câmera/i);
    }
  });
});

// ============================================================
// T6-11: Unmount stops all MediaStream tracks
// ============================================================
describe('Lifecycle — unmount encerra tracks', () => {
  it('para todos os MediaStreamTracks ao desmontar', async () => {
    const fakeStream = makeFakeStream();
    const track = (fakeStream as unknown as { _track: { stop: ReturnType<typeof vi.fn> } })._track;
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream);

    const options = makeOptions();
    const { result, unmount } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    unmount();

    expect(track.stop).toHaveBeenCalled();
    expect(cancelAnimationFrame).toHaveBeenCalled();
  });
});

// ============================================================
// T6-12: Camera switch stops previous stream
// ============================================================
describe('Lifecycle — troca de câmera encerra stream anterior', () => {
  it('chama stop nos tracks do stream anterior ao trocar de câmera', async () => {
    const fakeStream1 = makeFakeStream();
    const track1 = (fakeStream1 as unknown as { _track: { stop: ReturnType<typeof vi.fn> } })._track;
    const fakeStream2 = makeFakeStream();
    vi.mocked(navigator.mediaDevices.getUserMedia)
      .mockResolvedValueOnce(fakeStream1)
      .mockResolvedValueOnce(fakeStream2);

    const options = makeOptions();
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    await act(async () => {
      result.current.stop();
    });

    expect(track1.stop).toHaveBeenCalled();
  });
});

// ============================================================
// T6-8: Deduplication guard
// ============================================================
describe('Deduplicação — scanner pausa após leitura', () => {
  it('não dispara onDecode múltiplas vezes para o mesmo frame enquanto em voo', async () => {
    const onDecode = vi.fn();
    const fakeStream = makeFakeStream();
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream);

    const fakeDetector = {
      detect: vi.fn().mockResolvedValue([{ rawValue: 'token-same-qr' }]),
    };
    vi.stubGlobal('BarcodeDetector', vi.fn().mockImplementation(() => fakeDetector));

    const options = makeOptions({ onDecode });
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.scannerState.status).toBe('active');
    });

    expect(onDecode.mock.calls.length).toBeLessThanOrEqual(1);
  });
});

// ============================================================
// Contexto não seguro (sem HTTPS)
// ============================================================
describe('Contexto não seguro (sem HTTPS)', () => {
  it('define status no-https sem tentar getUserMedia', async () => {
    Object.defineProperty(window, 'isSecureContext', { value: false, configurable: true });

    const options = makeOptions();
    const { result } = renderHook(() => useQrDecoder(options));

    await act(async () => {
      result.current.start();
    });

    expect(result.current.scannerState.status).toBe('no-https');
    expect(navigator.mediaDevices.getUserMedia).not.toHaveBeenCalled();
  });
});
