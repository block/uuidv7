/**
 * Utility functions for generating and working with UUID v7 identifiers.
 *
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562.
 *
 * This module uses Math.random() for maximum performance with no synchronization overhead.
 * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
 * is guaranteed through random bits. For monotonic ordering guarantees, use
 * the monotonic module instead.
 */

import { toCompactString } from './base62';

export type Clock = () => number;

const HEX_CHARS = '0123456789abcdef';

function randomInt(max: number): number {
  return Math.floor(Math.random() * max);
}

function randomBigInt(bits: number): bigint {
  let result = 0n;
  const chunks = Math.ceil(bits / 32);
  for (let i = 0; i < chunks; i++) {
    result = (result << 32n) | BigInt(Math.floor(Math.random() * 0x100000000));
  }
  const mask = (1n << BigInt(bits)) - 1n;
  return result & mask;
}

function bytesToHex(bytes: Uint8Array): string {
  let hex = '';
  for (let i = 0; i < bytes.length; i++) {
    hex += HEX_CHARS[bytes[i] >> 4] + HEX_CHARS[bytes[i] & 0xf];
  }
  return hex;
}

function formatUUID(bytes: Uint8Array): string {
  return (
    bytesToHex(bytes.subarray(0, 4)) +
    '-' +
    bytesToHex(bytes.subarray(4, 6)) +
    '-' +
    bytesToHex(bytes.subarray(6, 8)) +
    '-' +
    bytesToHex(bytes.subarray(8, 10)) +
    '-' +
    bytesToHex(bytes.subarray(10, 16))
  );
}

/**
 * Builds a UUID v7 from timestamp and random components.
 * Shared by both standard and monotonic generators.
 *
 * @param timestamp - the timestamp in milliseconds since Unix epoch
 * @param randA - the random or counter value for bits 52-63 (12 bits)
 * @param randB - the random value for bits 66-127 (62 bits, variant will be set)
 * @returns a UUID v7 string in standard format
 */
export function build(timestamp: number, randA: number, randB: bigint): string {
  const bytes = new Uint8Array(16);

  bytes[0] = (timestamp / 0x10000000000) & 0xff;
  bytes[1] = (timestamp / 0x100000000) & 0xff;
  bytes[2] = (timestamp / 0x1000000) & 0xff;
  bytes[3] = (timestamp / 0x10000) & 0xff;
  bytes[4] = (timestamp / 0x100) & 0xff;
  bytes[5] = timestamp & 0xff;

  bytes[6] = 0x70 | ((randA >> 8) & 0x0f);
  bytes[7] = randA & 0xff;

  const randBWithVariant = randB | (0b10n << 62n);

  bytes[8] = Number((randBWithVariant >> 56n) & 0xffn);
  bytes[9] = Number((randBWithVariant >> 48n) & 0xffn);
  bytes[10] = Number((randBWithVariant >> 40n) & 0xffn);
  bytes[11] = Number((randBWithVariant >> 32n) & 0xffn);
  bytes[12] = Number((randBWithVariant >> 24n) & 0xffn);
  bytes[13] = Number((randBWithVariant >> 16n) & 0xffn);
  bytes[14] = Number((randBWithVariant >> 8n) & 0xffn);
  bytes[15] = Number(randBWithVariant & 0xffn);

  return formatUUID(bytes);
}

/**
 * Generates a new UUID v7 using the current system time.
 *
 * Uses Math.random() for maximum performance with no synchronization overhead.
 * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
 * is guaranteed through random bits.
 *
 * @param clock - optional custom clock function returning milliseconds since Unix epoch
 * @returns a new UUID v7 string
 */
export function generate(clock: Clock = Date.now): string {
  const timestamp = clock();
  const randA = randomInt(4096);
  const randB = randomBigInt(62);

  return build(timestamp, randA, randB);
}

/**
 * Generates a new UUID v7 as a compact string using the current system time.
 *
 * Equivalent to calling toCompactString(generate()).
 * Returns a 22-character Base62 encoded string that preserves time-ordering.
 *
 * @param clock - optional custom clock function returning milliseconds since Unix epoch
 * @returns a 22-character compact string representation of a new UUID v7
 */
export function generateCompactString(clock: Clock = Date.now): string {
  return toCompactString(generate(clock));
}

/**
 * Extracts the timestamp component from a UUID v7.
 *
 * @param uuid - the UUID v7 string to extract the timestamp from
 * @returns the timestamp in milliseconds since Unix epoch
 * @throws Error if uuid is null or not a v7 UUID
 */
export function getTimestamp(uuid: string): number {
  if (!uuid) {
    throw new Error('UUID cannot be null or empty');
  }

  const cleaned = uuid.replace(/-/g, '');
  if (cleaned.length !== 32) {
    throw new Error('Invalid UUID format');
  }

  const version = parseInt(cleaned[12], 16);
  if (version !== 7) {
    throw new Error(`UUID is not version 7 (got version ${version})`);
  }

  const timestampHex = cleaned.substring(0, 12);
  return parseInt(timestampHex, 16);
}

/**
 * Validates whether a string is a valid UUID v7.
 *
 * @param uuid - the string to validate
 * @returns true if the string is a valid UUID v7
 */
export function isValidUUIDv7(uuid: string): boolean {
  if (!uuid) return false;

  const cleaned = uuid.replace(/-/g, '');
  if (cleaned.length !== 32) return false;
  if (!/^[0-9a-fA-F]+$/.test(cleaned)) return false;

  const version = parseInt(cleaned[12], 16);
  if (version !== 7) return false;

  const variant = parseInt(cleaned[16], 16);
  return (variant & 0xc) === 0x8;
}
