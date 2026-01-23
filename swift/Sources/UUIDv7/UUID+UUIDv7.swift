import Foundation

extension UUID {
    
    /// The version number of this UUID.
    ///
    /// Returns the 4-bit version field from bits 48-51.
    public var version: Int {
        let (mostSigBits, _) = UUIDv7.bitsFromUUID(self)
        return Int((mostSigBits >> 12) & 0xF)
    }
    
    /// The variant number of this UUID.
    ///
    /// Returns 2 for RFC 4122 variant (binary: 10x), which is standard for UUID v7.
    public var variant: Int {
        let (_, leastSigBits) = UUIDv7.bitsFromUUID(self)
        let variantBits = leastSigBits >> 62
        
        // RFC 4122 variant has top 2 bits as 10
        if variantBits & 0b10 == 0b10 {
            return 2
        } else if variantBits == 0 {
            return 0
        } else {
            return Int(variantBits)
        }
    }
    
    /// The timestamp of this UUID v7 in milliseconds since Unix epoch.
    ///
    /// - Throws: `UUIDv7Error.notVersion7` if this UUID is not version 7
    public var timestamp: UInt64 {
        get throws {
            try UUIDv7.getTimestamp(self)
        }
    }
    
    /// The compact Base62 string representation of this UUID.
    ///
    /// The resulting string is exactly 22 characters long and preserves lexicographic
    /// ordering for UUID v7 values.
    public var compactString: String {
        UUIDv7.toCompactString(self)
    }
    
    /// Creates a UUID from a compact Base62 string.
    ///
    /// - Parameter compactString: A 22-character Base62 encoded string
    /// - Throws: `UUIDv7Error` if the string is invalid
    public init(compactString: String) throws {
        self = try UUIDv7.fromCompactString(compactString)
    }
}

extension UUID: @retroactive Comparable {
    
    public static func < (lhs: UUID, rhs: UUID) -> Bool {
        let (lhsMsb, lhsLsb) = UUIDv7.bitsFromUUID(lhs)
        let (rhsMsb, rhsLsb) = UUIDv7.bitsFromUUID(rhs)
        
        // Compare as signed to match Java's UUID.compareTo behavior
        let lhsMsbSigned = Int64(bitPattern: lhsMsb)
        let rhsMsbSigned = Int64(bitPattern: rhsMsb)
        
        if lhsMsbSigned != rhsMsbSigned {
            return lhsMsbSigned < rhsMsbSigned
        }
        
        let lhsLsbSigned = Int64(bitPattern: lhsLsb)
        let rhsLsbSigned = Int64(bitPattern: rhsLsb)
        
        return lhsLsbSigned < rhsLsbSigned
    }
}
