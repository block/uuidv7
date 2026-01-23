# Agent Guide - Swift Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd swift
swift build    # Build project
swift test     # Run tests
```

## Prerequisites

- Swift 5.9 or later
- Xcode 15+ (for macOS development)

## Project Structure

```
swift/
├── Sources/
│   └── UUIDv7/
│       ├── UUIDv7.swift              # High-performance UUID v7 (no ordering guarantees)
│       ├── MonotonicUUIDv7.swift     # Monotonic UUID v7 (strict ordering)
│       └── UUID+UUIDv7.swift         # UUID extensions (version, variant, timestamp, compactString)
├── Tests/
│   └── UUIDv7Tests/
│       ├── UUIDv7Tests.swift         # Tests for UUIDv7
│       ├── MonotonicUUIDv7Tests.swift # Tests for MonotonicUUIDv7
│       └── CompactStringTests.swift   # Tests for Base62 compact string encoding
├── Package.swift
├── README.md
└── AGENTS.md  # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- **UUIDv7**: Uses Swift's default random (`UInt16.random`, `UInt64.random`) for all random bits (maximum performance)
- **MonotonicUUIDv7**: Uses Swift's default random for rand_b (62 bits) and counter initialization
- **Rationale**: Cryptographic randomness not required for UUIDs; performance is priority

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), method blocks/waits for next millisecond
- Uses NSLock for thread safety
- This ensures uniqueness while maintaining strict ordering

### Timestamp Extraction

- Timestamp is in most significant 48 bits
- Extract via: `mostSigBits >> 16`
- Returns milliseconds since Unix epoch
- Validates UUID is version 7 before extracting (throws `UUIDv7Error.notVersion7` otherwise)

### Internal Methods

- `UUIDv7.build(timestamp:randA:randB:)`: Shared method for constructing UUID v7 from timestamp and random components, used by both UUIDv7 and MonotonicUUIDv7
- `UUIDv7.bitsFromUUID(_:)`: Extract most/least significant bits from UUID
- `UUIDv7.uuidFromBits(mostSigBits:leastSigBits:)`: Create UUID from bit components

### UUID Extensions

The library extends `Foundation.UUID` with:
- `version`: The 4-bit version field (7 for UUID v7)
- `variant`: The variant field (2 for RFC 4122)
- `timestamp`: The millisecond timestamp (throws if not v7)
- `compactString`: 22-character Base62 encoding
- `Comparable` conformance for sorting

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [Root AGENTS.md](../AGENTS.md) - Multi-language overview and shared principles
