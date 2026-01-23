import Foundation

/// Utility for generating and working with UUID v7 identifiers.
///
/// UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
/// in the most significant 48 bits, making UUIDs naturally sortable by creation time.
/// This implementation follows RFC 9562.
///
/// This struct uses non-cryptographic random for maximum performance with no synchronization overhead.
/// UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
/// is guaranteed through random bits. For monotonic ordering guarantees, use
/// `MonotonicUUIDv7` instead.
public enum UUIDv7 {
    
    public typealias Clock = () -> UInt64
    
    /// Generates a new UUID v7 using the current system time.
    ///
    /// Uses non-cryptographic random for maximum performance with no synchronization overhead.
    /// UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
    /// is guaranteed through random bits.
    ///
    /// - Returns: A new UUID v7 instance
    public static func generate() -> UUID {
        generate(clock: systemClock)
    }
    
    /// Generates a new UUID v7 using a custom clock source.
    ///
    /// Uses non-cryptographic random for maximum performance with no synchronization overhead.
    /// Useful for testing or specialized use cases where you need control over the timestamp.
    ///
    /// - Parameter clock: A closure that returns the current time in milliseconds since Unix epoch
    /// - Returns: A new UUID v7 instance
    public static func generate(clock: Clock) -> UUID {
        let timestamp = clock()
        let randA = UInt16.random(in: 0...0xFFF)
        let randB = UInt64.random(in: 0...UInt64.max)
        return build(timestamp: timestamp, randA: randA, randB: randB)
    }
    
    /// Generates a new UUID v7 as a compact string using the current system time.
    ///
    /// Equivalent to calling `toCompactString(generate())`.
    /// Returns a 22-character Base62 encoded string that preserves time-ordering.
    ///
    /// - Returns: A 22-character compact string representation of a new UUID v7
    public static func generateCompactString() -> String {
        toCompactString(generate())
    }
    
    /// Generates a new UUID v7 as a compact string using a custom clock source.
    ///
    /// Equivalent to calling `toCompactString(generate(clock:))`.
    /// Returns a 22-character Base62 encoded string that preserves time-ordering.
    ///
    /// - Parameter clock: A closure that returns the current time in milliseconds since Unix epoch
    /// - Returns: A 22-character compact string representation of a new UUID v7
    public static func generateCompactString(clock: Clock) -> String {
        toCompactString(generate(clock: clock))
    }
    
    /// Builds a UUID v7 from timestamp and random components.
    ///
    /// - Parameters:
    ///   - timestamp: The timestamp in milliseconds since Unix epoch
    ///   - randA: The random or counter value for bits 52-63 (12 bits)
    ///   - randB: The random value for bits 66-127 (62 bits, variant will be set)
    /// - Returns: A new UUID v7 instance
    static func build(timestamp: UInt64, randA: UInt16, randB: UInt64) -> UUID {
        let maskedRandA = UInt64(randA & 0xFFF)
        var mostSigBits = (timestamp << 16) | maskedRandA
        var leastSigBits = randB
        
        // Set version to 7 (0111 in bits 48-51)
        mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFF) | 0x0000000000007000
        
        // Set variant to 10 (RFC 4122) in bits 64-65
        leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFF) | 0x8000000000000000
        
        return uuidFromBits(mostSigBits: mostSigBits, leastSigBits: leastSigBits)
    }
    
    /// Extracts the timestamp component from a UUID v7.
    ///
    /// - Parameter uuid: The UUID v7 to extract the timestamp from
    /// - Returns: The timestamp in milliseconds since Unix epoch
    /// - Throws: `UUIDv7Error.notVersion7` if the UUID is not version 7
    public static func getTimestamp(_ uuid: UUID) throws -> UInt64 {
        let (mostSigBits, _) = bitsFromUUID(uuid)
        
        let version = Int((mostSigBits >> 12) & 0xF)
        guard version == 7 else {
            throw UUIDv7Error.notVersion7(actualVersion: version)
        }
        
        return mostSigBits >> 16
    }
    
    /// Default system clock returning milliseconds since Unix epoch.
    public static let systemClock: Clock = {
        UInt64(Date().timeIntervalSince1970 * 1000)
    }
}

