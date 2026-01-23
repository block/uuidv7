# Agent Guide

This is a multi-language monorepo for UUID v7 implementations.

## Repository Structure

```
/
├── java/         # Java implementation (stable)
├── swift/        # Swift implementation (stable)
├── ruby/         # Ruby implementation (planned)
├── go/           # Go implementation (planned)
├── javascript/   # JavaScript implementation (planned)
├── README.md     # Multi-language overview
└── AGENTS.md     # This file
```

## Language-Specific Guides

Each language has its own AGENTS.md with implementation details:

- **Java**: [java/AGENTS.md](java/AGENTS.md)
- **Swift**: [swift/AGENTS.md](swift/AGENTS.md)
- **Ruby**: Coming soon
- **Go**: Coming soon
- **JavaScript**: Coming soon

## Shared Implementation Principles

All implementations should follow these principles:

### UUID v7 Format

- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Counter or random bits (12 bits, `rand_a`)
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits, `rand_b`)

### Two Variants

1. **Standard (High Performance)**
   - Uses thread-local random for all random bits
   - Zero synchronization overhead
   - No ordering guarantees within same millisecond
   - Best for: High-throughput scenarios, distributed systems

2. **Monotonic (Strictly Ordered)**
   - Uses synchronized counter for `rand_a` (12 bits: 0-4095)
   - Counter increments within same millisecond
   - Counter resets to random value when timestamp advances
   - If counter overflows (4096 in same ms), wait for next millisecond
   - Best for: Database primary keys, audit logs

### Timestamp Extraction

- Timestamp is in most significant 48 bits
- Returns milliseconds since Unix epoch
- Should validate UUID is version 7 before extracting

### Compact String Encoding (PRIMARY FORMAT)

**This is the primary ID format - all implementations MUST support generating and parsing compact strings.**

- 22-character Base62 encoding using `0-9A-Za-z`
- **Lexicographically sortable** - maintains time-based ordering
- URL-safe with no special characters or encoding needed
- 39% shorter than standard UUID string format (22 vs 36 chars)
- Database indexes are smaller and faster
- Example: `01JDQYZ9M6K7TCJK2F3W8N`

**Implementation requirements:**
- Provide a method to generate UUID v7 directly as a compact string
- Provide a method to parse compact string back to UUID
- Provide a method to convert any UUID to/from compact string
- Ensure lexicographic sort order is preserved (big-endian encoding)

### Random Number Generation

- Use fast, non-cryptographic random (e.g., `ThreadLocalRandom` in Java)
- Cryptographic randomness not required for UUIDs (per RFC 9562)
- Performance is priority over cryptographic security
- Monotonic variant may use secure random only for counter initialization

## Working on a Language Implementation

When adding or modifying a language implementation:

1. Navigate to the language subdirectory
2. Read the language-specific AGENTS.md
3. Follow the build/test commands for that language
4. Ensure tests pass before submitting PR
5. Update both language-specific and root README if needed
