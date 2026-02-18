// Package uuidv7 provides generation and utilities for UUID v7 identifiers.
//
// UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
// in the most significant 48 bits, making UUIDs naturally sortable by creation time.
// This implementation follows RFC 9562.
//
// Two variants are provided:
//   - Generate/New: High-performance variant using random bits (no ordering guarantees within same millisecond)
//   - GenerateMonotonic/NewMonotonic: Strictly ordered variant with synchronized counter
package uuidv7

import (
	"crypto/rand"
	"encoding/binary"
	"errors"
	"fmt"
	"math/big"
	mrand "math/rand/v2"
	"sync"
	"time"
)

const (
	base62Alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
	compactLength  = 22
	counterMax     = 0xFFF // 12 bits = 4095
)

var (
	base62CharIndex [256]int
	base62BigInt    = big.NewInt(62)

	// Monotonic state (guarded by mutex)
	monotonicMu        sync.Mutex
	monotonicTimestamp int64
	monotonicCounter   int
)

func init() {
	// Initialize character lookup table for Base62 decoding
	for i := range base62CharIndex {
		base62CharIndex[i] = -1
	}
	for i, c := range base62Alphabet {
		base62CharIndex[c] = i
	}
}

// UUID represents a UUID v7 as a 16-byte array.
type UUID [16]byte

// Clock is a function that returns the current time in milliseconds since Unix epoch.
type Clock func() int64

// DefaultClock returns the current system time in milliseconds.
func DefaultClock() int64 {
	return time.Now().UnixMilli()
}

// Generate creates a new UUID v7 using the current system time.
//
// Uses math/rand/v2 for maximum performance with no synchronization overhead.
// UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
// is guaranteed through random bits. For monotonic ordering guarantees, use
// GenerateMonotonic instead.
func Generate() UUID {
	return GenerateWithClock(DefaultClock)
}

// GenerateWithClock creates a new UUID v7 using a custom clock source.
//
// Uses math/rand/v2 for maximum performance with no synchronization overhead.
// Useful for testing or specialized use cases where you need control over the timestamp.
func GenerateWithClock(clock Clock) UUID {
	timestamp := clock()
	randA := mrand.IntN(4096)
	randB := mrand.Uint64()
	return build(timestamp, randA, randB)
}

// GenerateMonotonic creates a new monotonic UUID v7 using the current system time.
//
// This function ensures that UUIDs generated within the same millisecond are strictly
// ordered by incrementing a counter. If the counter overflows within a millisecond,
// the function will block until the next millisecond to maintain uniqueness.
//
// This function is synchronized and best suited for database primary keys and scenarios
// requiring guaranteed sequential ordering.
func GenerateMonotonic() UUID {
	return GenerateMonotonicWithClock(DefaultClock)
}

// GenerateMonotonicWithClock creates a new monotonic UUID v7 using a custom clock source.
//
// This function ensures that UUIDs generated within the same millisecond are strictly
// ordered by incrementing a counter. Useful for testing monotonic behavior with
// controlled clock sources.
func GenerateMonotonicWithClock(clock Clock) UUID {
	monotonicMu.Lock()
	defer monotonicMu.Unlock()

	timestamp := clock()
	var counterValue int

	if timestamp <= monotonicTimestamp {
		// Same millisecond or clock went backward - clamp and increment counter
		timestamp = monotonicTimestamp
		monotonicCounter = (monotonicCounter + 1) & counterMax

		if monotonicCounter == 0 {
			// Counter overflow - wait for next millisecond to maintain uniqueness
			for timestamp <= monotonicTimestamp {
				timestamp = clock()
			}
			// New millisecond - update state and start with random counter value
			monotonicTimestamp = timestamp
			monotonicCounter = secureRandomCounter()
		}
		counterValue = monotonicCounter
	} else {
		// New millisecond - start with random counter value for unpredictability
		monotonicCounter = secureRandomCounter()
		counterValue = monotonicCounter
		monotonicTimestamp = timestamp
	}

	randB := mrand.Uint64()
	return build(timestamp, counterValue, randB)
}

