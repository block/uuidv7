/**
 * Utility functions for generating monotonic UUID v7 identifiers.
 *
 * This implementation ensures that UUIDs generated within the same millisecond are strictly
 * ordered by incrementing a counter. This provides guaranteed sequential ordering, making it
 * ideal for database primary keys and scenarios requiring chronological order guarantees.
 *
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562 with monotonic counter support.
 *
 * Note: In JavaScript's single-threaded environment, synchronization is simpler than in Java,
 * but the counter logic remains the same. If the counter overflows within a millisecond
 * (after 4096 UUIDs), the method will wait until the next millisecond.
 */

import { build, type Clock } from './uuidv7';
import { toCompactString } from './base62';

const COUNTER_MAX = 0xfff;

let lastTimestamp = 0;
let counter = 0;

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

function cryptoRandomInt(max: number): number {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint32Array(1);
    crypto.getRandomValues(array);
    return array[0] % max;
  }
  return randomInt(max);
}

/**
 * Generates a new monotonic UUID v7 using the current system time.
 *
 * This function ensures that UUIDs generated within the same millisecond are strictly
 * ordered by incrementing a counter. If the counter overflows within a millisecond,
 * the function will wait until the next millisecond to maintain uniqueness.
 *
 * Best suited for database primary keys and scenarios requiring guaranteed sequential ordering.
 *
 * @param clock - optional custom clock function returning milliseconds since Unix epoch
 * @returns a new UUID v7 string
 */
export function generate(clock: Clock = Date.now): string {
  let timestamp = clock();
  let counterValue: number;

  if (timestamp === lastTimestamp) {
    counter = (counter + 1) & COUNTER_MAX;

    if (counter === 0) {
      while (timestamp === lastTimestamp) {
        timestamp = clock();
      }
      counter = cryptoRandomInt(COUNTER_MAX + 1);
    }
    counterValue = counter;
  } else {
    counter = cryptoRandomInt(COUNTER_MAX + 1);
    counterValue = counter;
    lastTimestamp = timestamp;
  }

  const randB = randomBigInt(62);
  return build(timestamp, counterValue, randB);
}

/**
 * Generates a new monotonic UUID v7 as a compact string using the current system time.
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
 * Resets the monotonic state. Useful for testing.
 * Should not be used in production.
 */
export function resetState(): void {
  lastTimestamp = 0;
  counter = 0;
}
