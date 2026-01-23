# Agent Guide - JavaScript Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd javascript
npm install      # Install dependencies
npm run build    # Build project
npm test         # Run tests
npm run typecheck # Type checking
```

## Prerequisites

- Node.js 18 or later
- npm

## Project Structure

```
javascript/
├── src/
│   ├── index.ts        # Main entry point with exports
│   ├── uuidv7.ts       # High-performance UUID v7 (no ordering guarantees)
│   ├── monotonic.ts    # Monotonic UUID v7 (strict ordering)
│   └── base62.ts       # Base62 compact string encoding
├── test/
│   └── uuidv7.test.ts  # Comprehensive tests
├── dist/               # Build output (generated)
├── package.json
├── tsconfig.json
├── tsup.config.ts      # Build configuration
├── vitest.config.ts    # Test configuration
├── README.md
└── AGENTS.md           # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- **UUIDv7**: Uses `Math.random()` for all random bits (maximum performance)
- **MonotonicUUIDv7**: Uses `Math.random()` for rand_b, `crypto.getRandomValues()` for counter initialization when available
- **Rationale**: Cryptographic randomness not required for UUIDs; performance is priority

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), waits for next millisecond
- In JavaScript's single-threaded environment, no mutex needed

### BigInt Usage

- Uses `BigInt` for 128-bit UUID arithmetic
- Required for accurate Base62 encoding/decoding
- Ensures lexicographic sort order preservation

### Timestamp Extraction

- Timestamp is in most significant 48 bits
- Extracted from first 12 hex characters of UUID
- Returns milliseconds since Unix epoch
- Validates UUID is version 7 before extracting

### Shared Build Function

- `build(timestamp, randA, randB)`: Shared function for constructing UUID v7
- Used by both standard and monotonic generators
- Handles version (7) and variant (RFC 4122) bit setting

## Module Formats

The package supports both ESM and CommonJS:
- ESM: `dist/index.js`
- CJS: `dist/index.cjs`
- Types: `dist/index.d.ts` and `dist/index.d.cts`

## Testing

```bash
npm test              # Run all tests
npm run test:watch    # Watch mode
```

Tests cover:
- UUID generation and uniqueness
- Version and variant validation
- Timestamp extraction
- Monotonic ordering guarantees
- Compact string encoding/decoding
- Lexicographic sort order preservation

## Publishing

```bash
npm run build
npm publish
```

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [root AGENTS.md](../AGENTS.md) - Multi-language implementation principles
