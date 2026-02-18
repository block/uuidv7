import Foundation

/// Utility for generating monotonic UUID v7 identifiers.
///
/// This implementation ensures that UUIDs generated within the same millisecond are strictly
/// ordered by incrementing a counter. This provides guaranteed sequential ordering, making it
/// ideal for database primary keys and scenarios requiring chronological order guarantees.
///
/// UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
/// in the most significant 48 bits, making UUIDs naturally sortable by creation time.
/// This implementation follows RFC 9562 with monotonic counter support.
///
/// All generation methods use a lock to ensure strict ordering across threads.
/// If the counter overflows within a millisecond (after 4096 UUIDs), the method will
/// block until the next millisecond to maintain uniqueness.
public final class MonotonicUUIDv7: @unchecked Sendable {
    
    /// Shared singleton instance for convenient access.
    public static let shared = MonotonicUUIDv7()
    
    private static let counterMax: UInt16 = 0xFFF // 12 bits = 4095
    
    private let lock = NSLock()
    private var lastTimestamp: UInt64 = 0
    private var counter: UInt16 = 0
    
    public init() {}
    
    /// Generates a new monotonic UUID v7 using the current system time.
    ///
    /// This method ensures that UUIDs generated within the same millisecond are strictly
    /// ordered by incrementing a counter. If the counter overflows within a millisecond,
    /// the method will block until the next millisecond to maintain uniqueness.
    ///
    /// This method is thread-safe and best suited for database primary keys and scenarios
    /// requiring guaranteed sequential ordering.
    ///
    /// - Returns: A new UUID v7 instance
    public func generate() -> UUID {
        generate(clock: UUIDv7.systemClock)
    }
    
    /// Generates a new monotonic UUID v7 using a custom clock source.
    ///
    /// This method ensures that UUIDs generated within the same millisecond are strictly
    /// ordered by incrementing a counter. Useful for testing monotonic behavior with
    /// controlled clock sources.
    ///
    /// This method is thread-safe and best suited for database primary keys and scenarios
    /// requiring guaranteed sequential ordering.
    ///
    /// - Parameter clock: A closure that returns the current time in milliseconds since Unix epoch
    /// - Returns: A new UUID v7 instance
    public func generate(clock: UUIDv7.Clock) -> UUID {
        lock.lock()
        defer { lock.unlock() }
        
        var timestamp = clock()
        let counterValue: UInt16
        
        if timestamp <= lastTimestamp {
            // Same millisecond or clock went backward - clamp to maintain monotonicity
            timestamp = lastTimestamp
            counter = (counter + 1) & Self.counterMax
            
            if counter == 0 {
                // Counter overflow - wait for next millisecond to maintain uniqueness
                repeat {
                    timestamp = clock()
                } while timestamp <= lastTimestamp

                // New millisecond - update state and start with random counter value
                lastTimestamp = timestamp
                counter = UInt16.random(in: 0...Self.counterMax)
            }
            counterValue = counter
        } else {
            // New millisecond - start with random counter value for unpredictability
            counter = UInt16.random(in: 0...Self.counterMax)
            counterValue = counter
            lastTimestamp = timestamp
        }
        
        let randB = UInt64.random(in: 0...UInt64.max)
        
        return UUIDv7.build(timestamp: timestamp, randA: counterValue, randB: randB)
    }
    
    /// Convenience static method using the shared instance.
    ///
    /// - Returns: A new monotonic UUID v7 instance
    public static func generate() -> UUID {
        shared.generate()
    }
    
    /// Convenience static method using the shared instance with a custom clock.
    ///
    /// - Parameter clock: A closure that returns the current time in milliseconds since Unix epoch
    /// - Returns: A new monotonic UUID v7 instance
    public static func generate(clock: UUIDv7.Clock) -> UUID {
        shared.generate(clock: clock)
    }
}
