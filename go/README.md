# UUID v7 for Go

A high-performance UUID v7 implementation for Go following [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562.html).

## Features

- **Time-ordered UUIDs**: Naturally sortable by creation time
- **Two variants**:
  - `Generate()`: High-performance, no ordering guarantees within same millisecond
  - `GenerateMonotonic()`: Strictly ordered with synchronized counter
- **Compact strings**: 22-character Base62 encoding, lexicographically sortable
- **Thread-safe**: Safe for concurrent use

## Installation

```bash
go get github.com/block/uuidv7
```

## Quick Start

```go
package main

import (
    "fmt"
    "github.com/block/uuidv7"
)

func main() {
    // Generate a UUID v7
    id := uuidv7.Generate()
    fmt.Println(id.String())  // e.g., "01936a7e-5c1d-7abc-8def-0123456789ab"

    // Generate as compact string (22 characters)
    compact := uuidv7.GenerateCompactString()
    fmt.Println(compact)  // e.g., "01JDQYZ9M6K7TCJK2F3W8N"

    // Monotonic variant for strict ordering
    orderedID := uuidv7.GenerateMonotonic()
    fmt.Println(orderedID.String())
}
```

## API Reference

### Generation Functions

```go
// Standard generation (high performance, no ordering guarantees)
uuid := uuidv7.Generate()
uuid := uuidv7.New()  // alias

// Monotonic generation (strict ordering, synchronized)
uuid := uuidv7.GenerateMonotonic()
uuid := uuidv7.NewMonotonic()  // alias

// With custom clock (useful for testing)
uuid := uuidv7.GenerateWithClock(func() int64 { return time.Now().UnixMilli() })
uuid := uuidv7.GenerateMonotonicWithClock(clock)

// Generate directly as compact string
compact := uuidv7.GenerateCompactString()
compact := uuidv7.GenerateCompactStringWithClock(clock)
```

### UUID Methods

```go
uuid := uuidv7.Generate()

// String representation
uuid.String()         // "01936a7e-5c1d-7abc-8def-0123456789ab"
uuid.CompactString()  // "01JDQYZ9M6K7TCJK2F3W8N" (22 chars)

// Extract timestamp
ts, err := uuid.Timestamp()  // milliseconds since Unix epoch
t, err := uuid.Time()        // time.Time value

// Properties
uuid.Version()  // 7
uuid.Variant()  // 2 (RFC 4122)
uuid.Bytes()    // []byte

// Comparison
uuid1.Compare(uuid2)  // -1, 0, or 1
```

### Parsing Functions

```go
// From standard string
uuid, err := uuidv7.FromString("01936a7e-5c1d-7abc-8def-0123456789ab")
uuid := uuidv7.MustFromString("...")  // panics on error

// From compact string
uuid, err := uuidv7.FromCompactString("01JDQYZ9M6K7TCJK2F3W8N")
uuid := uuidv7.MustFromCompactString("...")  // panics on error

// From bytes
uuid, err := uuidv7.FromBytes(bytes)

// Convert any UUID to compact string
compact := uuidv7.ToCompactString(uuid)
```

## When to Use Each Variant

### `Generate()` - High Performance

- Best for: Distributed systems, high-throughput scenarios
- Guarantees: Uniqueness (via random bits)
- No synchronization overhead
- UUIDs in same millisecond may not be strictly ordered

### `GenerateMonotonic()` - Strict Ordering

- Best for: Database primary keys, audit logs
- Guarantees: Strict ordering within process
- Uses synchronized counter (12 bits: 0-4095 per millisecond)
- If counter overflows (>4096 in same ms), blocks until next millisecond

## Compact String Format

The compact string format provides:

- **22 characters** vs 36 for standard UUID strings (39% shorter)
- **Lexicographically sortable** - maintains time-based ordering
- **URL-safe** - only uses `0-9`, `A-Z`, `a-z`
- **Smaller indexes** - better database performance

```go
uuid := uuidv7.Generate()
compact := uuid.CompactString()  // "01JDQYZ9M6K7TCJK2F3W8N"

// Round-trip
decoded, _ := uuidv7.FromCompactString(compact)
// decoded == uuid
```

## UUID v7 Format

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         unix_ts_ms                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          unix_ts_ms           |  ver  |       rand_a          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|var|                       rand_b                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           rand_b                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version (0111 for v7)
- **Bits 52-63**: Random or counter (12 bits)
- **Bits 64-65**: Variant (10 for RFC 4122)
- **Bits 66-127**: Random (62 bits)

## Benchmarks

Run benchmarks with:

```bash
go test -bench=. -benchmem
```

## License

Apache 2.0 - See [LICENSE](../LICENSE) for details.
