# UUIDv7 Implementation

## Project Overview

A minimal, high-performance UUID v7 implementation for Java with excellent Kotlin bindings. UUID v7 is a time-ordered UUID format defined in RFC 9562 that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time.

**Group**: `xyz.block`
**Version**: `0.1.0-SNAPSHOT`
**License**: Apache License 2.0

## Technology Stack

- **Language**: Java 21 (primary), Kotlin 1.9.22 (extensions)
- **Build System**: Gradle 8.x (via Gradle Wrapper)
- **Testing**: JUnit Jupiter 5.10.1, AssertJ 3.24.2
- **Target**: Java/Kotlin libraries and applications

## Project Structure

```
/Users/mrohan/Development/uuidv7/
├── src/
│   ├── main/
│   │   ├── java/xyz/block/uuidv7/
│   │   │   └── UuidV7.java              # Core UUID v7 implementation
│   │   └── kotlin/xyz/block/uuidv7/
│   │       └── UuidV7Extensions.kt      # Kotlin extension property for timestamp
│   └── test/
│       ├── java/xyz/block/uuidv7/
│       │   └── UuidV7Test.java          # Java unit tests (comprehensive)
│       └── kotlin/xyz/block/uuidv7/
│           └── UuidV7ExtensionsTest.kt  # Kotlin extension tests
├── build.gradle.kts                      # Gradle build configuration
├── settings.gradle.kts                   # Gradle settings
├── gradlew                               # Gradle wrapper script
└── README.md                             # User documentation
```

## Core Implementation Details

### UuidV7.java (src/main/java/xyz/block/uuidv7/UuidV7.java)

**Purpose**: Main utility class for generating and working with UUID v7 identifiers.

**Key Components**:
- **Static variables**:
  - `COUNTER_BITS`: 12 bits for counter (rand_a field)
  - `COUNTER_MAX`: 0xFFF (4095)
  - `SECURE_RANDOM`: Thread-safe SecureRandom for initial counter values
  - `monotonicGeneratorLastTimestamp`: Tracks last timestamp for monotonic mode
  - `monotonicGeneratorCounter`: Current counter value for monotonic mode

**Public API Methods**:

1. **Non-Monotonic Generation** (High Performance):
   - `generate()`: Generate UUID v7 using current system time
   - `generate(LongSupplier clock)`: Generate with custom clock source
   - Uses `ThreadLocalRandom` for all random bits (zero synchronization)
   - UUIDs in same millisecond may not be strictly ordered
   - Best for: High-throughput scenarios, distributed systems, logging

2. **Monotonic Generation** (Sequential Ordering):
   - `generateMonotonic()`: Generate UUID v7 with strict ordering guarantee
   - `generateMonotonic(LongSupplier clock)`: Generate with custom clock and ordering
   - Uses synchronized counter to ensure ordering within same millisecond
   - Counter can generate up to 4096 UUIDs per millisecond
   - If counter overflows, blocks until next millisecond
   - Counter resets to random value when timestamp advances (unpredictability)
   - Best for: Database primary keys, audit logs, any scenario requiring guaranteed ordering

3. **Utility Methods**:
   - `getTimestamp(UUID uuid)`: Extract millisecond timestamp from UUID v7
   - Validates UUID is version 7 and not null

**Implementation Notes**:
- UUID v7 Format (RFC 9562):
  - Bits 0-47: Unix timestamp in milliseconds (48 bits)
  - Bits 48-51: Version field (0111 for v7)
  - Bits 52-63: Counter or random bits (12 bits, rand_a)
  - Bits 64-65: Variant field (10 for RFC 4122)
  - Bits 66-127: Random bits (62 bits, rand_b)
- All methods work with standard `java.util.UUID` class
- Monotonic methods are `synchronized` at the class level
- Non-monotonic methods have zero synchronization overhead

### UuidV7Extensions.kt (src/main/kotlin/xyz/block/uuidv7/UuidV7Extensions.kt)

**Purpose**: Kotlin extension property for idiomatic timestamp extraction.

**API**:
- `UUID.timestamp`: Extension property that returns `Long`
- Provides Kotlin-friendly syntax: `uuid.timestamp` instead of `UuidV7.getTimestamp(uuid)`

## Testing Approach

### Test Coverage (UuidV7Test.java)

The test suite is comprehensive and covers:

1. **Basic Functionality**:
   - Valid UUID generation (version 7, variant 2)
   - Custom clock support
   - Timestamp extraction accuracy

2. **Validation**:
   - Null UUID handling
   - Non-v7 UUID rejection
   - Timestamp precision (milliseconds)

3. **Uniqueness**:
   - 10,000+ unique UUIDs generated
   - High-load scenarios (10,000 UUIDs at same timestamp)
   - Both monotonic and non-monotonic modes

