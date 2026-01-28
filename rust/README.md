# UUID v7 for Rust

High-performance UUID v7 generation with optional monotonic ordering for Rust.

## Features

- **UUID v7 compliant**: Follows [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562.html)
- **Time-ordered**: UUIDs are naturally sortable by creation time
- **Two variants**: Standard (high-performance) and Monotonic (strictly ordered)
- **Compact strings**: 22-character Base62 encoding that preserves lexicographic ordering
- **Thread-safe**: Safe for concurrent use

## Installation

Add to your `Cargo.toml`:

```toml
[dependencies]
block-uuidv7 = "0.1"
```

## Usage

### Basic Generation

```rust
use block_uuidv7::{generate, Uuid};

// Generate a UUID v7
let uuid = generate();
println!("{}", uuid);  // e.g., "01902c74-5e82-7def-8a12-3456789abcde"

// Using the Uuid struct directly
let uuid = Uuid::new();
```

### Monotonic Generation

For scenarios requiring strict ordering within the same millisecond (database primary keys, audit logs):

```rust
use block_uuidv7::{generate_monotonic, Uuid};

let uuid1 = generate_monotonic();
let uuid2 = generate_monotonic();
assert!(uuid1 < uuid2);  // Always true, even within same millisecond

// Or using the struct method
let uuid = Uuid::new_monotonic();
```

### Compact String Format

22-character Base62 encoded strings that are URL-safe and preserve lexicographic ordering:

```rust
use block_uuidv7::{generate, generate_compact_string, from_compact_string};

// Generate directly as compact string
let compact = generate_compact_string();
println!("{}", compact);  // e.g., "0DXz5QpL3kF8N2M1R4Y6W"

// Convert existing UUID
let uuid = generate();
let compact = uuid.to_compact_string();

// Parse compact string back to UUID
let parsed = from_compact_string(&compact).unwrap();
assert_eq!(uuid, parsed);
```

### Timestamp Extraction

```rust
use block_uuidv7::generate;

let uuid = generate();

// Get timestamp in milliseconds since Unix epoch
if let Some(timestamp) = uuid.timestamp() {
    println!("Created at: {} ms", timestamp);
}

// Get as SystemTime
if let Some(time) = uuid.time() {
    println!("Created at: {:?}", time);
}
```

### Parsing and Conversion

```rust
use block_uuidv7::{from_string, from_compact_string, Uuid};

// Parse standard UUID string
let uuid = from_string("01902c74-5e82-7def-8a12-3456789abcde").unwrap();

// Using FromStr trait
let uuid: Uuid = "01902c74-5e82-7def-8a12-3456789abcde".parse().unwrap();

// Parse compact string
let uuid = from_compact_string("0DXz5QpL3kF8N2M1R4Y6W").unwrap();

// From bytes
let uuid = Uuid::from_bytes([0u8; 16]);
let uuid = Uuid::from_slice(&[0u8; 16]).unwrap();
```

### Custom Clock

For testing or specialized use cases:

```rust
use block_uuidv7::{generate_with_clock, generate_monotonic_with_clock};

let fixed_time = 1234567890000i64;
let uuid = generate_with_clock(|| fixed_time);

// For monotonic generation
let uuid = generate_monotonic_with_clock(|| fixed_time);
```

## API Reference

### Functions

| Function | Description |
|----------|-------------|
| `generate()` | Generate UUID v7 with random bits (high performance) |
| `generate_monotonic()` | Generate UUID v7 with monotonic counter (strict ordering) |
| `generate_with_clock(clock)` | Generate with custom timestamp source |
| `generate_monotonic_with_clock(clock)` | Monotonic generation with custom clock |
| `generate_compact_string()` | Generate directly as compact string |
| `to_compact_string(uuid)` | Convert UUID to compact string |
| `from_compact_string(s)` | Parse compact string to UUID |
| `from_string(s)` | Parse standard UUID string |
| `get_timestamp(uuid)` | Extract timestamp from UUID v7 |
| `reset_monotonic_state()` | Reset monotonic counter (for testing) |

### Uuid Methods

| Method | Description |
|--------|-------------|
| `new()` | Create new UUID v7 (alias for `generate()`) |
| `new_monotonic()` | Create new monotonic UUID v7 |
| `version()` | Get UUID version (7 for UUID v7) |
| `variant()` | Get UUID variant (2 for RFC 4122) |
| `timestamp()` | Extract timestamp in milliseconds |
| `time()` | Extract timestamp as `SystemTime` |
| `to_compact_string()` | Convert to 22-char Base62 string |
| `to_string()` | Convert to standard UUID string |
| `as_bytes()` | Get reference to byte array |
| `to_bytes()` | Get owned byte array |
| `from_bytes(bytes)` | Create from byte array |
| `from_slice(slice)` | Create from byte slice |

## UUID v7 Format

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         unix_ts_ms (32 bits)                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          unix_ts_ms (16 bits) |  ver  |       rand_a          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|var|                       rand_b                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           rand_b                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- **unix_ts_ms**: 48-bit timestamp in milliseconds since Unix epoch
- **ver**: 4-bit version (0111 for v7)
- **rand_a**: 12-bit random/counter field
- **var**: 2-bit variant (10 for RFC 4122)
- **rand_b**: 62-bit random field

## Compact String Format

- 22 characters using Base62 alphabet (`0-9`, `A-Z`, `a-z`)
- Lexicographically sortable (preserves time ordering)
- URL-safe with no special characters
- 39% shorter than standard UUID format (22 vs 36 characters)

## Performance

The standard `generate()` function uses `rand::thread_rng()` for maximum performance with no synchronization overhead.

The monotonic variant uses a mutex-protected counter with cryptographically secure random initialization.

Run benchmarks:

```bash
cargo bench
```

## License

Apache-2.0
