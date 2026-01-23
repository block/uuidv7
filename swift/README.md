# UUIDv7 - Swift

A minimal, high-performance UUID v7 implementation for Swift.

## Introduction

[UUID v7](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7) is a time-ordered UUID format that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time. This is useful for:

- Database indexed fields that benefit from sequential ordering
- Distributed systems where time-based ordering is valuable
- Event logs and audit trails where chronological sorting is important

This library provides a lightweight implementation that works seamlessly with Swift's standard `UUID` type.

### Compact Base62 Format

**Recommended for APIs, databases, and anywhere IDs are stored or transmitted as text.**

This implementation provides 22-character Base62 compact strings using `0-9A-Za-z` that are:
- **39% shorter** than standard UUID strings (22 vs 36 characters)
- **Lexicographically sortable** - preserves time-based ordering in databases and APIs
- **URL-safe** - no special characters, hyphens, or encoding needed
- **Database-friendly** - shorter indexed strings mean better query performance

**Example**: `01JDQYZ9M6K7TCJK2F3W8N` (compact) vs `01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f` (standard)

## Design Principles

**Minimal API Surface**: Static utility methods and extensions that work with Swift's `UUID` rather than introducing a new type. This ensures maximum compatibility with existing code.

**Separate Types for Different Use Cases**: Two distinct implementations for different performance/ordering trade-offs:
- **`UUIDv7`**: Uses non-cryptographic random with zero synchronization overhead for maximum performance. UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness is maintained through random bits.
- **`MonotonicUUIDv7`**: Uses a synchronized counter to ensure strict ordering within the same millisecond, following RFC 9562 recommendations. Best for database primary keys and scenarios requiring guaranteed sequential ordering.

**Timestamp Extraction**: UUIDs contain timing information, and this library makes it easy to extract this for debugging, observability, and time-based queries.

**Flexible Generation**: Static factories for common cases, configurable generators for testing or custom clock sources.

## Installation

### Swift Package Manager

Add to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/block/uuidv7.git", from: "1.0.0")
]
```

Or add via Xcode: File → Add Package Dependencies → Enter the repository URL.

## Usage

```swift
import UUIDv7

// Generate a UUID v7 (maximum performance, no ordering guarantees)
let uuid = UUIDv7.generate()

// Generate with monotonic ordering (for database primary keys)
let monotonicUuid = MonotonicUUIDv7.generate()

// Extract the timestamp (milliseconds since Unix epoch)
let timestamp = try uuid.timestamp

// Generate a compact string directly (22 characters, Base62)
let compactId = UUIDv7.generateCompactString()

// Convert UUID to compact string (preserves sort order)
let compact = uuid.compactString

// Convert compact string back to UUID
let fromCompact = try UUID(compactString: compact)

// Custom clock for testing (non-monotonic)
let testUuid = UUIDv7.generate(clock: { 1234567890000 })

// Custom clock with monotonic ordering
let generator = MonotonicUUIDv7()
let monotonicTestUuid = generator.generate(clock: { 1234567890000 })

// Access version and variant
print(uuid.version)  // 7
print(uuid.variant)  // 2
```

## Design Details

### Format

UUID v7 follows RFC 9562:
- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Counter or random bits (12 bits, called `rand_a`)
  - **Monotonic mode**: Sequential counter for strict ordering within the same millisecond
  - **Non-monotonic mode**: Random bits for maximum performance
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits, called `rand_b`)

### API

#### UUIDv7 (High Performance, No Ordering Guarantees)

**`UUIDv7.generate()`**: Generate a new UUID v7 using current system time. Uses non-cryptographic random with no synchronization for maximum performance. Returns `UUID`.

**`UUIDv7.generate(clock:)`**: Generate a new UUID v7 with a custom clock source (milliseconds since Unix epoch). Useful for testing or specialized use cases.

#### MonotonicUUIDv7 (Sequential Ordering Guaranteed)

**`MonotonicUUIDv7.generate()`**: Generate a new UUID v7 using current system time with monotonic ordering. Uses a synchronized counter to ensure strict ordering within the same millisecond. Returns `UUID`.

**`MonotonicUUIDv7.generate(clock:)`**: Generate a new UUID v7 with a custom clock source and monotonic ordering. Useful for testing monotonic behavior with controlled clock sources.

**`MonotonicUUIDv7.shared`**: Shared singleton instance for convenient access.

#### Utility Methods

**`UUIDv7.getTimestamp(_:)`**: Extract the millisecond timestamp from any UUID v7. Returns `UInt64`. Throws if not version 7.

**Compact String Methods**:
- **`UUIDv7.generateCompactString()`**: Generate a new UUID v7 and return it as a 22-character compact string
- **`UUIDv7.generateCompactString(clock:)`**: Generate with custom clock source, return as compact string
- **`UUIDv7.toCompactString(_:)`**: Convert any UUID to a 22-character Base62 compact string
- **`UUIDv7.fromCompactString(_:)`**: Convert a compact string back to UUID

**UUID Extensions**:
- `uuid.timestamp` - Extension property for idiomatic timestamp extraction (throws if not v7)
- `uuid.compactString` - Extension property for conversion to compact string format
- `uuid.version` - The version number of the UUID
- `uuid.variant` - The variant number of the UUID
- `UUID(compactString:)` - Initialize from compact string

### Implementation Details

**`MonotonicUUIDv7` class**:
- Uses a synchronized counter (NSLock) for strict ordering within the same millisecond
- Guarantees ordering: `uuid1 < uuid2` for sequential generation
- Can generate up to 4096 UUIDs per millisecond before blocking
- If counter overflows, waits for the next millisecond to maintain uniqueness
- Counter resets to a random value when the timestamp advances (unpredictability)
- Best for: Database primary keys, audit logs, any scenario requiring guaranteed ordering

**`UUIDv7` enum**:
- Uses non-cryptographic random for the counter field with zero synchronization overhead
- Maximum performance with no blocking possible
- Uniqueness guaranteed by random bits, but ordering within a millisecond is not guaranteed
- Best for: High-throughput scenarios, distributed systems, logging, tracing

### Random Number Generation

This implementation uses Swift's default random (which is non-cryptographic on most platforms) for generating random bits:

- **Performance**: Standard random is significantly faster than cryptographic random
- **No Synchronization**: No lock contention in multi-threaded environments when using `UUIDv7`
- **Sufficient for UUIDs**: Cryptographic randomness is not required for UUID generation. The primary goals are uniqueness and unpredictability, not security
- **RFC 9562 Compliance**: The RFC does not mandate cryptographic randomness for UUID generation

## Platform Support

- macOS 12+
- iOS 15+
- tvOS 15+
- watchOS 8+

## Project Resources

| Resource                                   | Description                                                                    |
| ------------------------------------------ | ------------------------------------------------------------------------------ |
| [CODEOWNERS](../CODEOWNERS)                | Outlines the project lead(s)                                                   |
| [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md)| Expected behavior for project contributors, promoting a welcoming environment |
| [CONTRIBUTING.md](../CONTRIBUTING.md)      | Developer guide to build, test, run, access CI, chat, discuss, file issues     |
| [GOVERNANCE.md](../GOVERNANCE.md)          | Project governance                                                             |
| [LICENSE](../LICENSE)                      | Apache License, Version 2.0                                                    |
