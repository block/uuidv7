/**
 * @block/uuidv7 - A minimal, high-performance UUID v7 implementation for JavaScript/TypeScript
 *
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562.
 *
 * @example
 * ```typescript
 * import { UUIDv7, MonotonicUUIDv7 } from '@block/uuidv7';
 *
 * // Generate a UUID v7 (maximum performance, no ordering guarantees)
 * const uuid = UUIDv7.generate();
 *
 * // Generate with monotonic ordering (for database primary keys)
 * const monotonicUuid = MonotonicUUIDv7.generate();
 *
 * // Generate a compact string directly (22 characters, Base62)
 * const compactId = UUIDv7.generateCompactString();
 *
 * // Convert UUID to compact string (preserves sort order)
 * const compact = UUIDv7.toCompactString(uuid);
 *
 * // Convert compact string back to UUID
 * const fromCompact = UUIDv7.fromCompactString(compact);
 *
 * // Extract the timestamp
 * const timestamp = UUIDv7.getTimestamp(uuid);
 * ```
 */

import * as uuidv7 from './uuidv7';
import * as monotonic from './monotonic';
import * as base62 from './base62';

export type { Clock } from './uuidv7';

/**
 * High-performance UUID v7 generator.
 *
 * Uses Math.random() for all random bits with zero synchronization overhead.
 * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
 * is guaranteed through random bits.
 *
 * Best for: High-throughput scenarios, distributed systems, logging, tracing.
 */
export const UUIDv7 = {
  /**
   * Generates a new UUID v7 using the current system time.
   *
   * @param clock - optional custom clock function returning milliseconds since Unix epoch
   * @returns a new UUID v7 string
   */
  generate: uuidv7.generate,

  /**
   * Generates a new UUID v7 as a compact string.
   *
   * @param clock - optional custom clock function returning milliseconds since Unix epoch
   * @returns a 22-character compact string representation of a new UUID v7
   */
  generateCompactString: uuidv7.generateCompactString,

  /**
   * Extracts the timestamp component from a UUID v7.
   *
   * @param uuid - the UUID v7 string to extract the timestamp from
   * @returns the timestamp in milliseconds since Unix epoch
   * @throws Error if uuid is null or not a v7 UUID
   */
  getTimestamp: uuidv7.getTimestamp,

  /**
   * Validates whether a string is a valid UUID v7.
   *
   * @param uuid - the string to validate
   * @returns true if the string is a valid UUID v7
   */
  isValid: uuidv7.isValidUUIDv7,

  /**
   * Converts a UUID to a compact string representation.
   *
   * @param uuid - the UUID to encode (standard format with hyphens)
   * @returns a 22-character compact string representation
   */
  toCompactString: base62.toCompactString,

  /**
   * Decodes a compact string representation back to a UUID.
   *
   * @param compactString - the compact string to decode (must be 22 characters)
   * @returns the decoded UUID in standard format
   */
  fromCompactString: base62.fromCompactString,
};

/**
 * Monotonic UUID v7 generator with guaranteed sequential ordering.
 *
 * Uses a counter to ensure strict ordering within the same millisecond.
 * If the counter overflows (after 4096 UUIDs in one millisecond), the method
 * will wait until the next millisecond to maintain uniqueness.
 *
 * Best for: Database primary keys, audit logs, any scenario requiring guaranteed ordering.
 */
export const MonotonicUUIDv7 = {
  /**
   * Generates a new monotonic UUID v7 using the current system time.
   *
   * This function ensures that UUIDs generated within the same millisecond are strictly
   * ordered by incrementing a counter.
   *
   * @param clock - optional custom clock function returning milliseconds since Unix epoch
   * @returns a new UUID v7 string
   */
  generate: monotonic.generate,

  /**
   * Generates a new monotonic UUID v7 as a compact string.
   *
   * @param clock - optional custom clock function returning milliseconds since Unix epoch
   * @returns a 22-character compact string representation of a new UUID v7
   */
  generateCompactString: monotonic.generateCompactString,

  /**
   * Resets the monotonic state. Useful for testing.
   * Should not be used in production.
   */
  resetState: monotonic.resetState,
};

export { toCompactString, fromCompactString } from './base62';
export { getTimestamp, isValidUUIDv7, build } from './uuidv7';
export { generate as generateMonotonic, generateCompactString as generateMonotonicCompactString } from './monotonic';
