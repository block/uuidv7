//! High-performance UUID v7 generation with optional monotonic ordering.
//!
//! UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
//! in the most significant 48 bits, making UUIDs naturally sortable by creation time.
//! This implementation follows RFC 9562.
//!
//! Two variants are provided:
//! - [`generate`]/[`Uuid::new`]: High-performance variant using random bits (no ordering guarantees within same millisecond)
//! - [`generate_monotonic`]/[`Uuid::new_monotonic`]: Strictly ordered variant with synchronized counter
//!
//! # Examples
//!
//! ```
//! use block_uuidv7::{generate, generate_monotonic, Uuid};
//!
//! // High-performance generation (no ordering guarantees within same ms)
//! let uuid = generate();
//! println!("UUID: {}", uuid);
//! println!("Compact: {}", uuid.to_compact_string());
//!
//! // Monotonic generation (strict ordering within same ms)
//! let uuid1 = generate_monotonic();
//! let uuid2 = generate_monotonic();
//! assert!(uuid1 < uuid2);
//! ```

use std::fmt;
use std::str::FromStr;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use rand::Rng;

const BASE62_ALPHABET: &[u8; 62] =
    b"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
const COMPACT_LENGTH: usize = 22;
const COUNTER_MAX: u16 = 0xFFF; // 12 bits = 4095

static BASE62_CHAR_INDEX: [i8; 256] = {
    let mut table = [-1i8; 256];
    let mut i = 0u8;
    while i < 10 {
        table[(b'0' + i) as usize] = i as i8;
        i += 1;
    }
    i = 0;
    while i < 26 {
        table[(b'A' + i) as usize] = (10 + i) as i8;
        table[(b'a' + i) as usize] = (36 + i) as i8;
        i += 1;
    }
    table
};

struct MonotonicState {
    timestamp: i64,
    counter: u16,
}

static MONOTONIC_STATE: Mutex<MonotonicState> = Mutex::new(MonotonicState {
    timestamp: 0,
    counter: 0,
});

/// A UUID v7 represented as a 16-byte array.
#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Default)]
pub struct Uuid([u8; 16]);

/// Clock trait for providing custom time sources.
pub trait Clock: Fn() -> i64 {}
impl<F: Fn() -> i64> Clock for F {}

/// Returns the current system time in milliseconds since Unix epoch.
pub fn default_clock() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("System time before Unix epoch")
        .as_millis() as i64
}

/// Generates a new UUID v7 using the current system time.
///
/// Uses a fast random number generator for all random bits with no synchronization overhead.
/// UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
/// is guaranteed through random bits. For monotonic ordering guarantees, use
/// [`generate_monotonic`] instead.
pub fn generate() -> Uuid {
    generate_with_clock(default_clock)
}

/// Generates a new UUID v7 using a custom clock source.
///
/// Useful for testing or specialized use cases where you need control over the timestamp.
pub fn generate_with_clock<C: Clock>(clock: C) -> Uuid {
    let timestamp = clock();
    let mut rng = rand::rng();
    let rand_a = rng.random_range(0..4096u16);
    let rand_b = rng.random::<u64>();
    build(timestamp, rand_a, rand_b)
}

/// Generates a new monotonic UUID v7 using the current system time.
///
/// This function ensures that UUIDs generated within the same millisecond are strictly
/// ordered by incrementing a counter. If the counter overflows within a millisecond,
/// the function will block until the next millisecond to maintain uniqueness.
///
/// This function is synchronized and best suited for database primary keys and scenarios
/// requiring guaranteed sequential ordering.
pub fn generate_monotonic() -> Uuid {
    generate_monotonic_with_clock(default_clock)
}

/// Generates a new monotonic UUID v7 using a custom clock source.
///
/// This function ensures that UUIDs generated within the same millisecond are strictly
/// ordered by incrementing a counter. Useful for testing monotonic behavior with
/// controlled clock sources.
pub fn generate_monotonic_with_clock<C: Clock>(clock: C) -> Uuid {
    let mut state = MONOTONIC_STATE.lock().unwrap();
    let mut timestamp = clock();
    let counter_value: u16;

    if timestamp == state.timestamp {
        state.counter = (state.counter + 1) & COUNTER_MAX;

        if state.counter == 0 {
            while timestamp == state.timestamp {
                timestamp = clock();
            }
            state.counter = secure_random_counter();
        }
        counter_value = state.counter;
    } else {
        state.counter = secure_random_counter();
        counter_value = state.counter;
        state.timestamp = timestamp;
    }

    let rand_b = rand::rng().random::<u64>();
    build(timestamp, counter_value, rand_b)
}

