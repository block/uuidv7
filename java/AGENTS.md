# Agent Guide - Java Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd java
./gradlew build    # Build project
./gradlew test     # Run tests
```

## Prerequisites

- Java 21 or later
- No need to install Gradle - the project uses Gradle Wrapper

## Project Structure

```
java/
├── src/
│   ├── main/
│   │   ├── java/xyz/block/uuidv7/
│   │   │   ├── UUIDv7.java              # High-performance UUID v7 (no ordering guarantees)
│   │   │   └── MonotonicUUIDv7.java     # Monotonic UUID v7 (strict ordering)
│   │   └── kotlin/xyz/block/uuidv7/
│   │       └── UuidV7Extensions.kt      # Kotlin extensions
│   └── test/
│       ├── java/xyz/block/uuidv7/
│       │   ├── UUIDv7Test.java          # Tests for UUIDv7
│       │   └── MonotonicUUIDv7Test.java # Tests for MonotonicUUIDv7
│       └── kotlin/xyz/block/uuidv7/
│           └── UUIDv7ExtensionsTest.kt  # Kotlin tests
├── build.gradle.kts
├── README.md
├── CONTRIBUTING.md
├── RELEASING.md
└── AGENTS.md  # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- **UUIDv7**: Uses `ThreadLocalRandom` for all random bits (maximum performance)
- **MonotonicUUIDv7**: Uses `ThreadLocalRandom` for rand_b (62 bits), `SecureRandom` only for counter initialization
- **Rationale**: Cryptographic randomness not required for UUIDs; performance is priority

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), method blocks/waits for next millisecond
- This ensures uniqueness while maintaining strict ordering

### Timestamp Extraction

- Timestamp is in most significant 48 bits
- Extract via: `uuid.getMostSignificantBits() >>> 16`
- Returns milliseconds since Unix epoch
- Validates UUID is version 7 before extracting

### Package-Private Methods

- `UUIDv7.build(long timestamp, int randA, long randB)`: Shared method for constructing UUID v7 from timestamp and random components (randA for bits 52-63, randB for bits 66-127), used by both UUIDv7 and MonotonicUUIDv7

## Maven Publishing

See [RELEASING.md](RELEASING.md) for instructions on publishing to Maven Central.

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [CONTRIBUTING.md](CONTRIBUTING.md) - Build instructions and contribution guidelines
- [RELEASING.md](RELEASING.md) - Release process for Maven Central
