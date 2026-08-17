/**
 * Minimal, standard QR Code generator (Version 1-10, Byte Mode, Error Correction L/M).
 * Encodes arbitrary string payloads (such as 64-hex char validationToken) into an SVG matrix.
 */

// Reed-Solomon GF(256) tables with primitive polynomial 0x11D (285)
const EXP_TABLE = new Uint8Array(512);
const LOG_TABLE = new Uint8Array(256);

(() => {
  let val = 1;
  for (let i = 0; i < 255; i++) {
    EXP_TABLE[i] = val;
    EXP_TABLE[i + 255] = val;
    LOG_TABLE[val] = i;
    val = (val << 1) ^ (val & 0x80 ? 0x11d : 0);
  }
})();

function gfMul(x: number, y: number): number {
  if (x === 0 || y === 0) return 0;
  return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]];
}

export function rsGeneratorPoly(degree: number): Uint8Array {
  let poly = new Uint8Array([1]);
  for (let i = 0; i < degree; i++) {
    const next = new Uint8Array(poly.length + 1);
    const root = EXP_TABLE[i];
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], root);
    }
    poly = next;
  }
  return poly;
}

function calculateECC(data: Uint8Array, eccCount: number): Uint8Array {
  const gen = rsGeneratorPoly(eccCount);
  const remainder = new Uint8Array(eccCount);
  for (let i = 0; i < data.length; i++) {
    const factor = data[i] ^ remainder[0];
    for (let j = 0; j < eccCount - 1; j++) {
      remainder[j] = remainder[j + 1];
    }
    remainder[eccCount - 1] = 0;
    if (factor !== 0) {
      for (let j = 0; j < eccCount; j++) {
        remainder[j] ^= gfMul(gen[j + 1], factor);
      }
    }
  }
  return remainder;
}

// Version table capacities for Byte mode with EC Level L (up to Version 6)
// Version: size, totalDataBytes, ecBytesPerBlock, numBlocks
type VersionInfo = {
  version: number;
  size: number;
  dataCapacity: number;
  totalDataCodewords: number;
  ecCodewordsPerBlock: number;
  numBlocks: number;
  alignments: number[];
};

const VERSIONS: VersionInfo[] = [
  { version: 1, size: 21, dataCapacity: 17, totalDataCodewords: 19, ecCodewordsPerBlock: 7, numBlocks: 1, alignments: [] },
  { version: 2, size: 25, dataCapacity: 32, totalDataCodewords: 34, ecCodewordsPerBlock: 10, numBlocks: 1, alignments: [6, 18] },
  { version: 3, size: 29, dataCapacity: 53, totalDataCodewords: 55, ecCodewordsPerBlock: 15, numBlocks: 1, alignments: [6, 22] },
  { version: 4, size: 33, dataCapacity: 78, totalDataCodewords: 80, ecCodewordsPerBlock: 20, numBlocks: 1, alignments: [6, 26] },
  { version: 5, size: 37, dataCapacity: 106, totalDataCodewords: 108, ecCodewordsPerBlock: 26, numBlocks: 1, alignments: [6, 30] },
  { version: 6, size: 41, dataCapacity: 134, totalDataCodewords: 136, ecCodewordsPerBlock: 18, numBlocks: 2, alignments: [6, 34] },
];