fn secure_random_counter() -> u16 {
    let mut buf = [0u8; 2];
    getrandom::fill(&mut buf).expect("Failed to get random bytes");
    u16::from_be_bytes(buf) & COUNTER_MAX
}

fn build(timestamp: i64, rand_a: u16, rand_b: u64) -> Uuid {
    let mut most_sig_bits = ((timestamp as u64) << 16) | ((rand_a & 0xFFF) as u64);
    let mut least_sig_bits = rand_b;

    // Set version to 7 (0111 in bits 48-51)
    most_sig_bits = (most_sig_bits & 0xFFFF_FFFF_FFFF_0FFF) | 0x0000_0000_0000_7000;

    // Set variant to 10 (RFC 4122) in bits 64-65
    least_sig_bits = (least_sig_bits & 0x3FFF_FFFF_FFFF_FFFF) | 0x8000_0000_0000_0000;

    let mut bytes = [0u8; 16];
    bytes[..8].copy_from_slice(&most_sig_bits.to_be_bytes());
    bytes[8..].copy_from_slice(&least_sig_bits.to_be_bytes());
    Uuid(bytes)
}

/// Generates a new UUID v7 and returns it as a compact string.
///
/// Equivalent to calling `generate().to_compact_string()`.
pub fn generate_compact_string() -> String {
    generate().to_compact_string()
}

/// Generates a new UUID v7 using a custom clock and returns it as a compact string.
pub fn generate_compact_string_with_clock<C: Clock>(clock: C) -> String {
    generate_with_clock(clock).to_compact_string()
}

/// Resets the monotonic counter state. Primarily useful for testing.
pub fn reset_monotonic_state() {
    let mut state = MONOTONIC_STATE.lock().unwrap();
    state.timestamp = 0;
    state.counter = 0;
}

impl Uuid {
    /// Creates a new UUID v7 using the current system time.
    pub fn new() -> Self {
        generate()
    }

    /// Creates a new monotonic UUID v7 using the current system time.
    pub fn new_monotonic() -> Self {
        generate_monotonic()
    }

    /// Returns the UUID version (should be 7 for UUID v7).
    pub fn version(&self) -> u8 {
        (self.0[6] >> 4) & 0x0F
    }

    /// Returns the UUID variant.
    pub fn variant(&self) -> u8 {
        match self.0[8] >> 6 {
            0b00 | 0b01 => 0, // NCS backward compatibility
            0b10 => 2,        // RFC 4122
            0b11 => 3,        // Future use / Microsoft
            _ => unreachable!(),
        }
    }

    /// Extracts the timestamp component from the UUID.
    ///
    /// Returns the timestamp in milliseconds since Unix epoch.
    /// Returns `None` if the UUID is not version 7.
    pub fn timestamp(&self) -> Option<i64> {
        if self.version() != 7 {
            return None;
        }
        let msb = u64::from_be_bytes(self.0[..8].try_into().unwrap());
        Some((msb >> 16) as i64)
    }

    /// Extracts the timestamp as a `SystemTime`.
    ///
    /// Returns `None` if the UUID is not version 7.
    pub fn time(&self) -> Option<SystemTime> {
        self.timestamp()
            .map(|ts| UNIX_EPOCH + std::time::Duration::from_millis(ts as u64))
    }

    /// Returns the UUID as a byte slice.
    pub fn as_bytes(&self) -> &[u8; 16] {
        &self.0
    }

    /// Returns the UUID as a byte array.
    pub fn to_bytes(self) -> [u8; 16] {
        self.0
    }

    /// Converts the UUID to a 22-character Base62 encoded compact string.
    ///
    /// The resulting string preserves lexicographic ordering for UUID v7 values
    /// (time-ordered UUIDs will sort correctly as compact strings).
    pub fn to_compact_string(&self) -> String {
        to_compact_string(self)
    }

