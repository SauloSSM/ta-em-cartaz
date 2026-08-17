import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GateQrScanner } from '../GateQrScanner';

function makeMockStream() {
  const track = { stop: vi.fn(), kind: 'video' };
  return {
    getTracks: vi.fn().mockReturnValue([track]),
    _track: track,
  } as unknown as MediaStream;
}

describe('GateQrScanner component (Story 7.4)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });
    vi.stubGlobal('navigator', {
      mediaDevices: {
        getUserMedia: vi.fn().mockResolvedValue(makeMockStream()),
        enumerateDevices: vi.fn().mockResolvedValue([
          { kind: 'videoinput', deviceId: 'cam-rear', label: 'Câmera Traseira', groupId: '' } as MediaDeviceInfo,
          { kind: 'videoinput', deviceId: 'cam-front', label: 'Câmera Frontal', groupId: '' } as MediaDeviceInfo,
        ]),
      },
    });
    vi.stubGlobal('BarcodeDetector', undefined);
    vi.stubGlobal('requestAnimationFrame', vi.fn().mockImplementation(() => 1));
    vi.stubGlobal('cancelAnimationFrame', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renderiza o scanner de QR com título, live region e vídeo', async () => {
    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    expect(screen.getByTestId('gate-qr-scanner')).toBeDefined();
    expect(screen.getByRole('heading', { level: 4, name: /Scanner de QR Code/i })).toBeDefined();
    expect(screen.getByTestId('gate-scanner-video')).toBeDefined();
    expect(screen.getByTestId('gate-scanner-status-region')).toBeDefined();
  });

  it('permite alternar para código manual via botão no cabeçalho', async () => {
    const user = userEvent.setup();
    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    const switchBtn = screen.getByTestId('gate-scanner-switch-manual-btn');
    await user.click(switchBtn);

    expect(onSwitchToManual).toHaveBeenCalledTimes(1);
  });

  it('permite alternar para código manual via link no hint inferior', async () => {
    const user = userEvent.setup();
    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    const hintLink = screen.getByTestId('gate-scanner-hint-manual-btn');
    await user.click(hintLink);

    expect(onSwitchToManual).toHaveBeenCalledTimes(1);
  });

  it('exibe seletor de câmeras quando há múltiplas câmeras disponíveis', async () => {
    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    await waitFor(() => {
      const select = screen.getByRole('combobox', { name: /Selecionar câmera/i });
      expect(select).toBeDefined();
      expect(screen.getByText('Câmera Traseira')).toBeDefined();
      expect(screen.getByText('Câmera Frontal')).toBeDefined();
    });
  });

  it('exibe overlay de validação quando paused=true', async () => {
    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        paused={true}
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('gate-scanner-paused')).toBeDefined();
      expect(screen.getByText('Validando...')).toBeDefined();
    });
  });

  it('exibe banner de erro acessível quando a permissão de câmera é negada', async () => {
    const user = userEvent.setup();
    const permissionError = Object.assign(new Error('Permission denied'), {
      name: 'NotAllowedError',
    });
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(permissionError);

    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    const errorBanner = await screen.findByTestId('gate-scanner-error');
    expect(errorBanner).toBeDefined();
    expect(screen.getByText(/Permissão de câmera negada/i)).toBeDefined();

    const manualBtn = screen.getByTestId('gate-scanner-use-manual-btn');
    await user.click(manualBtn);
    expect(onSwitchToManual).toHaveBeenCalledTimes(1);
  });

  it('exibe banner de erro quando nenhuma câmera é encontrada', async () => {
    const notFoundError = Object.assign(new Error('Devices not found'), {
      name: 'NotFoundError',
    });
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(notFoundError);

    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    const errorBanner = await screen.findByTestId('gate-scanner-error');
    expect(errorBanner).toBeDefined();
    expect(screen.getByText(/Nenhuma câmera encontrada/i)).toBeDefined();
  });

  it('exibe banner de erro em contexto não seguro (sem HTTPS)', async () => {
    Object.defineProperty(window, 'isSecureContext', { value: false, configurable: true });

    const onDecode = vi.fn();
    const onSwitchToManual = vi.fn();

    render(
      <GateQrScanner
        onDecode={onDecode}
        onSwitchToManual={onSwitchToManual}
      />,
    );

    const errorBanner = await screen.findByTestId('gate-scanner-error');
    expect(errorBanner).toBeDefined();
    expect(screen.getByText(/requer conexão segura \(HTTPS\)/i)).toBeDefined();
  });
});