// secureRandomCounter returns a random counter value using crypto/rand.
func secureRandomCounter() int {
	var buf [2]byte
	_, _ = rand.Read(buf[:])
	return int(binary.BigEndian.Uint16(buf[:])) & counterMax
}

// build constructs a UUID v7 from timestamp and random components.
func build(timestamp int64, randA int, randB uint64) UUID {
	// Defensively mask randA to ensure it fits in 12 bits
	mostSigBits := (uint64(timestamp) << 16) | uint64(randA&0xFFF)
	leastSigBits := randB

	// Set version to 7 (0111 in bits 48-51)
	mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFF) | 0x0000000000007000

	// Set variant to 10 (RFC 4122) in bits 64-65
	leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFF) | 0x8000000000000000

	var uuid UUID
	binary.BigEndian.PutUint64(uuid[:8], mostSigBits)
	binary.BigEndian.PutUint64(uuid[8:], leastSigBits)
	return uuid
}

// Timestamp extracts the timestamp component from the UUID.
//
// Returns the timestamp in milliseconds since Unix epoch.
// Returns an error if the UUID is not version 7.
func (u UUID) Timestamp() (int64, error) {
	if u.Version() != 7 {
		return 0, fmt.Errorf("UUID is not version 7 (got version %d)", u.Version())
	}
	msb := binary.BigEndian.Uint64(u[:8])
	return int64(msb >> 16), nil
}

// Version returns the UUID version.
func (u UUID) Version() int {
	return int((u[6] >> 4) & 0x0F)
}

// Variant returns the UUID variant.
func (u UUID) Variant() int {
	switch {
	case (u[8] >> 7) == 0:
		return 0 // NCS backward compatibility
	case (u[8] >> 6) == 2:
		return 2 // RFC 4122
	case (u[8] >> 5) == 6:
		return 6 // Microsoft backward compatibility
	case (u[8] >> 5) == 7:
		return 7 // Future use
	}
	return -1
}

// String returns the standard UUID string representation (8-4-4-4-12 format).
func (u UUID) String() string {
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x",
		u[:4], u[4:6], u[6:8], u[8:10], u[10:])
}

// CompactString returns the 22-character Base62 encoded representation.
//
// The resulting string preserves lexicographic ordering for UUID v7 values
// (time-ordered UUIDs will sort correctly as compact strings).
// Uses Base62 encoding (0-9, A-Z, a-z).
func (u UUID) CompactString() string {
	return ToCompactString(u)
}

// Time returns the timestamp as a time.Time value.
//
// Returns an error if the UUID is not version 7.
func (u UUID) Time() (time.Time, error) {
	ts, err := u.Timestamp()
	if err != nil {
		return time.Time{}, err
	}
	return time.UnixMilli(ts), nil
}

// Bytes returns the UUID as a byte slice.
func (u UUID) Bytes() []byte {
	return u[:]
}

// Compare compares two UUIDs lexicographically.
// Returns -1 if u < other, 0 if u == other, 1 if u > other.
func (u UUID) Compare(other UUID) int {
	for i := 0; i < 16; i++ {
		if u[i] < other[i] {
			return -1
		}
		if u[i] > other[i] {
			return 1
		}
	}
	return 0
}

// GenerateCompactString generates a new UUID v7 and returns it as a compact string.
//
// Equivalent to calling ToCompactString(Generate()).
func GenerateCompactString() string {
	return ToCompactString(Generate())
}

// GenerateCompactStringWithClock generates a new UUID v7 using a custom clock and returns it as a compact string.
func GenerateCompactStringWithClock(clock Clock) string {
	return ToCompactString(GenerateWithClock(clock))
}

