import XCTest
@testable import UUIDv7

final class CompactStringTests: XCTestCase {
    
    func testGenerateCompactStringProducesFixedLength() {
        let uuid = UUIDv7.generate()
        let compactString = UUIDv7.toCompactString(uuid)
        
        XCTAssertEqual(compactString.count, 22)
    }
    
    func testFromCompactStringThrowsOnInvalidLength() {
        XCTAssertThrowsError(try UUIDv7.fromCompactString("tooshort")) { error in
            guard case UUIDv7Error.invalidCompactStringLength = error else {
                XCTFail("Expected invalidCompactStringLength error")
                return
            }
        }
    }
    
    func testFromCompactStringThrowsOnInvalidCharacter() {
        XCTAssertThrowsError(try UUIDv7.fromCompactString("invalid@characters1234")) { error in
            guard case UUIDv7Error.invalidCompactStringCharacter = error else {
                XCTFail("Expected invalidCompactStringCharacter error")
                return
            }
        }
    }
    
    func testRoundTripConversion() {
        let original = UUIDv7.generate()
        let compactString = UUIDv7.toCompactString(original)
        let decoded = try! UUIDv7.fromCompactString(compactString)
        
        XCTAssertEqual(decoded, original)
    }
    
    func testRoundTripWithMultipleUuids() {
        for _ in 0..<1000 {
            let original = UUIDv7.generate()
            let compactString = UUIDv7.toCompactString(original)
            let decoded = try! UUIDv7.fromCompactString(compactString)
            
            XCTAssertEqual(decoded, original)
        }
    }
    
    func testRoundTripWithMonotonicUuids() {
        let generator = MonotonicUUIDv7()
        
        for _ in 0..<1000 {
            let original = generator.generate()
            let compactString = UUIDv7.toCompactString(original)
            let decoded = try! UUIDv7.fromCompactString(compactString)
            
            XCTAssertEqual(decoded, original)
        }
    }
    
    func testZeroUuidConvertsCorrectly() {
        let zero = UUIDv7.uuidFromBits(mostSigBits: 0, leastSigBits: 0)
        let compactString = UUIDv7.toCompactString(zero)
        
        XCTAssertEqual(compactString, "0000000000000000000000")
        XCTAssertEqual(try UUIDv7.fromCompactString(compactString), zero)
    }
    
    func testMaxUuidConvertsCorrectly() {
        let max = UUIDv7.uuidFromBits(mostSigBits: UInt64.max, leastSigBits: UInt64.max)
        let compactString = UUIDv7.toCompactString(max)
        
        XCTAssertEqual(compactString.count, 22)
        XCTAssertEqual(try UUIDv7.fromCompactString(compactString), max)
    }
    
    func testPreservesLexicographicOrderingForTimeOrderedUuids() {
        var uuids = [UUID]()
        var compactStrings = [String]()
        
        var ts: UInt64 = 1000000000000
        while ts < 1000000001000 {
            let timestamp = ts
            let uuid = UUIDv7.generate(clock: { timestamp })
            uuids.append(uuid)
            compactStrings.append(UUIDv7.toCompactString(uuid))
            ts += 100
        }
        
        let sortedUuids = uuids.sorted()
        XCTAssertEqual(uuids, sortedUuids)
        
        let sortedCompactStrings = compactStrings.sorted()
        XCTAssertEqual(compactStrings, sortedCompactStrings)
    }
    
    func testDifferentUuidsProduceDifferentCompactStrings() {
        let uuid1 = UUIDv7.generate()
        let uuid2 = UUIDv7.generate()
        
        let compact1 = UUIDv7.toCompactString(uuid1)
        let compact2 = UUIDv7.toCompactString(uuid2)
        
        XCTAssertNotEqual(compact1, compact2)
    }
    
    func testOnlyUsesCompactStringAlphabet() {
        let pattern = #"^[0-9A-Za-z]{22}$"#
        
        for _ in 0..<100 {
            let uuid = UUIDv7.generate()
            let compactString = UUIDv7.toCompactString(uuid)
            
            XCTAssertNotNil(compactString.range(of: pattern, options: .regularExpression))
        }
    }
    
    func testKnownValueConversion() {
        let known = UUID(uuidString: "01234567-89ab-7def-8012-3456789abcde")!
        let compactString = UUIDv7.toCompactString(known)
        
        let decoded = try! UUIDv7.fromCompactString(compactString)
        XCTAssertEqual(decoded, known)
    }
    
    func testUuidExtensionCompactString() {
        let uuid = UUIDv7.generate()
        let compactFromMethod = UUIDv7.toCompactString(uuid)
        let compactFromProperty = uuid.compactString
        
        XCTAssertEqual(compactFromMethod, compactFromProperty)
    }
    
    func testUuidInitFromCompactString() {
        let original = UUIDv7.generate()
        let compactString = original.compactString
        let decoded = try! UUID(compactString: compactString)
        
        XCTAssertEqual(decoded, original)
    }
    
    func testGenerateCompactStringDirectly() {
        let compactString = UUIDv7.generateCompactString()
        
        XCTAssertEqual(compactString.count, 22)
        
        let uuid = try! UUIDv7.fromCompactString(compactString)
        XCTAssertEqual(uuid.version, 7)
    }
    
    func testGenerateCompactStringWithClock() {
        let fixedTime: UInt64 = 1234567890000
        let compactString = UUIDv7.generateCompactString(clock: { fixedTime })
        
        XCTAssertEqual(compactString.count, 22)
        
        let uuid = try! UUIDv7.fromCompactString(compactString)
        XCTAssertEqual(try UUIDv7.getTimestamp(uuid), fixedTime)
    }
}
