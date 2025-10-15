# VeeSeven

A minimal, high-performance UUID v7 implementation for Java with excellent Kotlin bindings.

## Introduction

[UUID v7](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7) is a time-ordered UUID format that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time. This is useful for:

- Database indexed fields that benefit from sequential ordering
- Distributed systems where time-based ordering is valuable
- Event logs and audit trails where chronological sorting is important

VeeSeven provides a lightweight implementation that works seamlessly with Java's standard `java.util.UUID` class.

## Design Principles

**Minimal API Surface**: Static utility methods that work with `java.util.UUID` rather than introducing a new type. This ensures maximum compatibility with existing code.

**Dual-Mode Generation**: Separate methods for different performance/ordering trade-offs:
- **`generate()` - Non-Monotonic (default)**: Uses `ThreadLocalRandom` with zero synchronization overhead for maximum performance. UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness is maintained through random bits. Ideal for high-throughput scenarios and distributed systems where cross-server ordering guarantees are impossible.
- **`generateMonotonic()` - Monotonic ordering**: Uses a synchronized counter to ensure strict ordering within the same millisecond, following RFC 9562 recommendations. Best for database primary keys and scenarios requiring guaranteed sequential ordering.

**Timestamp Extraction**: UUIDs contain timing information, and VeeSeven makes it easy to extract this for debugging, observability, and time-based queries.

**Flexible Generation**: Static factory for common cases, configurable generators for testing or custom clock sources.

## Usage

### Java

```java
import xyz.block.veeseven.UuidV7;
import java.util.UUID;

// Generate a UUID v7 (non-monotonic, maximum performance)
UUID uuid = UuidV7.generate();

// Generate with monotonic ordering (for database primary keys)
UUID monotonicUuid = UuidV7.generateMonotonic();

// Extract the timestamp (milliseconds since Unix epoch)
long timestamp = UuidV7.getTimestamp(uuid);

// Custom clock for testing (non-monotonic)
UUID testUuid = UuidV7.generate(() -> 1234567890000L);

// Custom clock with monotonic ordering
UUID monotonicTestUuid = UuidV7.generateMonotonic(() -> 1234567890000L);
```

### Kotlin

```kotlin
import xyz.block.veeseven.UuidV7
import xyz.block.veeseven.timestamp

// Generate a UUID v7 (non-monotonic, maximum performance)
val uuid = UuidV7.generate()

// Generate with monotonic ordering (for database primary keys)
val monotonicUuid = UuidV7.generateMonotonic()

// Extract timestamp with extension property
val timestamp = uuid.timestamp

// Custom clock with monotonic ordering
val testUuid = UuidV7.generateMonotonic { 1234567890000L }
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

#### Non-Monotonic (High Performance)

**`UuidV7.generate()`**: Generate a new UUID v7 using current system time. Uses `ThreadLocalRandom` with no synchronization for maximum performance. Returns `java.util.UUID`.

**`UuidV7.generate(LongSupplier clock)`**: Generate a new UUID v7 with a custom clock source (milliseconds since Unix epoch). Useful for testing or specialized use cases.

#### Monotonic (Sequential Ordering)

**`UuidV7.generateMonotonic()`**: Generate a new UUID v7 using current system time with monotonic ordering. Uses a synchronized counter to ensure strict ordering within the same millisecond. Returns `java.util.UUID`.

**`UuidV7.generateMonotonic(LongSupplier clock)`**: Generate a new UUID v7 with a custom clock source and monotonic ordering. Useful for testing monotonic behavior with controlled clock sources.

#### Utility Methods

**`UuidV7.getTimestamp(UUID)`**: Extract the millisecond timestamp from a UUID v7. Returns `long`.

**Kotlin Extensions**: `UUID.timestamp` extension property for idiomatic timestamp extraction.

### Implementation Details

**`generateMonotonic()` methods**:
- Use a synchronized counter for strict ordering within the same millisecond
- Guarantee ordering: `uuid1.compareTo(uuid2) < 0` for sequential generation
- Can generate up to 4096 UUIDs per millisecond before blocking
- If counter overflows, waits for the next millisecond to maintain uniqueness
- Counter resets to a random value when the timestamp advances (unpredictability)
- Best for: Database primary keys, audit logs, any scenario requiring guaranteed ordering

**`generate()` methods**:
- Use `ThreadLocalRandom` for the counter field with zero synchronization overhead
- Maximum performance with no blocking possible
- Uniqueness guaranteed by random bits, but ordering within a millisecond is not guaranteed
- Best for: High-throughput scenarios, distributed systems, logging, tracing

### Random Number Generation

VeeSeven uses **`ThreadLocalRandom`** for generating random bits rather than `SecureRandom` for the following reasons:

- **Performance**: `ThreadLocalRandom` is significantly faster (10-100x) than `SecureRandom` as it avoids contention and doesn't require cryptographic operations
- **No Synchronization**: Thread-local design eliminates lock contention in multi-threaded environments
- **Sufficient for UUIDs**: Cryptographic randomness is not required for UUID generation. The primary goals are uniqueness and unpredictability, not security
- **RFC 9562 Compliance**: The RFC does not mandate cryptographic randomness for UUID generation

**Usage by method**:
- **`generate()` methods**: Use `ThreadLocalRandom` for all random bits (rand_a and rand_b) with zero synchronization overhead
- **`generateMonotonic()` methods**: Use `ThreadLocalRandom` for rand_b (62 bits) and `SecureRandom` only to initialize the counter value when the timestamp advances, providing unpredictability while maintaining performance

## Project Resources

| Resource                                   | Description                                                                    |
| ------------------------------------------ | ------------------------------------------------------------------------------ |
| [CODEOWNERS](./CODEOWNERS)                 | Outlines the project lead(s)                                                   |
| [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) | Expected behavior for project contributors, promoting a welcoming environment |
| [CONTRIBUTING.md](./CONTRIBUTING.md)       | Developer guide to build, test, run, access CI, chat, discuss, file issues     |
| [GOVERNANCE.md](./GOVERNANCE.md)           | Project governance                                                             |
| [LICENSE](./LICENSE)                       | Apache License, Version 2.0                                                    |
