# Agent Guide - Rust Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd rust
cargo build           # Build package
cargo test            # Run tests
cargo test -- --nocapture  # Run tests with output
cargo bench           # Run benchmarks
cargo clippy          # Run linter
cargo fmt             # Format code
```

## Prerequisites

- Rust 1.70 or later (2021 edition)
- Cargo (included with Rust)

## Project Structure

```
rust/
├── src/
│   └── lib.rs           # Main implementation (both variants + compact strings)
├── benches/
│   └── benchmarks.rs    # Criterion benchmarks
├── Cargo.toml           # Package manifest
├── README.md            # API documentation and usage
└── AGENTS.md            # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- **generate()**: Uses `rand::thread_rng()` for all random bits (maximum performance)
- **generate_monotonic()**: Uses `rand::thread_rng()` for rand_b (62 bits), `getrandom` for counter initialization
- **Rationale**: Cryptographic randomness not required for UUIDs; performance is priority

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), method blocks/waits for next millisecond
- Uses `std::sync::Mutex` for thread safety
- Call `reset_monotonic_state()` in tests to reset counter state

### Uuid Type

- `Uuid` is a newtype wrapper around `[u8; 16]`
- Derives: `Clone`, `Copy`, `PartialEq`, `Eq`, `PartialOrd`, `Ord`, `Hash`, `Default`
- Implements `Display`, `Debug`, `FromStr`
- Methods: `to_string()`, `to_compact_string()`, `timestamp()`, `time()`, `version()`, `variant()`, `as_bytes()`, `to_bytes()`
- Zero value is valid (all zeros)

### Compact String Encoding

- Uses native `u128` for Base62 conversion (no external bigint needed)
- Static lookup table for fast decoding
- Preserves lexicographic ordering (big-endian encoding)

### Error Handling

- Uses `ParseError` enum for parsing failures
- `timestamp()` and `time()` return `Option` (None if not version 7)
- Parsing functions return `Result<Uuid, ParseError>`

## Testing

```bash
# Run all tests
cargo test

# Run with output visible
cargo test -- --nocapture

# Run specific test
cargo test test_monotonic_ensures_strict_ordering

# Run benchmarks
cargo bench

# Run with thread sanitizer (nightly)
RUSTFLAGS="-Z sanitizer=thread" cargo +nightly test
```

## Dependencies

- `rand` (0.8): Fast random number generation via `thread_rng()`
- `getrandom` (0.2): Cryptographically secure random for counter initialization
- `criterion` (dev): Benchmarking framework

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562.html) - UUID v7 specification