// MARK: - Compact String Encoding

extension UUIDv7 {
    
    private static let base62Alphabet = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
    private static let base62Length = 22
    
    /// Converts a UUID to a compact string representation.
    ///
    /// The resulting string is exactly 22 characters long and preserves lexicographic
    /// ordering for UUID v7 values (time-ordered UUIDs will sort correctly as compact strings).
    /// Uses Base62 encoding (0-9, A-Z, a-z).
    ///
    /// - Parameter uuid: The UUID to encode
    /// - Returns: A 22-character compact string representation
    public static func toCompactString(_ uuid: UUID) -> String {
        let (mostSigBits, leastSigBits) = bitsFromUUID(uuid)
        
        // Convert to two UInt64s and then to base62
        var high = mostSigBits
        var low = leastSigBits
        
        var result = [Character](repeating: "0", count: base62Length)
        
        // Process from least significant digit
        for i in stride(from: base62Length - 1, through: 0, by: -1) {
            // Divide 128-bit number by 62
            let (newHigh, newLow, remainder) = divide128By62(high: high, low: low)
            result[i] = base62Alphabet[Int(remainder)]
            high = newHigh
            low = newLow
        }
        
        return String(result)
    }
    
    /// Decodes a compact string representation back to a UUID.
    ///
    /// - Parameter compactString: The compact string to decode (must be 22 characters)
    /// - Returns: The decoded UUID
    /// - Throws: `UUIDv7Error` if the string is invalid
    public static func fromCompactString(_ compactString: String) throws -> UUID {
        guard compactString.count == base62Length else {
            throw UUIDv7Error.invalidCompactStringLength(length: compactString.count)
        }
        
        var high: UInt64 = 0
        var low: UInt64 = 0
        
        for char in compactString {
            guard let digit = base62Value(of: char) else {
                throw UUIDv7Error.invalidCompactStringCharacter(character: char)
            }
            
            // Multiply 128-bit number by 62 and add digit
            (high, low) = multiply128By62AndAdd(high: high, low: low, add: UInt64(digit))
        }
        
        return uuidFromBits(mostSigBits: high, leastSigBits: low)
    }
    
    private static func base62Value(of char: Character) -> Int? {
        switch char {
        case "0"..."9":
            return Int(char.asciiValue! - Character("0").asciiValue!)
        case "A"..."Z":
            return Int(char.asciiValue! - Character("A").asciiValue!) + 10
        case "a"..."z":
            return Int(char.asciiValue! - Character("a").asciiValue!) + 36
        default:
            return nil
        }
    }
    
    private static func divide128By62(high: UInt64, low: UInt64) -> (high: UInt64, low: UInt64, remainder: UInt64) {
        // Divide 128-bit number by 62 using long division
        let divisor: UInt64 = 62
        
        // Divide high part
        let highQuotient = high / divisor
        let highRemainder = high % divisor
        
        // Combine remainder with low part
        // (highRemainder << 64) + low, divide by 62
        // Since highRemainder < 62, we can use a simpler approach
        
        let (lowQuotient, lowRemainder) = divideWithCarry(
            high: highRemainder,
            low: low,
            divisor: divisor
        )
        
        return (highQuotient, lowQuotient, lowRemainder)
    }
    
    private static func divideWithCarry(high: UInt64, low: UInt64, divisor: UInt64) -> (quotient: UInt64, remainder: UInt64) {
        // Split low into two 32-bit parts for easier handling
        let lowHigh = low >> 32
        let lowLow = low & 0xFFFFFFFF
        
        // Combine high remainder with upper 32 bits of low
        let upper = (high << 32) | lowHigh
        let upperQuotient = upper / divisor
        let upperRemainder = upper % divisor
        
        // Combine upper remainder with lower 32 bits
        let lower = (upperRemainder << 32) | lowLow
        let lowerQuotient = lower / divisor
        let lowerRemainder = lower % divisor
        
        // Combine quotients
        let quotient = (upperQuotient << 32) | lowerQuotient
        
        return (quotient, lowerRemainder)
    }
    
