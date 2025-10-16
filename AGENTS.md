# Agent Guide

For project overview and API documentation, you must read [README.md](./README.md).  
For build instructions and contribution guidelines, you must read [CONTRIBUTING.md](./CONTRIBUTING.md).

## Quick Commands

```bash
./gradlew build    # Build project
./gradlew test     # Run tests
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
