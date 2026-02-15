import XCTest
@testable import UUIDv7

final class MonotonicUUIDv7Tests: XCTestCase {
    
    var generator: MonotonicUUIDv7!
    
    override func setUp() {
        super.setUp()
        generator = MonotonicUUIDv7()
    }
    
    func testGenerateCreatesValidUuid() {
        let uuid = generator.generate()
        
        XCTAssertEqual(uuid.version, 7)
        XCTAssertEqual(uuid.variant, 2)
    }
    
    func testGenerateWithCustomClock() {
        let fixedTime: UInt64 = 1234567890000
        let uuid = generator.generate(clock: { fixedTime })
        
        XCTAssertEqual(try UUIDv7.getTimestamp(uuid), fixedTime)
    }
    
    func testGetTimestampExtractsCorrectValue() {
        let expectedTime = UInt64(Date().timeIntervalSince1970 * 1000)
        let uuid = generator.generate(clock: { expectedTime })
        
        let actualTime = try! UUIDv7.getTimestamp(uuid)
        
        XCTAssertEqual(actualTime, expectedTime)
    }
    
    func testGeneratedUuidsAreTimeSorted() {
        let time1: UInt64 = 1000000000000
        let time2: UInt64 = 2000000000000
        
        let gen1 = MonotonicUUIDv7()
        let uuid1 = gen1.generate(clock: { time1 })
        let uuid2 = gen1.generate(clock: { time2 })
        
        XCTAssertLessThan(uuid1, uuid2, "Earlier UUID should sort before later UUID")
    }
    
    func testTimestampPreservesMillisecondPrecision() {
        let testTimes: [UInt64] = [
            0,
            1,
            1234567890123,
            281474976710655
        ]
        
        for testTime in testTimes {
            let gen = MonotonicUUIDv7()
            let uuid = gen.generate(clock: { testTime })
            let extractedTime = try! UUIDv7.getTimestamp(uuid)
            
            XCTAssertEqual(extractedTime, testTime, "Timestamp should be preserved exactly for \(testTime)")
        }
    }
    
    func testGenerateMonotonicMethodWorks() {
        var uuids = [UUID]()
        
        for _ in 0..<50 {
            uuids.append(generator.generate())
        }
        
        let uniqueUuids = Set(uuids)
        XCTAssertEqual(uniqueUuids.count, 50)
        
        for uuid in uniqueUuids {
            XCTAssertEqual(uuid.version, 7)
            XCTAssertEqual(uuid.variant, 2)
        }
    }
    
    func testGenerateEnsuresStrictOrdering() {
        let fixedTime: UInt64 = 1234567890000
        var uuids = [UUID]()
        
        for _ in 0..<100 {
            uuids.append(generator.generate(clock: { fixedTime }))
        }
        
        for i in 0..<(uuids.count - 1) {
            XCTAssertLessThan(uuids[i], uuids[i + 1], "UUID at index \(i) should be less than UUID at index \(i + 1)")
        }
    }
    
    func testGenerateAdvancesTimestampOnCounterOverflow() {
        var timestamp: UInt64 = 1234567890000
        var uuids = [UUID]()
        
        for _ in 0..<5000 {
            uuids.append(generator.generate(clock: {
                timestamp += 1
                return timestamp
            }))
        }
        
        let uniqueUuids = Set(uuids)
        XCTAssertEqual(uniqueUuids.count, 5000)
        
        let maxTimestamp = uuids.compactMap { try? UUIDv7.getTimestamp($0) }.max() ?? 0
        XCTAssertGreaterThan(maxTimestamp, 1234567890000)
        
        for i in 0..<(uuids.count - 1) {
            XCTAssertLessThan(uuids[i], uuids[i + 1], "UUID at index \(i) should be less than UUID at index \(i + 1)")
        }
    }
    
    func testGenerateWithCustomClockEnsuresOrdering() {
        let fixedTime: UInt64 = 1234567890000
        var uuids = [UUID]()
        
        for _ in 0..<50 {
            uuids.append(generator.generate(clock: { fixedTime }))
        }
        
        for i in 0..<(uuids.count - 1) {
            XCTAssertLessThan(uuids[i], uuids[i + 1])
        }
    }
    
    func testGenerateResetsCounterOnNewTimestamp() {
        var timestamp: UInt64 = 1000000000000
        
        let uuid1 = generator.generate(clock: { timestamp })
        let uuid2 = generator.generate(clock: { timestamp })
        
        timestamp = 2000000000000
        
        let uuid3 = generator.generate(clock: { timestamp })
        
        let ts1 = try! UUIDv7.getTimestamp(uuid1)
        let ts2 = try! UUIDv7.getTimestamp(uuid2)
        let ts3 = try! UUIDv7.getTimestamp(uuid3)
        
        XCTAssertGreaterThan(ts3, ts1)
        XCTAssertGreaterThan(ts3, ts2)
        XCTAssertLessThan(uuid1, uuid3)
        XCTAssertLessThan(uuid2, uuid3)
    }
    
    func testGenerateHandlesBackwardClock() {
        let highTime: UInt64 = 2000000000000
        let lowTime: UInt64 = 1000000000000
        var timestamp = highTime

        // Generate at a high timestamp
        let uuid1 = generator.generate(clock: { timestamp })
        let uuid2 = generator.generate(clock: { timestamp })

        // Move clock backward
        timestamp = lowTime
        let uuid3 = generator.generate(clock: { timestamp })

        // Timestamp should be clamped to the original value
        XCTAssertGreaterThanOrEqual(
            try! UUIDv7.getTimestamp(uuid3),
            try! UUIDv7.getTimestamp(uuid1),
            "Backward clock should clamp timestamp"
        )

        // Monotonic ordering must be maintained
        XCTAssertLessThan(uuid1, uuid2, "uuid1 should sort before uuid2")
        XCTAssertLessThan(uuid2, uuid3, "uuid2 should sort before uuid3 despite backward clock")
    }

    func testUniquenessUnderHighLoad() {
        var timestamp: UInt64 = 1234567890000
        var uuids = Set<UUID>()
        
        for _ in 0..<10000 {
            uuids.insert(generator.generate(clock: {
                timestamp += 1
                return timestamp
            }))
        }
        
        XCTAssertEqual(uuids.count, 10000)
        
        let maxTimestamp = uuids.compactMap { try? UUIDv7.getTimestamp($0) }.max() ?? 0
        XCTAssertGreaterThan(maxTimestamp, 1234567890000)
    }
    
    func testStaticGenerateUsesSharedInstance() {
        let uuid1 = MonotonicUUIDv7.generate()
        let uuid2 = MonotonicUUIDv7.generate()
        
        XCTAssertEqual(uuid1.version, 7)
        XCTAssertEqual(uuid2.version, 7)
        XCTAssertNotEqual(uuid1, uuid2)
    }
}
