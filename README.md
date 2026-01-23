# UUIDv7

High-performance UUID v7 implementations across multiple languages.

## Introduction

[UUID v7](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7) is a time-ordered UUID format that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time. This is useful for:

- Database indexed fields that benefit from sequential ordering
- Distributed systems where time-based ordering is valuable
- Event logs and audit trails where chronological sorting is important

This repository provides minimal, high-performance implementations across multiple languages that integrate seamlessly with each language's standard UUID type.

### Compact Base62 Format

**Recommended for APIs, databases, and anywhere IDs are stored or transmitted as text.**

All implementations provide 22-character Base62 compact strings using `0-9A-Za-z` that are:
- **39% shorter** than standard UUID strings (22 vs 36 characters)
- **Lexicographically sortable** - preserves time-based ordering
- **URL-safe** - no special characters, hyphens, or encoding needed
- **Database-friendly** - shorter indexed strings, better performance

**Example**: `01JDQYZ9M6K7TCJK2F3W8N` (compact) vs `01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f` (standard)

Use compact strings for wire formats, database text fields, URLs, and API responses.

## Language Implementations

| Language | Status | Directory | Package |
|----------|--------|-----------|---------|
| **Java** | ✅ Stable | [java/](java/) | `xyz.block:uuidv7` |
| **JavaScript** | ✅ Stable | [javascript/](javascript/) | `@block/uuidv7` |
| **Ruby** | 🚧 Planned | [ruby/](ruby/) | - |
| **Go** | 🚧 Planned | [go/](go/) | - |
| **Swift** | 🚧 Planned | [swift/](swift/) | - |

## Design Principles

All implementations follow these core principles:

**Minimal API Surface**: Utility functions that work with the language's standard UUID type rather than introducing a new type. Maximum compatibility with existing code.

**Separate Implementations for Different Use Cases**: Two distinct variants for different performance/ordering trade-offs:
- **Standard**: Maximum performance with zero synchronization overhead. UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness is maintained through random bits.
- **Monotonic**: Uses a synchronized counter to ensure strict ordering within the same millisecond, following RFC 9562 recommendations. Best for database primary keys and scenarios requiring guaranteed sequential ordering.

**Timestamp Extraction**: UUIDs contain timing information, and implementations make it easy to extract this for debugging, observability, and time-based queries.

**Compact String Representation**: All implementations provide methods to generate and parse 22-character Base62 compact strings directly. Recommended for wire formats, database text fields, URLs, and API responses. The compact format is lexicographically sortable and preserves the time-ordering of UUID v7.

## Format

UUID v7 follows RFC 9562:
- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Counter or random bits (12 bits, called `rand_a`)
  - **Monotonic mode**: Sequential counter for strict ordering within the same millisecond
  - **Standard mode**: Random bits for maximum performance
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits, called `rand_b`)

## Quick Start

See the README in each language directory for installation and usage:

- **Java**: [java/README.md](java/README.md)
- **JavaScript**: [javascript/README.md](javascript/README.md)
- **Ruby**: Coming soon
- **Go**: Coming soon
- **Swift**: Coming soon

## Project Resources

| Resource                                   | Description                                                                    |
| ------------------------------------------ | ------------------------------------------------------------------------------ |
| [CODEOWNERS](./CODEOWNERS)                 | Outlines the project lead(s)                                                   |
| [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) | Expected behavior for project contributors, promoting a welcoming environment |
| [CONTRIBUTING.md](./CONTRIBUTING.md)       | Developer guide to build, test, run, access CI, chat, discuss, file issues     |
| [GOVERNANCE.md](./GOVERNANCE.md)           | Project governance                                                             |
| [LICENSE](./LICENSE)                       | Apache License, Version 2.0                                                    |
