import { describe, it, expect, beforeEach } from 'vitest';
import { UUIDv7, MonotonicUUIDv7, toCompactString, fromCompactString } from '../src/index';

describe('UUIDv7', () => {
  describe('generate', () => {
    it('should generate a valid UUID v7', () => {
      const uuid = UUIDv7.generate();
      expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    });

    it('should generate unique UUIDs', () => {
      const uuids = new Set<string>();
      for (let i = 0; i < 1000; i++) {
        uuids.add(UUIDv7.generate());
      }
      expect(uuids.size).toBe(1000);
    });

    it('should use custom clock', () => {
      const fixedTime = 1704067200000;
      const uuid = UUIDv7.generate(() => fixedTime);

      const timestamp = UUIDv7.getTimestamp(uuid);
      expect(timestamp).toBe(fixedTime);
    });

    it('should have version 7', () => {
      const uuid = UUIDv7.generate();
      const version = parseInt(uuid.split('-')[2][0], 16);
      expect(version).toBe(7);
    });

    it('should have correct variant', () => {
      const uuid = UUIDv7.generate();
      const variant = parseInt(uuid.split('-')[3][0], 16);
      expect(variant & 0xc).toBe(0x8);
    });
  });

  describe('getTimestamp', () => {
    it('should extract timestamp correctly', () => {
      const now = Date.now();
      const uuid = UUIDv7.generate(() => now);

      const extracted = UUIDv7.getTimestamp(uuid);
      expect(extracted).toBe(now);
    });

    it('should throw for null UUID', () => {
      expect(() => UUIDv7.getTimestamp('')).toThrow('UUID cannot be null or empty');
    });

    it('should throw for non-v7 UUID', () => {
      const v4uuid = '550e8400-e29b-41d4-a716-446655440000';
      expect(() => UUIDv7.getTimestamp(v4uuid)).toThrow('UUID is not version 7');
    });

    it('should throw for invalid format', () => {
      expect(() => UUIDv7.getTimestamp('not-a-uuid')).toThrow('Invalid UUID format');
    });
  });

  describe('isValid', () => {
    it('should return true for valid UUID v7', () => {
      const uuid = UUIDv7.generate();
      expect(UUIDv7.isValid(uuid)).toBe(true);
    });

    it('should return false for null', () => {
      expect(UUIDv7.isValid('')).toBe(false);
    });

    it('should return false for v4 UUID', () => {
      const v4uuid = '550e8400-e29b-41d4-a716-446655440000';
      expect(UUIDv7.isValid(v4uuid)).toBe(false);
    });

    it('should return false for invalid string', () => {
      expect(UUIDv7.isValid('not-a-uuid')).toBe(false);
    });
  });

  describe('compact string conversion', () => {
    it('should convert to compact string', () => {
      const uuid = UUIDv7.generate();
      const compact = UUIDv7.toCompactString(uuid);

      expect(compact.length).toBe(22);
      expect(compact).toMatch(/^[0-9A-Za-z]+$/);
    });

    it('should roundtrip correctly', () => {
      const original = UUIDv7.generate();
      const compact = UUIDv7.toCompactString(original);
      const restored = UUIDv7.fromCompactString(compact);

      expect(restored).toBe(original);
    });

    it('should preserve sort order', () => {
      const timestamps = [1000000, 2000000, 3000000, 4000000, 5000000];
      const compacts: string[] = [];

      for (const ts of timestamps) {
        const uuid = UUIDv7.generate(() => ts);
        compacts.push(UUIDv7.toCompactString(uuid));
      }

      const sorted = [...compacts].sort();
      expect(compacts).toEqual(sorted);
    });

    it('should throw for null UUID', () => {
      expect(() => UUIDv7.toCompactString('')).toThrow('UUID cannot be null or empty');
    });

    it('should throw for invalid compact string length', () => {
      expect(() => UUIDv7.fromCompactString('abc')).toThrow('must be exactly 22 characters');
    });

    it('should throw for invalid compact string characters', () => {
      expect(() => UUIDv7.fromCompactString('!@#$%^&*()_+{}[]|":;~<')).toThrow('Invalid compact string character');
    });
  });

  describe('generateCompactString', () => {
    it('should generate a valid compact string directly', () => {
      const compact = UUIDv7.generateCompactString();

      expect(compact.length).toBe(22);
      expect(compact).toMatch(/^[0-9A-Za-z]+$/);

      const uuid = UUIDv7.fromCompactString(compact);
      expect(UUIDv7.isValid(uuid)).toBe(true);
    });
  });
});

