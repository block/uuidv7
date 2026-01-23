# UUIDv7 - Ruby

A minimal, high-performance UUID v7 implementation for Ruby.

## Introduction

[UUID v7](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7) is a time-ordered UUID format that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time. This is useful for:

- Database indexed fields that benefit from sequential ordering
- Distributed systems where time-based ordering is valuable
- Event logs and audit trails where chronological sorting is important

### Compact Base62 Format

**Recommended for APIs, databases, and anywhere IDs are stored or transmitted as text.**

#### Why Compact Strings?

UUIDs are 128-bit values. When you need to store or transmit them, you have two choices:

1. **Binary (16 bytes)**: Most efficient, but not human-readable and requires binary-safe storage
2. **String**: Human-readable and universally supported, but takes more space

If you must use strings (APIs, URLs, text database columns, JSON, logs), the standard UUID format (`01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f`) uses 36 characters. The compact format reduces this to **22 characters** while preserving all the benefits of UUID v7.

#### How It Works

The compact format uses **Base62 encoding** (digits `0-9`, uppercase `A-Z`, lowercase `a-z`) to represent the 128-bit UUID value. This is similar to how Base64 works, but without special characters like `+`, `/`, or `=`.

```
Standard:  01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f  (36 chars)
Compact:   01JDQYZ9M6K7TCJK2F3W8N                (22 chars)
           ↑
           Same UUID, different encoding
```

The encoding preserves **lexicographic ordering**: if UUID A was generated before UUID B, then `compact(A) < compact(B)` in string comparison. This means database indexes on compact strings maintain time-ordering, and APIs can sort by ID to get chronological order.

#### When to Use Compact Strings

| Use Case | Recommended Format |
|----------|-------------------|
| Database binary column (BINARY(16), BLOB) | Binary (16 bytes) |
| Database text/VARCHAR column | **Compact (22 chars)** |
| REST API responses | **Compact (22 chars)** |
| URLs and query parameters | **Compact (22 chars)** |
| Logs and debugging | Standard (36 chars) for readability |
| Interop with systems expecting standard UUIDs | Standard (36 chars) |

#### Benefits Summary

- **39% shorter** than standard UUID strings (22 vs 36 characters)
- **Lexicographically sortable** - preserves time-based ordering in databases and APIs
- **URL-safe** - no special characters, hyphens, or encoding needed
- **Database-friendly** - smaller indexes, faster queries, less storage

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'uuidv7'
```

And then execute:

```bash
bundle install
```

Or install it yourself as:

```bash
gem install uuidv7
```

## Usage

```ruby
require 'uuidv7'

# Generate a UUID v7 (high performance, no ordering guarantees within same millisecond)
uuid = UUIDv7.generate
# => "01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f"

# Generate with monotonic ordering (for database primary keys)
uuid = MonotonicUUIDv7.generate
# => "01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f"

# Extract the timestamp (milliseconds since Unix epoch)
timestamp = UUIDv7.timestamp(uuid)
# => 1700000000000

# Generate a compact string directly (22 characters, Base62)
compact_id = UUIDv7.generate_compact
# => "01JDQYZ9M6K7TCJK2F3W8N"

# Convert UUID to compact string (preserves sort order)
compact = UUIDv7.to_compact(uuid)

# Convert compact string back to UUID
uuid = UUIDv7.from_compact(compact)

# Custom clock for testing
uuid = UUIDv7.generate { 1700000000000 }

# Monotonic with custom clock
uuid = MonotonicUUIDv7.generate { 1700000000000 }
```

## Design Principles

**Minimal API Surface**: Simple module methods that return standard UUID strings. No custom UUID class - maximum compatibility with existing code.

**Separate Modules for Different Use Cases**: Two distinct implementations for different performance/ordering trade-offs:
- **`UUIDv7`**: Maximum performance with no synchronization overhead. UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness is maintained through random bits. Ideal for high-throughput scenarios and distributed systems.
- **`MonotonicUUIDv7`**: Uses a mutex-protected counter to ensure strict ordering within the same millisecond, following RFC 9562 recommendations. Best for database primary keys and scenarios requiring guaranteed sequential ordering.

**Timestamp Extraction**: UUIDs contain timing information, and this library makes it easy to extract this for debugging, observability, and time-based queries.

**Flexible Generation**: Block-based clock injection for testing or custom time sources.

## API Reference

### UUIDv7 (High Performance, No Ordering Guarantees)

| Method | Description |
|--------|-------------|
| `UUIDv7.generate(&clock)` | Generate a new UUID v7 string. Optional block for custom clock. |
| `UUIDv7.generate_compact(&clock)` | Generate a new UUID v7 as 22-character compact string. |
| `UUIDv7.timestamp(uuid)` | Extract millisecond timestamp from UUID v7 string. |
| `UUIDv7.to_compact(uuid)` | Convert UUID string to 22-character compact string. |
| `UUIDv7.from_compact(compact)` | Convert compact string back to UUID string. |

### MonotonicUUIDv7 (Sequential Ordering Guaranteed)

| Method | Description |
|--------|-------------|
| `MonotonicUUIDv7.generate(&clock)` | Generate a new UUID v7 with strict ordering. Optional block for custom clock. |
| `MonotonicUUIDv7.generate_compact(&clock)` | Generate with strict ordering as compact string. |

## Implementation Details

### Format

UUID v7 follows RFC 9562:
- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Counter or random bits (12 bits, called `rand_a`)
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits, called `rand_b`)

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances
- If counter overflows (4096 in same ms), waits for next millisecond
- Thread-safe via Mutex synchronization

## License

Apache-2.0
