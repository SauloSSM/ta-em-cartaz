import { useState, useId } from 'react';
import { generateQrMatrix, formatManualCode } from '../lib/qrCode';

export type QRCodePanelProps = {
  validationToken: string;
  manualCode: string;
};

export function QRCodePanel({ validationToken, manualCode }: QRCodePanelProps) {
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);
  const copyStatusId = useId();

  let matrix: boolean[][] = [];
  let qrError = false;
  try {
    matrix = generateQrMatrix(validationToken);
  } catch {
    qrError = true;
  }

  const formattedCode = formatManualCode(manualCode);
  const margin = 4;
  const totalSize = matrix.length + margin * 2;

  const handleCopyCode = async () => {
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(formattedCode);
      }
      setCopyFeedback('Código manual copiado para a área de transferência!');
      setTimeout(() => {
        setCopyFeedback(null);
      }, 3000);
    } catch {
      setCopyFeedback('Não foi possível copiar o código automaticamente.');
    }
  };

  return (
    <div className="edt-qrcode-panel" data-testid="qrcode-panel">
      <div className="edt-qrcode-panel__visual">
        {!qrError && matrix.length > 0 ? (
          <div className="edt-qrcode-panel__qr-container" data-testid="qr-container">
            <svg
              className="edt-qrcode-panel__svg"
              viewBox={`0 0 ${totalSize} ${totalSize}`}
              width="200"
              height="200"
              role="img"
              aria-label="QR do ingresso"
              data-testid="ticket-qr-svg"
            >
              <rect x={0} y={0} width={totalSize} height={totalSize} fill="#ffffff" />
              {matrix.map((row, r) =>
                row.map((cell, c) =>
                  cell ? (
                    <rect
                      key={`${r}-${c}`}
                      x={c + margin}
                      y={r + margin}
                      width={1}
                      height={1}
                      fill="#000000"
                    />
                  ) : null
                )
              )}
            </svg>
          </div>
        ) : (
          <div className="edt-qrcode-panel__qr-fallback" role="img" aria-label="QR do ingresso">
            <p>QR Code indisponível</p>
          </div>
        )}
      </div>

      <div className="edt-qrcode-panel__manual-section">
        <span className="edt-qrcode-panel__manual-label">Código Manual de Entrada</span>
        <div className="edt-qrcode-panel__code-wrapper">
          <code className="edt-qrcode-panel__code" data-testid="ticket-manual-code">
            {formattedCode}
          </code>
          <button
            type="button"
            className="edt-button edt-button--secondary edt-button--small edt-qrcode-panel__copy-btn"
            onClick={() => void handleCopyCode()}
            aria-describedby={copyStatusId}
            data-testid="copy-manual-code-btn"
          >
            Copiar Código
          </button>
        </div>
        <div
          id={copyStatusId}
          className="edt-qrcode-panel__copy-status sr-only"
          role="status"
          aria-live="polite"
        >
          {copyFeedback}
        </div>
        {copyFeedback && (
          <p className="edt-qrcode-panel__copy-feedback" aria-hidden="true">
            {copyFeedback}
          </p>
        )}
      </div>
    </div>
  );
}