    private static func multiply128By62AndAdd(high: UInt64, low: UInt64, add: UInt64) -> (high: UInt64, low: UInt64) {
        let multiplier: UInt64 = 62
        
        // Multiply low part
        let (lowProduct, lowOverflow) = low.multipliedReportingOverflow(by: multiplier)
        var lowCarry: UInt64 = 0
        if lowOverflow {
            // Calculate the high part of the multiplication manually
            lowCarry = low.multipliedFullWidth(by: multiplier).high
        }
        
        // Multiply high part and add carry from low
        let (highProduct, _) = high.multipliedReportingOverflow(by: multiplier)
        let newHigh = highProduct &+ lowCarry
        
        // Add the digit
        let (newLow, addOverflow) = lowProduct.addingReportingOverflow(add)
        let finalHigh = addOverflow ? newHigh &+ 1 : newHigh
        
        return (finalHigh, newLow)
    }
}

// MARK: - UUID Bit Manipulation

extension UUIDv7 {
    
    static func bitsFromUUID(_ uuid: UUID) -> (mostSigBits: UInt64, leastSigBits: UInt64) {
        var bytes = uuid.uuid
        
        // UUID bytes are stored in big-endian order
        let mostSigBits = withUnsafeBytes(of: &bytes) { ptr -> UInt64 in
            var value: UInt64 = 0
            for i in 0..<8 {
                value = (value << 8) | UInt64(ptr[i])
            }
            return value
        }
        
        let leastSigBits = withUnsafeBytes(of: &bytes) { ptr -> UInt64 in
            var value: UInt64 = 0
            for i in 8..<16 {
                value = (value << 8) | UInt64(ptr[i])
            }
            return value
        }
        
        return (mostSigBits, leastSigBits)
    }
    
    static func uuidFromBits(mostSigBits: UInt64, leastSigBits: UInt64) -> UUID {
        var bytes = (UInt8(0), UInt8(0), UInt8(0), UInt8(0),
                     UInt8(0), UInt8(0), UInt8(0), UInt8(0),
                     UInt8(0), UInt8(0), UInt8(0), UInt8(0),
                     UInt8(0), UInt8(0), UInt8(0), UInt8(0))
        
        // Store in big-endian order
        bytes.0 = UInt8((mostSigBits >> 56) & 0xFF)
        bytes.1 = UInt8((mostSigBits >> 48) & 0xFF)
        bytes.2 = UInt8((mostSigBits >> 40) & 0xFF)
        bytes.3 = UInt8((mostSigBits >> 32) & 0xFF)
        bytes.4 = UInt8((mostSigBits >> 24) & 0xFF)
        bytes.5 = UInt8((mostSigBits >> 16) & 0xFF)
        bytes.6 = UInt8((mostSigBits >> 8) & 0xFF)
        bytes.7 = UInt8(mostSigBits & 0xFF)
        
        bytes.8 = UInt8((leastSigBits >> 56) & 0xFF)
        bytes.9 = UInt8((leastSigBits >> 48) & 0xFF)
        bytes.10 = UInt8((leastSigBits >> 40) & 0xFF)
        bytes.11 = UInt8((leastSigBits >> 32) & 0xFF)
        bytes.12 = UInt8((leastSigBits >> 24) & 0xFF)
        bytes.13 = UInt8((leastSigBits >> 16) & 0xFF)
        bytes.14 = UInt8((leastSigBits >> 8) & 0xFF)
        bytes.15 = UInt8(leastSigBits & 0xFF)
        
        return UUID(uuid: bytes)
    }
}

// MARK: - Errors

public enum UUIDv7Error: Error, Equatable {
    case notVersion7(actualVersion: Int)
    case invalidCompactStringLength(length: Int)
    case invalidCompactStringCharacter(character: Character)
}

extension UUIDv7Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .notVersion7(let actualVersion):
            return "UUID is not version 7 (got version \(actualVersion))"
        case .invalidCompactStringLength(let length):
            return "Compact string must be exactly 22 characters (got \(length))"
        case .invalidCompactStringCharacter(let character):
            return "Invalid compact string character: \(character)"
        }
    }
}
