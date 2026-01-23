# UUIDv7 - JavaScript/TypeScript

A minimal, high-performance UUID v7 implementation for JavaScript and TypeScript.

## Introduction

[UUID v7](https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7) is a time-ordered UUID format that encodes a Unix timestamp in the most significant 48 bits, making UUIDs naturally sortable by creation time. This is useful for:

- Database indexed fields that benefit from sequential ordering
- Distributed systems where time-based ordering is valuable
- Event logs and audit trails where chronological sorting is important

This library works with:
- **Node.js** (v18+)
- **Next.js** (App Router and Pages Router)
- **Browser** environments
- Any JavaScript/TypeScript project

### Compact Base62 Format

**Recommended for APIs, databases, and anywhere IDs are stored or transmitted as text.**

This implementation provides 22-character Base62 compact strings using `0-9A-Za-z` that are:
- **39% shorter** than standard UUID strings (22 vs 36 characters)
- **Lexicographically sortable** - preserves time-based ordering in databases and APIs
- **URL-safe** - no special characters, hyphens, or encoding needed
- **Database-friendly** - shorter indexed strings mean better query performance

**Example**: `01JDQYZ9M6K7TCJK2F3W8N` (compact) vs `01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f` (standard)

## Installation

```bash
npm install @block/uuidv7
```

## Usage

### Basic Usage

```typescript
import { UUIDv7, MonotonicUUIDv7 } from '@block/uuidv7';

// Generate a UUID v7 (maximum performance, no ordering guarantees)
const uuid = UUIDv7.generate();
// => "01936c0a-5e0c-7b3a-8f9d-2e1c4a6b8d0f"

// Generate with monotonic ordering (for database primary keys)
const monotonicUuid = MonotonicUUIDv7.generate();

// Extract the timestamp (milliseconds since Unix epoch)
const timestamp = UUIDv7.getTimestamp(uuid);

// Validate a UUID v7
const isValid = UUIDv7.isValid(uuid); // => true
```

### Compact String Format

```typescript
import { UUIDv7 } from '@block/uuidv7';

// Generate a compact string directly (22 characters, Base62)
const compactId = UUIDv7.generateCompactString();
// => "01JDQYZ9M6K7TCJK2F3W8N"

// Convert UUID to compact string (preserves sort order)
const uuid = UUIDv7.generate();
const compact = UUIDv7.toCompactString(uuid);

// Convert compact string back to UUID
const fromCompact = UUIDv7.fromCompactString(compact);
```

### Next.js Usage

Works seamlessly in both Server and Client Components:

```typescript
// app/api/users/route.ts (App Router)
import { UUIDv7 } from '@block/uuidv7';

export async function POST(request: Request) {
  const id = UUIDv7.generateCompactString();
  // Use id as database primary key
}
```

```typescript
// components/CreateButton.tsx (Client Component)
'use client';
import { UUIDv7 } from '@block/uuidv7';

export function CreateButton() {
  const handleClick = () => {
    const tempId = UUIDv7.generate();
    // Use for optimistic updates
  };
}
```

### Custom Clock (for Testing)

```typescript
import { UUIDv7 } from '@block/uuidv7';

// Use a fixed timestamp for reproducible tests
const fixedTime = 1704067200000; // 2024-01-01 00:00:00 UTC
const uuid = UUIDv7.generate(() => fixedTime);
```

## API Reference

### UUIDv7 (High Performance)

Uses `Math.random()` with zero synchronization overhead. UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness is guaranteed.

| Method | Description |
|--------|-------------|
| `generate(clock?)` | Generate a new UUID v7 string |
| `generateCompactString(clock?)` | Generate a new UUID v7 as 22-char compact string |
| `getTimestamp(uuid)` | Extract timestamp from UUID v7 |
| `isValid(uuid)` | Check if string is valid UUID v7 |
| `toCompactString(uuid)` | Convert UUID to compact string |
| `fromCompactString(compact)` | Convert compact string to UUID |

### MonotonicUUIDv7 (Strict Ordering)

Uses a counter to ensure strict ordering within the same millisecond. Best for database primary keys.

| Method | Description |
|--------|-------------|
| `generate(clock?)` | Generate a new monotonic UUID v7 string |
| `generateCompactString(clock?)` | Generate a new monotonic UUID v7 as compact string |

## Design Details

### Format

UUID v7 follows RFC 9562:
- **Bits 0-47**: Unix timestamp in milliseconds (48 bits)
- **Bits 48-51**: Version field (0111 for v7)
- **Bits 52-63**: Counter or random bits (12 bits, called `rand_a`)
- **Bits 64-65**: Variant field (10 for RFC 4122)
- **Bits 66-127**: Random bits (62 bits, called `rand_b`)

### Two Implementations

**`UUIDv7`**:
- Uses `Math.random()` for all random bits
- Maximum performance with no synchronization
- Best for: High-throughput scenarios, distributed systems, logging

**`MonotonicUUIDv7`**:
- Uses a counter for strict ordering within milliseconds
- Can generate up to 4096 UUIDs per millisecond before waiting
- Uses `crypto.getRandomValues()` for counter initialization when available
- Best for: Database primary keys, audit logs

## License

Apache-2.0