// ToCompactString converts a UUID to a 22-character Base62 encoded string.
//
// The resulting string preserves lexicographic ordering for UUID v7 values
// (time-ordered UUIDs will sort correctly as compact strings).
func ToCompactString(u UUID) string {
	// Convert UUID to big.Int (unsigned 128-bit value)
	value := new(big.Int).SetBytes(u[:])

	// Convert to Base62
	result := make([]byte, compactLength)
	for i := compactLength - 1; i >= 0; i-- {
		var remainder big.Int
		value.DivMod(value, base62BigInt, &remainder)
		result[i] = base62Alphabet[remainder.Int64()]
	}

	return string(result)
}

// FromCompactString decodes a 22-character Base62 string back to a UUID.
//
// Returns an error if the string is not exactly 22 characters or contains invalid characters.
func FromCompactString(s string) (UUID, error) {
	if len(s) != compactLength {
		return UUID{}, fmt.Errorf("compact string must be exactly %d characters (got %d)", compactLength, len(s))
	}

	value := new(big.Int)
	for i := 0; i < compactLength; i++ {
		digit := base62CharIndex[s[i]]
		if digit < 0 {
			return UUID{}, fmt.Errorf("invalid compact string character: %c", s[i])
		}
		value.Mul(value, base62BigInt)
		value.Add(value, big.NewInt(int64(digit)))
	}

	// Convert big.Int to UUID bytes
	bytes := value.Bytes()
	var uuid UUID

	// Pad with leading zeros if necessary
	if len(bytes) < 16 {
		copy(uuid[16-len(bytes):], bytes)
	} else if len(bytes) == 16 {
		copy(uuid[:], bytes)
	} else {
		// Should not happen with valid 22-char Base62
		copy(uuid[:], bytes[len(bytes)-16:])
	}

	return uuid, nil
}

// MustFromCompactString decodes a compact string and panics on error.
func MustFromCompactString(s string) UUID {
	u, err := FromCompactString(s)
	if err != nil {
		panic(err)
	}
	return u
}

// FromString parses a UUID from its standard string representation.
func FromString(s string) (UUID, error) {
	if len(s) != 36 {
		return UUID{}, errors.New("invalid UUID string length")
	}

	if s[8] != '-' || s[13] != '-' || s[18] != '-' || s[23] != '-' {
		return UUID{}, errors.New("invalid UUID string format")
	}

	var uuid UUID
	hexStr := s[0:8] + s[9:13] + s[14:18] + s[19:23] + s[24:36]

	for i := 0; i < 16; i++ {
		hi := hexDigit(hexStr[i*2])
		lo := hexDigit(hexStr[i*2+1])
		if hi < 0 || lo < 0 {
			return UUID{}, errors.New("invalid hex character in UUID string")
		}
		uuid[i] = byte(hi<<4 | lo)
	}

	return uuid, nil
}

// MustFromString parses a UUID string and panics on error.
func MustFromString(s string) UUID {
	u, err := FromString(s)
	if err != nil {
		panic(err)
	}
	return u
}

// FromBytes creates a UUID from a 16-byte slice.
func FromBytes(b []byte) (UUID, error) {
	if len(b) != 16 {
		return UUID{}, fmt.Errorf("byte slice must be exactly 16 bytes (got %d)", len(b))
	}
	var uuid UUID
	copy(uuid[:], b)
	return uuid, nil
}

// GetTimestamp extracts the timestamp from a UUID v7.
//
// Returns an error if the UUID is not version 7.
// This is a package-level function equivalent to uuid.Timestamp().
func GetTimestamp(u UUID) (int64, error) {
	return u.Timestamp()
}

func hexDigit(c byte) int {
	switch {
	case '0' <= c && c <= '9':
		return int(c - '0')
	case 'a' <= c && c <= 'f':
		return int(c - 'a' + 10)
	case 'A' <= c && c <= 'F':
		return int(c - 'A' + 10)
	}
	return -1
}

// ResetMonotonicState resets the monotonic counter state.
// This is primarily useful for testing.
func ResetMonotonicState() {
	monotonicMu.Lock()
	defer monotonicMu.Unlock()
	monotonicTimestamp = 0
	monotonicCounter = 0
}