4. **Monotonic Mode Behavior**:
   - Strict ordering guarantee within same millisecond
   - Counter overflow handling (blocks until next millisecond)
   - Counter reset on timestamp change
   - 5,000+ UUID generation with counter overflow

5. **Non-Monotonic Mode Behavior**:
   - No blocking behavior
   - Uniqueness maintained via random bits
   - Performance under high load

6. **Time Ordering**:
   - UUIDs from different timestamps sort correctly
   - Sequential generation maintains order in monotonic mode

## Build & Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# Generate JAR
./gradlew jar

# Generate sources JAR
./gradlew sourcesJar

# Generate javadoc JAR
./gradlew javadocJar

# Publish to local Maven repository (for testing)
./gradlew publishToMavenLocal
```

## Git Workflow

**Current Branch**: `mrohan/monotonic-uuid`
**Main Branch**: `main` (use for pull requests)
**Status**: Clean working directory

**Recent Commits**:
- `70afb84`: Renamed monotonic generator counter and last timestamp variables
- `de72373`: Added support for monotonic UUIDv7
- `8d76a6f`: Add @mrohan-sq as a code owner
- `51b4916`: Contributing guidelines
- `ede084e`: Primary Implementation of UUID v7

## Design Principles

1. **Minimal API Surface**: Static utility methods work with `java.util.UUID` rather than introducing a new type
2. **Dual-Mode Generation**: Separate methods for different performance/ordering trade-offs
3. **Zero Dependencies**: Uses only Java standard library (ThreadLocalRandom, SecureRandom, UUID)
4. **Maximum Compatibility**: Returns standard `java.util.UUID` instances
5. **Performance-First**: Non-monotonic mode has zero synchronization overhead
6. **RFC Compliance**: Follows RFC 9562 UUID v7 specification

## Code Style & Conventions

- Follow standard Java and Kotlin conventions
- Keep API minimal and focused
- All public methods must have Javadoc
- Ensure all tests pass before submitting changes
- Use AssertJ for test assertions (fluent assertions)
- Test method names should be descriptive and use camelCase

## Common Development Tasks

### Adding a New Feature

1. Create feature branch from `main`
2. Implement feature with tests
3. Ensure `./gradlew test` passes
4. Update README.md if API changes
5. Submit pull request to `main`

### Modifying Core Implementation

**File**: `src/main/java/xyz/block/uuidv7/UuidV7.java`

- Keep both non-monotonic and monotonic modes synchronized with changes
- Update tests in `UuidV7Test.java`
- Consider thread safety implications
- Benchmark performance if changing generation logic

### Adding Kotlin Extensions

**File**: `src/main/kotlin/xyz/block/uuidv7/UuidV7Extensions.kt`

- Keep extensions minimal and focused
- Add tests in `UuidV7ExtensionsTest.kt`
- Ensure extensions feel idiomatic to Kotlin developers

## Key Implementation Details to Remember

### Random Number Generation Strategy

- **Non-monotonic mode**: Uses `ThreadLocalRandom` for all random bits (maximum performance)
- **Monotonic mode**: Uses `ThreadLocalRandom` for rand_b (62 bits), `SecureRandom` only for counter initialization
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

## Performance Characteristics

- **Non-monotonic mode**: ~100+ million UUIDs/second (no contention)
- **Monotonic mode**: Limited by synchronization, ~4096 UUIDs/millisecond max
- **Memory**: Zero allocation overhead beyond UUID object itself
- **Thread safety**:
  - Non-monotonic: Lock-free, thread-safe via ThreadLocalRandom
  - Monotonic: Thread-safe via method-level synchronization

## When to Use Each Mode

### Use Non-Monotonic (`generate()`):
- High-throughput scenarios
- Distributed systems (cross-server ordering impossible anyway)
- Logging and tracing
- Event streams
- Any scenario where performance > strict ordering

### Use Monotonic (`generateMonotonic()`):
- Database primary keys
- Audit logs requiring guaranteed chronological order
- Single-node sequential ID generation
- Any scenario where strict ordering is required

## Dependencies

**Runtime**: None (uses only Java standard library)

**Test**:
- JUnit Jupiter 5.10.1 (test framework)
- AssertJ 3.24.2 (fluent assertions)
- JUnit Platform Launcher (test runtime)

## Related Resources

- [RFC 9562 - UUID v7 Specification](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7)
- [CODEOWNERS](./CODEOWNERS) - Project maintainers
- [CONTRIBUTING.md](./CONTRIBUTING.md) - Development guide
- [GOVERNANCE.md](./GOVERNANCE.md) - Project governance
- [LICENSE](./LICENSE) - Apache License 2.0
