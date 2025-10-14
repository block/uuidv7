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

**Stateless & Non-Monotonic**: No thread-local counters or synchronization overhead. Each UUID is independently generated. While this means UUIDs generated in the same millisecond may not be strictly ordered, it provides better performance and simplicity for distributed systems where cross-server ordering guarantees are impossible anyway.

**Timestamp Extraction**: UUIDs contain timing information, and VeeSeven makes it easy to extract this for debugging, observability, and time-based queries.

**Flexible Generation**: Static factory for common cases, configurable generators for testing or custom clock sources.

## Usage

### Java

```java
import xyz.block.veeseven.UuidV7;
import java.util.UUID;

// Generate a UUID v7
UUID uuid = UuidV7.generate();

// Extract the timestamp (milliseconds since Unix epoch)
long timestamp = UuidV7.getTimestamp(uuid);

// Custom clock for testing
UUID testUuid = UuidV7.generate(() -> 1234567890000L);
```

### Kotlin

```kotlin
import xyz.block.veeseven.UuidV7
import xyz.block.veeseven.timestamp

// Generate a UUID v7
val uuid = UuidV7.generate()

// Extract timestamp with extension property
val timestamp = uuid.timestamp

// Custom clock for testing
val testUuid = UuidV7.generate { 1234567890000L }
```

## Design Details

### Format

UUID v7 follows RFC 9562:
- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Random bits
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits)

### API

**`UuidV7.generate()`**: Generate a new UUID v7 with current system time and random bits. Returns `java.util.UUID`.

**`UuidV7.generate(LongSupplier clock)`**: Generate a new UUID v7 with a custom clock source (milliseconds since Unix epoch). Useful for testing or specialized use cases.

**`UuidV7.getTimestamp(UUID)`**: Extract the millisecond timestamp from a UUID v7. Returns `long`.

**Kotlin Extensions**: `UUID.timestamp` extension property for idiomatic timestamp extraction.

## Project Resources

| Resource                                   | Description                                                                    |
| ------------------------------------------ | ------------------------------------------------------------------------------ |
| [CODEOWNERS](./CODEOWNERS)                 | Outlines the project lead(s)                                                   |
| [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) | Expected behavior for project contributors, promoting a welcoming environment |
| [CONTRIBUTING.md](./CONTRIBUTING.md)       | Developer guide to build, test, run, access CI, chat, discuss, file issues     |
| [GOVERNANCE.md](./GOVERNANCE.md)           | Project governance                                                             |
| [LICENSE](./LICENSE)                       | Apache License, Version 2.0                                                    |
