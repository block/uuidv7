# Agent Guide - Go Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd go
go build ./...    # Build package
go test ./...     # Run tests
go test -v ./...  # Run tests with verbose output
go test -bench=.  # Run benchmarks
```

## Prerequisites

- Go 1.21 or later

## Project Structure

```
go/
├── uuidv7.go       # Main implementation (both variants + compact strings)
├── uuidv7_test.go  # Comprehensive tests
├── go.mod          # Module definition
├── README.md       # API documentation and usage
└── AGENTS.md       # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- **Generate()**: Uses `math/rand/v2` for all random bits (maximum performance, no seeding required)
- **GenerateMonotonic()**: Uses `math/rand/v2` for rand_b (62 bits), `crypto/rand` only for counter initialization
- **Rationale**: Cryptographic randomness not required for UUIDs; performance is priority

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), method blocks/waits for next millisecond
- Uses `sync.Mutex` for thread safety
- Call `ResetMonotonicState()` in tests to reset counter state

### UUID Type

- `UUID` is a `[16]byte` array (value type, comparable, can be used as map key)
- Methods: `String()`, `CompactString()`, `Timestamp()`, `Time()`, `Version()`, `Variant()`, `Bytes()`, `Compare()`
- Zero value is valid (all zeros)

### Compact String Encoding

- Uses `math/big` for Base62 conversion (handles 128-bit values)
- Character lookup table for fast decoding
- Preserves lexicographic ordering (big-endian encoding)

### Package-Level Functions

- `build(timestamp, randA, randB)`: Internal function for constructing UUID v7
- `ToCompactString(uuid)`: Convert any UUID to compact string
- `FromCompactString(s)`: Parse compact string to UUID
- `FromString(s)`: Parse standard UUID string
- `FromBytes(b)`: Create UUID from bytes
- `GetTimestamp(uuid)`: Extract timestamp (alias for method)

## Testing

```bash
# Run all tests
go test ./...

# Run with verbose output
go test -v ./...

# Run specific test
go test -v -run TestMonotonicEnsuresStrictOrdering

# Run benchmarks
go test -bench=. -benchmem

# Run with race detector
go test -race ./...
```

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562.html) - UUID v7 specification