    /// Returns the standard UUID string representation (8-4-4-4-12 format).
    fn format_string(&self) -> String {
        format!(
            "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
            self.0[0], self.0[1], self.0[2], self.0[3],
            self.0[4], self.0[5],
            self.0[6], self.0[7],
            self.0[8], self.0[9],
            self.0[10], self.0[11], self.0[12], self.0[13], self.0[14], self.0[15]
        )
    }

    /// Creates a UUID from a 16-byte array.
    pub fn from_bytes(bytes: [u8; 16]) -> Self {
        Uuid(bytes)
    }

    /// Creates a UUID from a byte slice.
    ///
    /// Returns `None` if the slice is not exactly 16 bytes.
    pub fn from_slice(slice: &[u8]) -> Option<Self> {
        if slice.len() != 16 {
            return None;
        }
        let mut bytes = [0u8; 16];
        bytes.copy_from_slice(slice);
        Some(Uuid(bytes))
    }
}

impl fmt::Display for Uuid {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.format_string())
    }
}

impl fmt::Debug for Uuid {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "Uuid({})", self.format_string())
    }
}

/// Error type for UUID parsing failures.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParseError {
    InvalidLength { expected: usize, got: usize },
    InvalidFormat,
    InvalidCharacter(char),
}

impl fmt::Display for ParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ParseError::InvalidLength { expected, got } => {
                write!(f, "invalid length: expected {}, got {}", expected, got)
            }
            ParseError::InvalidFormat => write!(f, "invalid UUID format"),
            ParseError::InvalidCharacter(c) => write!(f, "invalid character: {}", c),
        }
    }
}

impl std::error::Error for ParseError {}

impl FromStr for Uuid {
    type Err = ParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        from_string(s)
    }
}

/// Converts a UUID to a 22-character Base62 encoded string.
pub fn to_compact_string(uuid: &Uuid) -> String {
    let value = u128::from_be_bytes(uuid.0);

    if value == 0 {
        return "0".repeat(COMPACT_LENGTH);
    }

    let mut result = [b'0'; COMPACT_LENGTH];
    let mut val = value;

    for i in (0..COMPACT_LENGTH).rev() {
        result[i] = BASE62_ALPHABET[(val % 62) as usize];
        val /= 62;
    }

    String::from_utf8(result.to_vec()).unwrap()
}

/// Decodes a 22-character Base62 string back to a UUID.
pub fn from_compact_string(s: &str) -> Result<Uuid, ParseError> {
    if s.len() != COMPACT_LENGTH {
        return Err(ParseError::InvalidLength {
            expected: COMPACT_LENGTH,
            got: s.len(),
        });
    }

    let mut value: u128 = 0;
    for c in s.bytes() {
        let digit = BASE62_CHAR_INDEX[c as usize];
        if digit < 0 {
            return Err(ParseError::InvalidCharacter(c as char));
        }
        value = value
            .checked_mul(62)
            .and_then(|v| v.checked_add(digit as u128))
            .ok_or(ParseError::InvalidFormat)?;
    }

    Ok(Uuid(value.to_be_bytes()))
}

/// Parses a UUID from its standard string representation.
pub fn from_string(s: &str) -> Result<Uuid, ParseError> {
    if s.len() != 36 {
        return Err(ParseError::InvalidLength {
            expected: 36,
            got: s.len(),
        });
    }

    let bytes = s.as_bytes();
    if bytes[8] != b'-' || bytes[13] != b'-' || bytes[18] != b'-' || bytes[23] != b'-' {
        return Err(ParseError::InvalidFormat);
    }

    let hex_str: String = [&s[0..8], &s[9..13], &s[14..18], &s[19..23], &s[24..36]].concat();
    let hex_bytes = hex_str.as_bytes();

    let mut uuid_bytes = [0u8; 16];
    for (i, byte) in uuid_bytes.iter_mut().enumerate() {
        let hi = hex_digit(hex_bytes[i * 2])?;
        let lo = hex_digit(hex_bytes[i * 2 + 1])?;
        *byte = (hi << 4) | lo;
    }

    Ok(Uuid(uuid_bytes))
}

fn hex_digit(c: u8) -> Result<u8, ParseError> {
    match c {
        b'0'..=b'9' => Ok(c - b'0'),
        b'a'..=b'f' => Ok(c - b'a' + 10),
        b'A'..=b'F' => Ok(c - b'A' + 10),
        _ => Err(ParseError::InvalidCharacter(c as char)),
    }
}