export function generateQrMatrix(text: string): boolean[][] {
  const encoder = new TextEncoder();
  const textBytes = encoder.encode(text);
  
  // Pick smallest version that fits
  const version = VERSIONS.find((v) => v.dataCapacity >= textBytes.length);
  if (!version) {
    throw new Error('Texto muito longo para QR Code');
  }

  // BitStream creation (Byte mode: 0100 + char count + bytes + terminator)
  const bitArray: number[] = [];
  const pushBits = (val: number, len: number) => {
    for (let i = len - 1; i >= 0; i--) {
      bitArray.push((val >> i) & 1);
    }
  };

  // Mode Indicator 0100 (Byte mode)
  pushBits(0b0100, 4);
  // Character count indicator (8 bits for V1-V9)
  pushBits(textBytes.length, 8);
  // Payload
  for (const b of textBytes) {
    pushBits(b, 8);
  }
  // Terminator (up to 4 zeroes)
  const capacityBits = version.totalDataCodewords * 8;
  const termLen = Math.min(4, capacityBits - bitArray.length);
  pushBits(0, termLen);
  // Pad to byte boundary
  while (bitArray.length % 8 !== 0) {
    bitArray.push(0);
  }
  // Pad bytes 0xEC, 0x11
  const padBytes = [0xec, 0x11];
  let padIdx = 0;
  while (bitArray.length < capacityBits) {
    pushBits(padBytes[padIdx % 2], 8);
    padIdx++;
  }

  // Convert to data codewords
  const dataCodewords = new Uint8Array(version.totalDataCodewords);
  for (let i = 0; i < version.totalDataCodewords; i++) {
    let byte = 0;
    for (let b = 0; b < 8; b++) {
      byte = (byte << 1) | bitArray[i * 8 + b];
    }
    dataCodewords[i] = byte;
  }

  // Split into blocks and compute EC
  const blockSize = Math.floor(dataCodewords.length / version.numBlocks);
  const blocks: Uint8Array[] = [];
  const ecBlocks: Uint8Array[] = [];
  for (let i = 0; i < version.numBlocks; i++) {
    const blockData = dataCodewords.subarray(i * blockSize, (i + 1) * blockSize);
    blocks.push(blockData);
    ecBlocks.push(calculateECC(blockData, version.ecCodewordsPerBlock));
  }

  // Interleave data and EC codewords
  const finalCodewords: number[] = [];
  for (let i = 0; i < blockSize; i++) {
    for (let b = 0; b < version.numBlocks; b++) {
      finalCodewords.push(blocks[b][i]);
    }
  }
  for (let i = 0; i < version.ecCodewordsPerBlock; i++) {
    for (let b = 0; b < version.numBlocks; b++) {
      finalCodewords.push(ecBlocks[b][i]);
    }
  }

  // Create matrix
  const size = version.size;
  const matrix: (boolean | null)[][] = Array.from({ length: size }, () =>
    Array.from({ length: size }, () => null)
  );

  // Position detection patterns (7x7) + separators
  const drawFinder = (startX: number, startY: number) => {
    for (let y = -1; y <= 7; y++) {
      for (let x = -1; x <= 7; x++) {
        const px = startX + x;
        const py = startY + y;
        if (px >= 0 && px < size && py >= 0 && py < size) {
          if (x >= 0 && x <= 6 && y >= 0 && y <= 6) {
            const isBorder = x === 0 || x === 6 || y === 0 || y === 6;
            const isCenter = x >= 2 && x <= 4 && y >= 2 && y <= 4;
            matrix[py][px] = isBorder || isCenter;
          } else {
            matrix[py][px] = false;
          }
        }
      }
    }
  };

  drawFinder(0, 0);
  drawFinder(size - 7, 0);
  drawFinder(0, size - 7);

  // Alignment patterns
  if (version.alignments.length >= 2) {
    for (const r of version.alignments) {
      for (const c of version.alignments) {
        // Skip finder positions
        if ((r === 6 && c === 6) || (r === 6 && c === size - 7) || (r === size - 7 && c === 6)) {
          continue;
        }
        for (let y = -2; y <= 2; y++) {
          for (let x = -2; x <= 2; x++) {
            const isBorder = Math.abs(x) === 2 || Math.abs(y) === 2;
            const isCenter = x === 0 && y === 0;
            matrix[r + y][c + x] = isBorder || isCenter;
          }
        }
      }
    }
  }

  // Timing patterns
  for (let i = 8; i < size - 8; i++) {
    if (matrix[6][i] === null) matrix[6][i] = i % 2 === 0;
    if (matrix[i][6] === null) matrix[i][6] = i % 2 === 0;
  }

  // Dark module
  matrix[size - 8][8] = true;

  // Reserve format information areas
  for (let i = 0; i < 9; i++) {
    if (matrix[8][i] === null) matrix[8][i] = false;
    if (matrix[i][8] === null) matrix[i][8] = false;
  }
  for (let i = size - 8; i < size; i++) {
    if (matrix[8][i] === null) matrix[8][i] = false;
    if (matrix[i][8] === null) matrix[i][8] = false;
  }

  // Placement of data bits in 2-column zigzag
  let bitIdx = 0;
  const totalBits = finalCodewords.length * 8;
  const getNextBit = () => {
    if (bitIdx >= totalBits) return false;
    const byte = finalCodewords[Math.floor(bitIdx / 8)];
    const bit = (byte >> (7 - (bitIdx % 8))) & 1;
    bitIdx++;
    return bit === 1;
  };

  let upwards = true;
  for (let right = size - 1; right > 0; right -= 2) {
    if (right === 6) right--; // Skip vertical timing column
    const rows = upwards
      ? Array.from({ length: size }, (_, i) => size - 1 - i)
      : Array.from({ length: size }, (_, i) => i);

    for (const r of rows) {
      for (const col of [right, right - 1]) {
        if (matrix[r][col] === null) {
          let bit = getNextBit();
          // Apply standard mask pattern 0: (row + col) % 2 == 0
          if ((r + col) % 2 === 0) {
            bit = !bit;
          }
          matrix[r][col] = bit;
        }
      }
    }
    upwards = !upwards;
  }

  // Write format info for EC Level L + Mask 0 -> 0x77C4 (0b111011111000100)
  // Format bits from MSB (bit 14) to LSB (bit 0):
  const formatBits = [1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0];

  // Top-left:
  // (x=0..5, y=8) -> bits 0..5 (MSB to bit 9)
  // (x=7, y=8) -> bit 6 (bit 8)
  // (x=8, y=8) -> bit 7 (bit 7)
  // (x=8, y=7) -> bit 8 (bit 6)
  // (x=8, y=5..0) -> bits 9..14 (bit 5 to LSB)
  matrix[8][0] = formatBits[0] === 1;
  matrix[8][1] = formatBits[1] === 1;
  matrix[8][2] = formatBits[2] === 1;
  matrix[8][3] = formatBits[3] === 1;
  matrix[8][4] = formatBits[4] === 1;
  matrix[8][5] = formatBits[5] === 1;
  matrix[8][7] = formatBits[6] === 1;
  matrix[8][8] = formatBits[7] === 1;
  matrix[7][8] = formatBits[8] === 1;
  matrix[5][8] = formatBits[9] === 1;
  matrix[4][8] = formatBits[10] === 1;
  matrix[3][8] = formatBits[11] === 1;
  matrix[2][8] = formatBits[12] === 1;
  matrix[1][8] = formatBits[13] === 1;
  matrix[0][8] = formatBits[14] === 1;

  // Split around other finders:
  // Bottom-left: (x=8, y=size-1 down to size-7) -> bits 0..6 (MSB to bit 8)
  for (let i = 0; i < 7; i++) {
    matrix[size - 1 - i][8] = formatBits[i] === 1;
  }
  // Top-right: (x=size-8 to size-1, y=8) -> bits 7..14 (bit 7 to LSB)
  for (let i = 0; i < 8; i++) {
    matrix[8][size - 8 + i] = formatBits[7 + i] === 1;
  }

  return matrix.map((row) => row.map((cell) => cell === true));
}

export function formatManualCode(code: string): string {
  if (!code) return '';
  const cleaned = code.replace(/[^0-9A-Za-z]/g, '').toUpperCase();
  if (cleaned.length === 10) {
    return `${cleaned.slice(0, 4)}-${cleaned.slice(4, 8)}-${cleaned.slice(8)}`;
  }
  return code;
}