describe('MonotonicUUIDv7', () => {
  beforeEach(() => {
    MonotonicUUIDv7.resetState();
  });

  describe('generate', () => {
    it('should generate a valid UUID v7', () => {
      const uuid = MonotonicUUIDv7.generate();
      expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    });

    it('should generate unique UUIDs', () => {
      const uuids = new Set<string>();
      for (let i = 0; i < 1000; i++) {
        uuids.add(MonotonicUUIDv7.generate());
      }
      expect(uuids.size).toBe(1000);
    });

    it('should maintain strict ordering within same millisecond', () => {
      const fixedTime = 1704067200000;
      const uuids: string[] = [];

      for (let i = 0; i < 100; i++) {
        uuids.push(MonotonicUUIDv7.generate(() => fixedTime));
      }

      const sorted = [...uuids].sort();
      expect(uuids).toEqual(sorted);
    });

    it('should handle backward clock and maintain ordering', () => {
      const highTime = 2000000000000;
      const lowTime = 1000000000000;

      // Generate at a high timestamp
      const uuid1 = MonotonicUUIDv7.generate(() => highTime);
      const uuid2 = MonotonicUUIDv7.generate(() => highTime);

      // Move clock backward
      const uuid3 = MonotonicUUIDv7.generate(() => lowTime);

      // Timestamp should be clamped
      expect(UUIDv7.getTimestamp(uuid3)).toBeGreaterThanOrEqual(UUIDv7.getTimestamp(uuid1));

      // Monotonic ordering must be maintained
      expect(uuid1.localeCompare(uuid2)).toBeLessThan(0);
      expect(uuid2.localeCompare(uuid3)).toBeLessThan(0);
    });

    it('should increment counter within same millisecond', () => {
      const fixedTime = 1704067200000;
      const uuid1 = MonotonicUUIDv7.generate(() => fixedTime);
      const uuid2 = MonotonicUUIDv7.generate(() => fixedTime);

      expect(uuid1).not.toBe(uuid2);
      expect(uuid1.localeCompare(uuid2)).toBeLessThan(0);
    });
  });

  describe('generateCompactString', () => {
    it('should generate a valid compact string', () => {
      const compact = MonotonicUUIDv7.generateCompactString();

      expect(compact.length).toBe(22);
      expect(compact).toMatch(/^[0-9A-Za-z]+$/);

      const uuid = fromCompactString(compact);
      expect(UUIDv7.isValid(uuid)).toBe(true);
    });

    it('should maintain sort order within same millisecond', () => {
      const fixedTime = 1704067200000;
      const compacts: string[] = [];

      for (let i = 0; i < 50; i++) {
        compacts.push(MonotonicUUIDv7.generateCompactString(() => fixedTime));
      }

      const sorted = [...compacts].sort();
      expect(compacts).toEqual(sorted);
    });
  });
});

describe('Base62', () => {
  describe('toCompactString / fromCompactString', () => {
    it('should handle min UUID', () => {
      const minUuid = '00000000-0000-7000-8000-000000000000';
      const compact = toCompactString(minUuid);
      const restored = fromCompactString(compact);
      expect(restored).toBe(minUuid);
    });

    it('should handle max UUID', () => {
      const maxUuid = 'ffffffff-ffff-7fff-bfff-ffffffffffff';
      const compact = toCompactString(maxUuid);
      const restored = fromCompactString(compact);
      expect(restored).toBe(maxUuid);
    });

    it('should produce lexicographically sortable strings', () => {
      const uuid1 = '01936c0a-0000-7000-8000-000000000000';
      const uuid2 = '01936c0a-0001-7000-8000-000000000000';
      const uuid3 = '01936c0b-0000-7000-8000-000000000000';

      const compact1 = toCompactString(uuid1);
      const compact2 = toCompactString(uuid2);
      const compact3 = toCompactString(uuid3);

      expect(compact1 < compact2).toBe(true);
      expect(compact2 < compact3).toBe(true);
    });
  });
});