/// Extracts the timestamp from a UUID v7.
///
/// Returns `None` if the UUID is not version 7.
pub fn get_timestamp(uuid: &Uuid) -> Option<i64> {
    uuid.timestamp()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashSet;
    use std::sync::atomic::{AtomicI64, Ordering};
    use std::thread;

    static MONOTONIC_TEST_MUTEX: Mutex<()> = Mutex::new(());

    #[test]
    fn test_generate_creates_valid_uuid() {
        let uuid = generate();
        assert_eq!(uuid.version(), 7);
        assert_eq!(uuid.variant(), 2);
    }

    #[test]
    fn test_generate_with_custom_clock() {
        let fixed_time = 1234567890000i64;
        let uuid = generate_with_clock(|| fixed_time);
        assert_eq!(uuid.timestamp(), Some(fixed_time));
    }

    #[test]
    fn test_get_timestamp_extracts_correct_value() {
        let expected_time = 1234567890123i64;
        let uuid = generate_with_clock(|| expected_time);
        assert_eq!(uuid.timestamp(), Some(expected_time));
    }

    #[test]
    fn test_get_timestamp_returns_none_for_non_v7() {
        let mut v4_uuid = Uuid::default();
        v4_uuid.0[6] = (v4_uuid.0[6] & 0x0F) | 0x40; // Set version to 4
        v4_uuid.0[8] = (v4_uuid.0[8] & 0x3F) | 0x80; // Set variant to RFC 4122
        assert_eq!(v4_uuid.timestamp(), None);
    }

    #[test]
    fn test_generated_uuids_are_unique() {
        let mut uuids = HashSet::new();
        for _ in 0..10000 {
            let uuid = generate();
            assert!(uuids.insert(uuid), "duplicate UUID found");
        }
    }

    #[test]
    fn test_to_string_produces_standard_format() {
        let uuid = generate();
        let s = uuid.to_string();

        assert_eq!(s.len(), 36);
        assert_eq!(&s[8..9], "-");
        assert_eq!(&s[13..14], "-");
        assert_eq!(&s[18..19], "-");
        assert_eq!(&s[23..24], "-");
        assert!(s.chars().nth(14).unwrap() == '7');
    }

    #[test]
    fn test_timestamp_preserves_millisecond_precision() {
        let test_times = [0i64, 1, 1234567890123, 281474976710655];

        for &test_time in &test_times {
            let uuid = generate_with_clock(|| test_time);
            assert_eq!(uuid.timestamp(), Some(test_time));
        }
    }

    #[test]
    fn test_uniqueness_under_high_load() {
        let fixed_time = 1234567890000i64;
        let mut uuids = HashSet::new();

        for _ in 0..10000 {
            let uuid = generate_with_clock(|| fixed_time);
            assert!(uuids.insert(uuid), "duplicate UUID found");
        }
    }

    // Monotonic tests

    #[test]
    fn test_monotonic_generate_creates_valid_uuid() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let uuid = generate_monotonic();
        assert_eq!(uuid.version(), 7);
        assert_eq!(uuid.variant(), 2);
    }

    #[test]
    fn test_monotonic_generate_with_custom_clock() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let fixed_time = 1234567890000i64;
        let uuid = generate_monotonic_with_clock(|| fixed_time);
        assert_eq!(uuid.timestamp(), Some(fixed_time));
    }

    #[test]
    fn test_monotonic_generated_uuids_are_time_sorted() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let time1 = 1000000000000i64;
        let time2 = 2000000000000i64;

        let uuid1 = generate_monotonic_with_clock(|| time1);
        let uuid2 = generate_monotonic_with_clock(|| time2);

        assert!(uuid1 < uuid2, "earlier UUID should sort before later UUID");
    }

    #[test]
    fn test_monotonic_ensures_strict_ordering() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let call_count = AtomicI64::new(0);

        let uuids: Vec<Uuid> = (0..100)
            .map(|_| {
                generate_monotonic_with_clock(|| {
                    1234567890000 + call_count.fetch_add(1, Ordering::SeqCst) / 4000
                })
            })
            .collect();

        for i in 0..uuids.len() - 1 {
            assert!(
                uuids[i] < uuids[i + 1],
                "UUID at index {} should be less than UUID at index {}",
                i,
                i + 1
            );
        }
    }

    #[test]
    fn test_monotonic_advances_timestamp_on_counter_overflow() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let timestamp = AtomicI64::new(1234567890000);

        let clock = || timestamp.fetch_add(1, Ordering::SeqCst) + 1;

        let uuids: Vec<Uuid> = (0..5000)
            .map(|_| generate_monotonic_with_clock(clock))
            .collect();

        let unique: HashSet<Uuid> = uuids.iter().copied().collect();
        assert_eq!(unique.len(), 5000);

        for i in 0..uuids.len() - 1 {
            assert!(uuids[i] < uuids[i + 1]);
        }
    }

    // Compact string tests

    #[test]
    fn test_compact_string_produces_fixed_length() {
        let uuid = generate();
        let compact = uuid.to_compact_string();
        assert_eq!(compact.len(), 22);
    }

    #[test]
    fn test_compact_string_round_trip() {
        for _ in 0..1000 {
            let original = generate();
            let compact = original.to_compact_string();
            let decoded = from_compact_string(&compact).unwrap();
            assert_eq!(original, decoded);
        }
    }

    #[test]
    fn test_compact_string_round_trip_monotonic() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        for _ in 0..1000 {
            let original = generate_monotonic();
            let compact = original.to_compact_string();
            let decoded = from_compact_string(&compact).unwrap();
            assert_eq!(original, decoded);
        }
    }

    #[test]
    fn test_zero_uuid_converts_correctly() {
        let zero = Uuid::default();
        let compact = to_compact_string(&zero);
        assert_eq!(compact, "0000000000000000000000");

        let decoded = from_compact_string(&compact).unwrap();
        assert_eq!(decoded, zero);
    }

    #[test]
    fn test_max_uuid_converts_correctly() {
        let max = Uuid([0xFF; 16]);
        let compact = to_compact_string(&max);
        assert_eq!(compact.len(), 22);

        let decoded = from_compact_string(&compact).unwrap();
        assert_eq!(decoded, max);
    }

    #[test]
    fn test_compact_string_preserves_lexicographic_ordering() {
        let mut uuids: Vec<Uuid> = Vec::new();
        let mut compact_strings: Vec<String> = Vec::new();

        for ts in (1000000000000i64..1000000001000).step_by(100) {
            let timestamp = ts;
            let uuid = generate_with_clock(move || timestamp);
            compact_strings.push(uuid.to_compact_string());
            uuids.push(uuid);
        }

        let mut sorted_uuids = uuids.clone();
        sorted_uuids.sort();
        assert_eq!(uuids, sorted_uuids);

        let mut sorted_strings = compact_strings.clone();
        sorted_strings.sort();
        assert_eq!(compact_strings, sorted_strings);
    }

    #[test]
    fn test_compact_string_only_uses_alphabet() {
        for _ in 0..100 {
            let uuid = generate();
            let compact = uuid.to_compact_string();
            assert!(compact.chars().all(|c| c.is_ascii_alphanumeric()));
        }
    }

    #[test]
    fn test_from_compact_string_invalid_length() {
        let result = from_compact_string("tooshort");
        assert!(matches!(result, Err(ParseError::InvalidLength { .. })));
    }

    #[test]
    fn test_from_compact_string_invalid_character() {
        let result = from_compact_string("invalid@characters1234");
        assert!(matches!(result, Err(ParseError::InvalidCharacter(_))));
    }

    #[test]
    fn test_known_value_conversion() {
        let known = from_string("01234567-89ab-7def-8012-3456789abcde").unwrap();
        let compact = to_compact_string(&known);
        let decoded = from_compact_string(&compact).unwrap();
        assert_eq!(decoded, known);
    }

    #[test]
    fn test_generate_compact_string() {
        let compact = generate_compact_string();
        assert_eq!(compact.len(), 22);
        assert!(from_compact_string(&compact).is_ok());
    }

    // String parsing tests

    #[test]
    fn test_from_string() {
        let uuid = generate();
        let s = uuid.to_string();
        let parsed = from_string(&s).unwrap();
        assert_eq!(parsed, uuid);
    }

    #[test]
    fn test_from_string_invalid_length() {
        let result = from_string("invalid");
        assert!(matches!(result, Err(ParseError::InvalidLength { .. })));
    }

    #[test]
    fn test_from_string_invalid_format() {
        let result = from_string("01234567089ab07def080120345678abcde");
        assert!(matches!(result, Err(ParseError::InvalidLength { .. })));
    }

    #[test]
    fn test_from_str_trait() {
        let uuid = generate();
        let s = uuid.to_string();
        let parsed: Uuid = s.parse().unwrap();
        assert_eq!(parsed, uuid);
    }

    // FromBytes tests

    #[test]
    fn test_from_bytes() {
        let uuid = generate();
        let bytes = uuid.to_bytes();
        let parsed = Uuid::from_bytes(bytes);
        assert_eq!(parsed, uuid);
    }

    #[test]
    fn test_from_slice() {
        let uuid = generate();
        let bytes = uuid.as_bytes();
        let parsed = Uuid::from_slice(bytes).unwrap();
        assert_eq!(parsed, uuid);
    }

    #[test]
    fn test_from_slice_invalid_length() {
        let result = Uuid::from_slice(&[1, 2, 3]);
        assert!(result.is_none());
    }

    // Time method test

    #[test]
    fn test_time_method() {
        let fixed_time = 1234567890123i64;
        let uuid = generate_with_clock(|| fixed_time);
        let time = uuid.time().unwrap();
        assert_eq!(
            time.duration_since(UNIX_EPOCH).unwrap().as_millis() as i64,
            fixed_time
        );
    }

    // Concurrency test

    #[test]
    fn test_concurrent_generate() {
        const THREADS: usize = 10;
        const UUIDS_PER_THREAD: usize = 1000;

        let handles: Vec<_> = (0..THREADS)
            .map(|_| {
                thread::spawn(|| {
                    (0..UUIDS_PER_THREAD)
                        .map(|_| generate())
                        .collect::<Vec<_>>()
                })
            })
            .collect();

        let all_uuids: HashSet<Uuid> = handles
            .into_iter()
            .flat_map(|h| h.join().unwrap())
            .collect();

        assert_eq!(all_uuids.len(), THREADS * UUIDS_PER_THREAD);
    }

    #[test]
    fn test_concurrent_generate_monotonic() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        const THREADS: usize = 10;
        const UUIDS_PER_THREAD: usize = 100;

        let handles: Vec<_> = (0..THREADS)
            .map(|_| {
                thread::spawn(|| {
                    (0..UUIDS_PER_THREAD)
                        .map(|_| generate_monotonic())
                        .collect::<Vec<_>>()
                })
            })
            .collect();

        let all_uuids: HashSet<Uuid> = handles
            .into_iter()
            .flat_map(|h| h.join().unwrap())
            .collect();

        assert_eq!(all_uuids.len(), THREADS * UUIDS_PER_THREAD);
    }

    // Additional edge case tests

    #[test]
    fn test_compare() {
        let uuid1 = generate_with_clock(|| 1000000000000i64);
        let uuid2 = generate_with_clock(|| 2000000000000i64);

        assert!(uuid1 < uuid2);
        assert!(uuid2 > uuid1);
        assert_eq!(uuid1.cmp(&uuid1), std::cmp::Ordering::Equal);
    }

    #[test]
    fn test_monotonic_uniqueness_under_high_load() {
        let _guard = MONOTONIC_TEST_MUTEX.lock().unwrap();
        reset_monotonic_state();
        let timestamp = AtomicI64::new(1234567890000);

        let mut uuids = HashSet::new();
        for _ in 0..10000 {
            let uuid =
                generate_monotonic_with_clock(|| timestamp.fetch_add(1, Ordering::SeqCst) + 1);
            assert!(uuids.insert(uuid), "duplicate UUID found");
        }

        assert_eq!(uuids.len(), 10000);
    }

    #[test]
    fn test_uuid_debug_format() {
        let uuid = generate();
        let debug = format!("{:?}", uuid);
        assert!(debug.starts_with("Uuid("));
        assert!(debug.ends_with(")"));
    }

    #[test]
    fn test_uuid_display_format() {
        let uuid = generate();
        let display = format!("{}", uuid);
        assert_eq!(display.len(), 36);
    }

    #[test]
    fn test_uuid_hash() {
        let uuid1 = generate();
        let uuid2 = uuid1;

        let mut set = HashSet::new();
        set.insert(uuid1);
        assert!(set.contains(&uuid2));
    }
}
