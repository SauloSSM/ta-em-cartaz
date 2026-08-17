import { describe, it, expect } from 'vitest';
import { generateQrMatrix } from './qrCode';
import jsQR from 'jsqr';

function matrixToRgba(matrix: boolean[][], scale = 4, margin = 4): { data: Uint8ClampedArray; width: number; height: number } {
  const size = matrix.length;
  const fullSize = (size + margin * 2) * scale;
  const data = new Uint8ClampedArray(fullSize * fullSize * 4);

  // Fill white
  data.fill(255);

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      if (matrix[r][c]) {
        for (let dy = 0; dy < scale; dy++) {
          for (let dx = 0; dx < scale; dx++) {
            const px = (c + margin) * scale + dx;
            const py = (r + margin) * scale + dy;
            const idx = (py * fullSize + px) * 4;
            data[idx] = 0;     // R
            data[idx + 1] = 0; // G
            data[idx + 2] = 0; // B
            data[idx + 3] = 255; // A
          }
        }
      }
    }
  }

  return { data, width: fullSize, height: fullSize };
}

describe('generateQrMatrix', () => {
  it('generates a valid QR matrix that decodes with jsQR for real 64-char validation tokens', () => {
    const tokens = [
      'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
      'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
    ];

    for (const token of tokens) {
      const matrix = generateQrMatrix(token);
      const { data, width, height } = matrixToRgba(matrix, 4, 4);
      const res = jsQR(data, width, height);
      expect(res).not.toBeNull();
      expect(res?.data).toBe(token);
    }
  });

  it('generates valid QR matrices for various payload lengths (Versions 1 to 6)', () => {
    const lengths = [1, 5, 10, 17, 25, 32, 50, 64, 78, 100, 130];
    for (const len of lengths) {
      const payload = 'k'.repeat(len);
      const matrix = generateQrMatrix(payload);
      const { data, width, height } = matrixToRgba(matrix, 4, 4);
      const res = jsQR(data, width, height);
      expect(res).not.toBeNull();
      expect(res?.data).toBe(payload);
    }
  });
});
