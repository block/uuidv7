# Agent Guide - Ruby Implementation

For the multi-language overview, see [root AGENTS.md](../AGENTS.md).

## Quick Commands

```bash
cd ruby
bundle install    # Install dependencies
bundle exec rake spec      # Run tests
bundle exec rake rubocop   # Run linter
bundle exec rake           # Run both tests and linter
```

## Prerequisites

- Ruby 3.0 or later
- Bundler

## Project Structure

```
ruby/
├── lib/
│   ├── uuidv7.rb                    # Main entry point
│   └── uuidv7/
│       ├── version.rb               # Version constant
│       ├── generator.rb             # High-performance UUID v7 (no ordering guarantees)
│       ├── monotonic_generator.rb   # Monotonic UUID v7 (strict ordering)
│       └── base62.rb                # Base62 encoding/decoding
├── spec/
│   ├── spec_helper.rb
│   ├── uuidv7_spec.rb
│   ├── monotonic_uuidv7_spec.rb
│   └── base62_spec.rb
├── uuidv7.gemspec
├── Gemfile
├── Rakefile
├── README.md
└── AGENTS.md  # This file
```

## Key Implementation Details

### Random Number Generation Strategy

- Uses `SecureRandom` from Ruby stdlib for all random bits
- Ruby's `rand()` is not thread-safe, so `SecureRandom` is preferred
- Performance is still excellent for typical use cases

### Monotonic Counter Behavior

- Counter occupies 12 bits (rand_a field): 0-4095
- Counter increments with each generation in same millisecond
- Counter resets to **random value** when timestamp advances (not zero!)
- If counter overflows (4096 in same ms), waits for next millisecond
- Uses `Mutex` for thread safety

### Thread Safety

- `UUIDv7.generate`: Thread-safe (no shared mutable state)
- `MonotonicUUIDv7.generate`: Thread-safe via Mutex synchronization

### Clock Injection

Uses block-based clock injection for testability:

```ruby
UUIDv7.generate { 1700000000000 }
MonotonicUUIDv7.generate { Time.now.to_i * 1000 }
```

### Testing Notes

- `MonotonicGenerator.reset_state!` is available for test isolation
- Always reset state before monotonic tests to ensure deterministic behavior

## Gem Publishing

```bash
gem build uuidv7.gemspec
gem push uuidv7-x.x.x.gem
```

## Further Reading

- [README.md](README.md) - API documentation and usage examples
- [Root AGENTS.md](../AGENTS.md) - Shared algorithm and design principles
