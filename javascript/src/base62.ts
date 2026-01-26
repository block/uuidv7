/**
 * Base62 encoding/decoding utilities for UUID compact string format.
 *
 * The compact string format uses Base62 encoding (0-9, A-Z, a-z) producing
 * URL-safe, 22-character strings that preserve lexicographic sort order
 * for time-ordered UUIDs.
 *
 * This is 39% shorter than standard UUID string format (22 vs 36 characters).
 */

const BASE62_ALPHABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
const BASE62_LENGTH = 22;
const BASE = 62n;

const CHAR_TO_VALUE = new Map<string, number>();
for (let i = 0; i < BASE62_ALPHABET.length; i++) {
  CHAR_TO_VALUE.set(BASE62_ALPHABET[i], i);
}

/**
 * Converts a UUID string to bytes.
 */
function uuidToBytes(uuid: string): Uint8Array {
  const hex = uuid.replace(/-/g, '');
  if (hex.length !== 32) {
    throw new Error('Invalid UUID format');
  }

  const bytes = new Uint8Array(16);
  for (let i = 0; i < 16; i++) {
    bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

/**
 * Converts bytes to a UUID string.
 */
function bytesToUuid(bytes: Uint8Array): string {
  const hex = Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');

  return (
    hex.substring(0, 8) +
    '-' +
    hex.substring(8, 12) +
    '-' +
    hex.substring(12, 16) +
    '-' +
    hex.substring(16, 20) +
    '-' +
    hex.substring(20, 32)
  );
}

/**
 * Converts a UUID to a compact string representation.
 *
 * The resulting string is exactly 22 characters long and preserves lexicographic
 * ordering for UUID v7 values (time-ordered UUIDs will sort correctly as compact strings).
 * Uses Base62 encoding (0-9, A-Z, a-z).
 *
 * @param uuid - the UUID to encode (standard format with hyphens)
 * @returns a 22-character compact string representation
 * @throws Error if uuid is null or invalid format
 */
export function toCompactString(uuid: string): string {
  if (!uuid) {
    throw new Error('UUID cannot be null or empty');
  }

  const bytes = uuidToBytes(uuid);

  let value = 0n;
  for (let i = 0; i < 16; i++) {
    value = (value << 8n) | BigInt(bytes[i]);
  }

  let result = '';
  while (value > 0n) {
    const remainder = Number(value % BASE);
    result = BASE62_ALPHABET[remainder] + result;
    value = value / BASE;
  }

  return result.padStart(BASE62_LENGTH, '0');
}

/**
 * Decodes a compact string representation back to a UUID.
 *
 * @param compactString - the compact string to decode (must be 22 characters)
 * @returns the decoded UUID in standard format
 * @throws Error if compactString is null, not 22 characters, or contains invalid characters
 */
export function fromCompactString(compactString: string): string {
  if (!compactString) {
    throw new Error('Compact string cannot be null or empty');
  }

  if (compactString.length !== BASE62_LENGTH) {
    throw new Error(
      `Compact string must be exactly ${BASE62_LENGTH} characters (got ${compactString.length})`
    );
  }

  let value = 0n;
  for (let i = 0; i < compactString.length; i++) {
    const char = compactString[i];
    const digit = CHAR_TO_VALUE.get(char);
    if (digit === undefined) {
      throw new Error(`Invalid compact string character: ${char}`);
    }
    value = value * BASE + BigInt(digit);
  }

  const bytes = new Uint8Array(16);
  for (let i = 15; i >= 0; i--) {
    bytes[i] = Number(value & 0xffn);
    value = value >> 8n;
  }

  return bytesToUuid(bytes);
}
