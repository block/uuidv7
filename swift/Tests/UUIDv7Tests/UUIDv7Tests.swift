import XCTest
@testable import UUIDv7

final class UUIDv7Tests: XCTestCase {
    
    func testGenerateCreatesValidUuid() {
        let uuid = UUIDv7.generate()
        
        XCTAssertEqual(uuid.version, 7)
        XCTAssertEqual(uuid.variant, 2)
    }
    
    func testGenerateWithCustomClock() {
        let fixedTime: UInt64 = 1234567890000
        let uuid = UUIDv7.generate(clock: { fixedTime })
        
        XCTAssertEqual(try UUIDv7.getTimestamp(uuid), fixedTime)
    }
    
    func testGetTimestampExtractsCorrectValue() {
        let expectedTime = UInt64(Date().timeIntervalSince1970 * 1000)
        let uuid = UUIDv7.generate(clock: { expectedTime })
        
        let actualTime = try! UUIDv7.getTimestamp(uuid)
        
        XCTAssertEqual(actualTime, expectedTime)
    }
    
    func testGetTimestampThrowsOnNonV7Uuid() {
        let v4Uuid = UUID()
        
        XCTAssertThrowsError(try UUIDv7.getTimestamp(v4Uuid)) { error in
            guard case UUIDv7Error.notVersion7 = error else {
                XCTFail("Expected notVersion7 error")
                return
            }
        }
    }
    
    func testGeneratedUuidsAreUnique() {
        var uuids = Set<UUID>()
        
        for _ in 0..<10000 {
            let uuid = UUIDv7.generate()
            XCTAssertTrue(uuids.insert(uuid).inserted, "UUID should be unique")
        }
    }
    
    func testToStringProducesStandardFormat() {
        let uuid = UUIDv7.generate()
        let str = uuid.uuidString.lowercased()
        
        let pattern = #"^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"#
        XCTAssertNotNil(str.range(of: pattern, options: .regularExpression))
    }
    
    func testTimestampPreservesMillisecondPrecision() {
        let testTimes: [UInt64] = [
            0,
            1,
            1234567890123,
            281474976710655
        ]
        
        for testTime in testTimes {
            let uuid = UUIDv7.generate(clock: { testTime })
            let extractedTime = try! UUIDv7.getTimestamp(uuid)
            
            XCTAssertEqual(extractedTime, testTime, "Timestamp should be preserved exactly for \(testTime)")
        }
    }
    
    func testGenerateMethodWorks() {
        var uuids = Set<UUID>()
        
        for _ in 0..<1000 {
            let uuid = UUIDv7.generate()
            XCTAssertEqual(uuid.version, 7)
            XCTAssertEqual(uuid.variant, 2)
            uuids.insert(uuid)
        }
        
        XCTAssertEqual(uuids.count, 1000)
    }
    
    func testGenerateDoesNotBlockOrGuaranteeOrdering() {
        let fixedTime: UInt64 = 1234567890000
        var uuids = Set<UUID>()
        
        for _ in 0..<1000 {
            let uuid = UUIDv7.generate(clock: { fixedTime })
            XCTAssertEqual(try UUIDv7.getTimestamp(uuid), fixedTime)
            uuids.insert(uuid)
        }
        
        XCTAssertEqual(uuids.count, 1000)
    }
    
    func testUniquenessUnderHighLoad() {
        let fixedTime: UInt64 = 1234567890000
        var uuids = Set<UUID>()
        
        for _ in 0..<10000 {
            uuids.insert(UUIDv7.generate(clock: { fixedTime }))
        }
        
        XCTAssertEqual(uuids.count, 10000)
    }
}
