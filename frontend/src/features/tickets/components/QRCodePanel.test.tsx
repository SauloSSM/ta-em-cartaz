import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QRCodePanel } from './QRCodePanel';

describe('QRCodePanel', () => {
  const sampleValidationToken = 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2';
  const sampleManualCode = 'AB7K92QX4M';

  it('renders QR code SVG with accessible role and label without leaking payload to alt text', () => {
    render(
      <QRCodePanel
        validationToken={sampleValidationToken}
        manualCode={sampleManualCode}
      />
    );

    const svg = screen.getByTestId('ticket-qr-svg');
    expect(svg).toBeDefined();
    expect(svg.getAttribute('role')).toBe('img');
    expect(svg.getAttribute('aria-label')).toBe('QR do ingresso');
    // AC: payload de QR nunca vira alt text
    expect(svg.getAttribute('aria-label')).not.toContain(sampleValidationToken);
  });

  it('renders formatted Crockford Base32 manual code', () => {
    render(
      <QRCodePanel
        validationToken={sampleValidationToken}
        manualCode={sampleManualCode}
      />
    );

    const codeElement = screen.getByTestId('ticket-manual-code');
    expect(codeElement).toBeDefined();
    // Grouped 4-4-2 format
    expect(codeElement.textContent).toBe('AB7K-92QX-4M');
  });

  it('copies formatted manual code when copy button is clicked', async () => {
    const user = userEvent.setup();
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: writeTextMock },
      writable: true,
      configurable: true,
    });

    render(
      <QRCodePanel
        validationToken={sampleValidationToken}
        manualCode={sampleManualCode}
      />
    );

    const copyBtn = screen.getByTestId('copy-manual-code-btn');
    await user.click(copyBtn);

    expect(writeTextMock).toHaveBeenCalledWith('AB7K-92QX-4M');
    expect(screen.getAllByText(/copiado para a área de transferência/i).length).toBeGreaterThanOrEqual(1);
  });
});
