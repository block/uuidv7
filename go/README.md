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
go get github.com/block/uuidv7/go
```

> **Note**: This package lives in the `go/` subdirectory of the monorepo, so the import path includes `/go`.

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
    fmt.Println(orderedID.CompactString())  // Works on any UUID!
}
```

**Note**: `String()` and `CompactString()` work on any UUID regardless of how it was generated. The difference between `Generate()` and `GenerateMonotonic()` is only about ordering guarantees during generation—the resulting UUID type is the same.

```go
// All UUIDs support both output formats
uuid := uuidv7.Generate()           // or GenerateMonotonic()
uuid.String()                       // "01936a7e-5c1d-7abc-..."
uuid.CompactString()                // "01JDQYZ9M6K7..."
```

## API Reference

### Generation Functions

```go
// Standard generation (high performance, no ordering guarantees)
uuid := uuidv7.Generate()

// Monotonic generation (strict ordering, synchronized)
uuid := uuidv7.GenerateMonotonic()

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

// String representation - two formats available:
uuid.String()         // "01936a7e-5c1d-7abc-8def-0123456789ab" (36 chars)
uuid.CompactString()  // "01JDQYZ9M6K7TCJK2F3W8N" (22 chars, recommended)

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

### Compact String Format

Compact strings are **Base62-encoded** UUIDs using only `0-9`, `A-Z`, `a-z`:

| Format | Example | Length | Storage |
|--------|---------|--------|---------|
| Standard | `01936a7e-5c1d-7abc-8def-0123456789ab` | 36 chars | 36 bytes |
| Compact | `01JDQYZ9M6K7TCJK2F3W8N` | 22 chars | 22 bytes |

**Why use compact strings?**
- **39% smaller** - 22 vs 36 characters saves storage in databases and logs
- **No dashes** - No punctuation means no escaping in URLs, filenames, or shell commands
- **Lexicographically sortable** - Maintains time-ordering when sorted as strings
- **URL-safe** - No special characters requiring percent-encoding

```go
// These are equivalent - same UUID, different string formats
uuid := uuidv7.Generate()
uuid.String()        // "01936a7e-5c1d-7abc-8def-0123456789ab"
uuid.CompactString() // "01JDQYZ9M6K7TCJK2F3W8N"

// Both can round-trip back to the same UUID
uuidv7.FromString("01936a7e-5c1d-7abc-8def-0123456789ab")
uuidv7.FromCompactString("01JDQYZ9M6K7TCJK2F3W8N")
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

```
goos: darwin
goarch: arm64
cpu: Apple M4 Max
BenchmarkGenerate-16                     36503178        32.81 ns/op       0 B/op    0 allocs/op
BenchmarkGenerateMonotonic-16             2434989       480.4 ns/op        0 B/op    0 allocs/op
BenchmarkToCompactString-16               3033646       389.2 ns/op      240 B/op   23 allocs/op
BenchmarkFromCompactString-16             3429414       340.9 ns/op       72 B/op    3 allocs/op
BenchmarkGenerateCompactString-16         2738077       437.8 ns/op      238 B/op   22 allocs/op
BenchmarkGenerateParallel-16             23659772        60.59 ns/op       0 B/op    0 allocs/op
BenchmarkGenerateMonotonicParallel-16     2377994       491.0 ns/op        0 B/op    0 allocs/op
```

Run benchmarks yourself:

```bash
go test -bench=. -benchmem
```

## License

Apache 2.0 - See [LICENSE](../LICENSE) for details.
