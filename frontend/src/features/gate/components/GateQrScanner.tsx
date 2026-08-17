import { useRef, useState, useCallback, useEffect } from 'react';
import { useQrDecoder } from '../hooks/useQrDecoder';
import type { QrScannerState } from '../hooks/useQrDecoder';

export type GateQrScannerProps = {
  /** When true, the decode loop is paused (e.g. during HTTP validation). */
  paused?: boolean;
  onDecode: (validationToken: string) => void;
  onSwitchToManual: () => void;
};

/** Camera selector row shown when multiple cameras are available. */
function CameraSelector({
  cameras,
  selectedDeviceId,
  onChange,
}: {
  cameras: MediaDeviceInfo[];
  selectedDeviceId: string | undefined;
  onChange: (deviceId: string) => void;
}) {
  if (cameras.length <= 1) return null;
  return (
    <div className="edt-gate-scanner__camera-selector">
      <label htmlFor="gate-camera-select" className="edt-gate-form-label">
        Câmera
      </label>
      <select
        id="gate-camera-select"
        className="edt-gate-scanner__camera-select"
        value={selectedDeviceId ?? ''}
        onChange={(e) => onChange(e.target.value)}
        aria-label="Selecionar câmera"
      >
        {cameras.map((cam, idx) => (
          <option key={cam.deviceId} value={cam.deviceId}>
            {cam.label || `Câmera ${idx + 1}`}
          </option>
        ))}
      </select>
    </div>
  );
}

/** Error banner shown for camera permission/availability errors. */
function CameraErrorBanner({
  state,
  onSwitchToManual,
}: {
  state: QrScannerState;
  onSwitchToManual: () => void;
}) {
  const message =
    'message' in state
      ? state.message
      : 'Não foi possível iniciar a câmera. Use o formulário de código manual abaixo.';

  return (
    <div
      className="edt-gate-scanner__error"
      role="alert"
      data-testid="gate-scanner-error"
    >
      <span className="edt-gate-scanner__error-icon" aria-hidden="true">
        📷
      </span>
      <div className="edt-gate-scanner__error-body">
        <p className="edt-gate-scanner__error-msg">{message}</p>
        <button
          type="button"
          className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
          onClick={onSwitchToManual}
          data-testid="gate-scanner-use-manual-btn"
        >
          Usar código manual
        </button>
      </div>
    </div>
  );
}

export function GateQrScanner({ paused = false, onDecode, onSwitchToManual }: GateQrScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | undefined>(undefined);

  const { scannerState, start, stop, cameras } = useQrDecoder({
    videoRef: videoRef as React.RefObject<HTMLVideoElement | null>,
    canvasRef: canvasRef as React.RefObject<HTMLCanvasElement | null>,
    onDecode,
    deviceId: selectedDeviceId,
    paused,
  });

  // Start scanner on mount
  useEffect(() => {
    start();
    return () => {
      stop();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Restart scanner when camera device changes
  const handleCameraChange = useCallback(
    (deviceId: string) => {
      stop();
      setSelectedDeviceId(deviceId);
    },
    [stop],
  );

  // Restart when selectedDeviceId changes (after handleCameraChange)
  useEffect(() => {
    if (selectedDeviceId !== undefined) {
      start();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDeviceId]);

  const isError =
    scannerState.status === 'denied' ||
    scannerState.status === 'no-camera' ||
    scannerState.status === 'no-https' ||
    scannerState.status === 'unsupported';

  const isActive = scannerState.status === 'active' || scannerState.status === 'paused';
  const isRequesting = scannerState.status === 'requesting';

  return (
    <section
      className="edt-gate-scanner"
      aria-labelledby="gate-scanner-heading"
      data-testid="gate-qr-scanner"
    >
      <div className="edt-gate-scanner__header">
        <h4 id="gate-scanner-heading" className="edt-gate-scanner__title">
          Scanner de QR Code
        </h4>
        <button
          type="button"
          className="edt-gate-btn edt-gate-btn--secondary edt-gate-btn--small"
          onClick={onSwitchToManual}
          data-testid="gate-scanner-switch-manual-btn"
          aria-label="Alternar para entrada de código manual"
        >
          Código manual
        </button>
      </div>

      {/* Status announcement for screen readers */}
      <div
        role="status"
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
        data-testid="gate-scanner-status-region"
      >
        {isRequesting && 'Solicitando acesso à câmera...'}
        {isActive && !paused && 'Scanner ativo. Aponte a câmera para o QR Code do ingresso.'}
        {paused && 'Scanner pausado durante validação.'}
        {isError && 'Câmera indisponível. Use o formulário de código manual.'}
      </div>

      {isError ? (
        <CameraErrorBanner state={scannerState} onSwitchToManual={onSwitchToManual} />
      ) : (
        <>
          <div
            className={`edt-gate-scanner__viewfinder ${paused ? 'edt-gate-scanner__viewfinder--paused' : ''}`}
            aria-hidden="true"
          >
            {isRequesting && (
              <div className="edt-gate-scanner__overlay" data-testid="gate-scanner-requesting">
                <span>Acessando câmera...</span>
              </div>
            )}
            {paused && (
              <div className="edt-gate-scanner__overlay edt-gate-scanner__overlay--paused" data-testid="gate-scanner-paused">
                <span>Validando...</span>
              </div>
            )}
            <video
              ref={videoRef}
              className="edt-gate-scanner__video"
              autoPlay
              playsInline
              muted
              aria-label="Câmera de leitura de QR Code"
              data-testid="gate-scanner-video"
            />
            {/* Hidden canvas used for fallback rAF decoding */}
            <canvas
              ref={canvasRef}
              className="edt-gate-scanner__canvas"
              aria-hidden="true"
              data-testid="gate-scanner-canvas"
            />
          </div>

          <CameraSelector
            cameras={cameras}
            selectedDeviceId={selectedDeviceId}
            onChange={handleCameraChange}
          />
        </>
      )}

      <p className="edt-gate-scanner__hint">
        O scanner de QR é o caminho principal. Se preferir, use{' '}
        <button
          type="button"
          className="edt-gate-scanner__hint-link"
          onClick={onSwitchToManual}
          data-testid="gate-scanner-hint-manual-btn"
        >
          entrada manual
        </button>
        .
      </p>
    </section>
  );
}
