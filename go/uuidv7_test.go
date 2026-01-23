package uuidv7

import (
	"regexp"
	"sort"
	"sync"
	"sync/atomic"
	"testing"
)

func TestGenerateCreatesValidUUID(t *testing.T) {
	uuid := Generate()

	if uuid.Version() != 7 {
		t.Errorf("expected version 7, got %d", uuid.Version())
	}
	if uuid.Variant() != 2 {
		t.Errorf("expected variant 2, got %d", uuid.Variant())
	}
}

func TestGenerateWithCustomClock(t *testing.T) {
	fixedTime := int64(1234567890000)
	uuid := GenerateWithClock(func() int64 { return fixedTime })

	ts, err := uuid.Timestamp()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if ts != fixedTime {
		t.Errorf("expected timestamp %d, got %d", fixedTime, ts)
	}
}

func TestGetTimestampExtractsCorrectValue(t *testing.T) {
	expectedTime := int64(1234567890123)
	uuid := GenerateWithClock(func() int64 { return expectedTime })

	actualTime, err := uuid.Timestamp()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if actualTime != expectedTime {
		t.Errorf("expected timestamp %d, got %d", expectedTime, actualTime)
	}
}

func TestGetTimestampErrorOnNonV7UUID(t *testing.T) {
	// Create a v4-like UUID
	var v4UUID UUID
	v4UUID[6] = (v4UUID[6] & 0x0F) | 0x40 // Set version to 4
	v4UUID[8] = (v4UUID[8] & 0x3F) | 0x80 // Set variant to RFC 4122

	_, err := v4UUID.Timestamp()
	if err == nil {
		t.Error("expected error for non-v7 UUID")
	}
}

func TestGeneratedUUIDsAreUnique(t *testing.T) {
	uuids := make(map[UUID]bool)
	for i := 0; i < 10000; i++ {
		uuid := Generate()
		if uuids[uuid] {
			t.Errorf("duplicate UUID found: %s", uuid)
		}
		uuids[uuid] = true
	}
}

func TestToStringProducesStandardFormat(t *testing.T) {
	uuid := Generate()
	str := uuid.String()

	pattern := regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
	if !pattern.MatchString(str) {
		t.Errorf("UUID string does not match expected format: %s", str)
	}
}

func TestTimestampPreservesMillisecondPrecision(t *testing.T) {
	testTimes := []int64{
		0,
		1,
		1234567890123,
		281474976710655, // Max 48-bit value
	}

	for _, testTime := range testTimes {
		uuid := GenerateWithClock(func() int64 { return testTime })
		extractedTime, err := uuid.Timestamp()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if extractedTime != testTime {
			t.Errorf("timestamp not preserved for %d, got %d", testTime, extractedTime)
		}
	}
}

func TestUniquenessUnderHighLoad(t *testing.T) {
	fixedTime := int64(1234567890000)
	uuids := make(map[UUID]bool)

	for i := 0; i < 10000; i++ {
		uuid := GenerateWithClock(func() int64 { return fixedTime })
		if uuids[uuid] {
			t.Errorf("duplicate UUID found at iteration %d", i)
		}
		uuids[uuid] = true
	}
}

// Monotonic tests

func TestMonotonicGenerateCreatesValidUUID(t *testing.T) {
	ResetMonotonicState()
	uuid := GenerateMonotonic()

	if uuid.Version() != 7 {
		t.Errorf("expected version 7, got %d", uuid.Version())
	}
	if uuid.Variant() != 2 {
		t.Errorf("expected variant 2, got %d", uuid.Variant())
	}
}

func TestMonotonicGenerateWithCustomClock(t *testing.T) {
	ResetMonotonicState()
	fixedTime := int64(1234567890000)
	uuid := GenerateMonotonicWithClock(func() int64 { return fixedTime })

	ts, err := uuid.Timestamp()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if ts != fixedTime {
		t.Errorf("expected timestamp %d, got %d", fixedTime, ts)
	}
}

func TestMonotonicGeneratedUUIDsAreTimeSorted(t *testing.T) {
	ResetMonotonicState()
	time1 := int64(1000000000000)
	time2 := int64(2000000000000)

	uuid1 := GenerateMonotonicWithClock(func() int64 { return time1 })
	uuid2 := GenerateMonotonicWithClock(func() int64 { return time2 })

	if uuid1.Compare(uuid2) >= 0 {
		t.Errorf("earlier UUID should sort before later UUID")
	}
}

func TestMonotonicEnsuresStrictOrdering(t *testing.T) {
	ResetMonotonicState()
	fixedTime := int64(1234567890000)

	uuids := make([]UUID, 100)
	for i := 0; i < 100; i++ {
		uuids[i] = GenerateMonotonicWithClock(func() int64 { return fixedTime })
	}

	for i := 0; i < len(uuids)-1; i++ {
		if uuids[i].Compare(uuids[i+1]) >= 0 {
			t.Errorf("UUID at index %d should be less than UUID at index %d", i, i+1)
		}
	}
}

func TestMonotonicAdvancesTimestampOnCounterOverflow(t *testing.T) {
	ResetMonotonicState()
	timestamp := int64(1234567890000)
	var mu sync.Mutex

	clock := func() int64 {
		mu.Lock()
		defer mu.Unlock()
		timestamp++
		return timestamp
	}

	uuids := make([]UUID, 5000)
	for i := 0; i < 5000; i++ {
		uuids[i] = GenerateMonotonicWithClock(clock)
	}

	// Verify all UUIDs are unique
	uniqueMap := make(map[UUID]bool)
	for _, u := range uuids {
		if uniqueMap[u] {
			t.Error("duplicate UUID found")
		}
		uniqueMap[u] = true
	}

	// Verify UUIDs are strictly ordered
	for i := 0; i < len(uuids)-1; i++ {
		if uuids[i].Compare(uuids[i+1]) >= 0 {
			t.Errorf("UUID at index %d should be less than UUID at index %d", i, i+1)
		}
	}
}

func TestMonotonicResetsCounterOnNewTimestamp(t *testing.T) {
	ResetMonotonicState()
	timestamp := int64(1000000000000)
	var mu sync.Mutex

	clock := func() int64 {
		mu.Lock()
		defer mu.Unlock()
		return timestamp
	}

	setClock := func(ts int64) {
		mu.Lock()
		defer mu.Unlock()
		timestamp = ts
	}

	uuid1 := GenerateMonotonicWithClock(clock)
	uuid2 := GenerateMonotonicWithClock(clock)

	setClock(2000000000000)
	uuid3 := GenerateMonotonicWithClock(clock)

	ts1, _ := uuid1.Timestamp()
	ts2, _ := uuid2.Timestamp()
	ts3, _ := uuid3.Timestamp()

	if ts3 <= ts1 || ts3 <= ts2 {
		t.Error("uuid3 should have later timestamp")
	}

	if uuid1.Compare(uuid3) >= 0 {
		t.Error("uuid1 should sort before uuid3")
	}
	if uuid2.Compare(uuid3) >= 0 {
		t.Error("uuid2 should sort before uuid3")
	}
}

// Compact string tests

func TestCompactStringProducesFixedLength(t *testing.T) {
	uuid := Generate()
	compactString := uuid.CompactString()

	if len(compactString) != 22 {
		t.Errorf("expected length 22, got %d", len(compactString))
	}
}

func TestCompactStringRoundTrip(t *testing.T) {
	for i := 0; i < 1000; i++ {
		original := Generate()
		compactString := original.CompactString()
		decoded, err := FromCompactString(compactString)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if original != decoded {
			t.Errorf("round trip failed: original=%s, decoded=%s", original, decoded)
		}
	}
}

func TestCompactStringRoundTripMonotonic(t *testing.T) {
	ResetMonotonicState()
	for i := 0; i < 1000; i++ {
		original := GenerateMonotonic()
		compactString := original.CompactString()
		decoded, err := FromCompactString(compactString)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if original != decoded {
			t.Errorf("round trip failed: original=%s, decoded=%s", original, decoded)
		}
	}
}

func TestZeroUUIDConvertsCorrectly(t *testing.T) {
	var zero UUID
	compactString := ToCompactString(zero)

	if compactString != "0000000000000000000000" {
		t.Errorf("expected all zeros, got %s", compactString)
	}

	decoded, err := FromCompactString(compactString)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if decoded != zero {
		t.Errorf("decoded UUID does not match zero UUID")
	}
}

func TestMaxUUIDConvertsCorrectly(t *testing.T) {
	var max UUID
	for i := range max {
		max[i] = 0xFF
	}

	compactString := ToCompactString(max)
	if len(compactString) != 22 {
		t.Errorf("expected length 22, got %d", len(compactString))
	}

	decoded, err := FromCompactString(compactString)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if decoded != max {
		t.Errorf("decoded UUID does not match max UUID")
	}
}

func TestCompactStringPreservesLexicographicOrdering(t *testing.T) {
	var uuids []UUID
	var compactStrings []string

	for ts := int64(1000000000000); ts < 1000000001000; ts += 100 {
		timestamp := ts
		uuid := GenerateWithClock(func() int64 { return timestamp })
		uuids = append(uuids, uuid)
		compactStrings = append(compactStrings, uuid.CompactString())
	}

	// Verify UUIDs are in timestamp order
	sortedUUIDs := make([]UUID, len(uuids))
	copy(sortedUUIDs, uuids)
	sort.Slice(sortedUUIDs, func(i, j int) bool {
		return sortedUUIDs[i].Compare(sortedUUIDs[j]) < 0
	})

	for i, u := range uuids {
		if u != sortedUUIDs[i] {
			t.Error("UUIDs are not in expected order")
			break
		}
	}

	// Verify compact strings are in lexicographic order
	sortedStrings := make([]string, len(compactStrings))
	copy(sortedStrings, compactStrings)
	sort.Strings(sortedStrings)

	for i, s := range compactStrings {
		if s != sortedStrings[i] {
			t.Error("compact strings are not in lexicographic order")
			break
		}
	}
}

func TestCompactStringOnlyUsesAlphabet(t *testing.T) {
	pattern := regexp.MustCompile(`^[0-9A-Za-z]{22}$`)
	for i := 0; i < 100; i++ {
		uuid := Generate()
		compactString := uuid.CompactString()
		if !pattern.MatchString(compactString) {
			t.Errorf("compact string contains invalid characters: %s", compactString)
		}
	}
}

func TestFromCompactStringInvalidLength(t *testing.T) {
	_, err := FromCompactString("tooshort")
	if err == nil {
		t.Error("expected error for invalid length")
	}
}

func TestFromCompactStringInvalidCharacter(t *testing.T) {
	_, err := FromCompactString("invalid@characters1234")
	if err == nil {
		t.Error("expected error for invalid character")
	}
}

func TestKnownValueConversion(t *testing.T) {
	known, err := FromString("01234567-89ab-7def-8012-3456789abcde")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	compactString := ToCompactString(known)
	decoded, err := FromCompactString(compactString)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if decoded != known {
		t.Errorf("round trip failed for known value")
	}
}

func TestGenerateCompactString(t *testing.T) {
	compactString := GenerateCompactString()
	if len(compactString) != 22 {
		t.Errorf("expected length 22, got %d", len(compactString))
	}

	// Should be valid and decodable
	_, err := FromCompactString(compactString)
	if err != nil {
		t.Errorf("generated compact string is not valid: %v", err)
	}
}

// String parsing tests

func TestFromString(t *testing.T) {
	uuid := Generate()
	str := uuid.String()

	parsed, err := FromString(str)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if parsed != uuid {
		t.Errorf("parsed UUID does not match original")
	}
}

func TestFromStringInvalidLength(t *testing.T) {
	_, err := FromString("invalid")
	if err == nil {
		t.Error("expected error for invalid length")
	}
}

func TestFromStringInvalidFormat(t *testing.T) {
	_, err := FromString("01234567089ab07def080120345678abcde")
	if err == nil {
		t.Error("expected error for invalid format")
	}
}

func TestMustFromStringPanics(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic for invalid string")
		}
	}()
	MustFromString("invalid")
}

func TestMustFromCompactStringPanics(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic for invalid compact string")
		}
	}()
	MustFromCompactString("invalid")
}

// FromBytes tests

func TestFromBytes(t *testing.T) {
	uuid := Generate()
	bytes := uuid.Bytes()

	parsed, err := FromBytes(bytes)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if parsed != uuid {
		t.Errorf("parsed UUID does not match original")
	}
}

func TestFromBytesInvalidLength(t *testing.T) {
	_, err := FromBytes([]byte{1, 2, 3})
	if err == nil {
		t.Error("expected error for invalid length")
	}
}

// Time method test

func TestTimeMethod(t *testing.T) {
	fixedTime := int64(1234567890123)
	uuid := GenerateWithClock(func() int64 { return fixedTime })

	tm, err := uuid.Time()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if tm.UnixMilli() != fixedTime {
		t.Errorf("expected time %d, got %d", fixedTime, tm.UnixMilli())
	}
}

// Concurrency test

func TestConcurrentGenerate(t *testing.T) {
	const goroutines = 10
	const uuidsPerGoroutine = 1000

	var wg sync.WaitGroup
	uuidsChan := make(chan UUID, goroutines*uuidsPerGoroutine)

	for i := 0; i < goroutines; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < uuidsPerGoroutine; j++ {
				uuidsChan <- Generate()
			}
		}()
	}

	wg.Wait()
	close(uuidsChan)

	uuids := make(map[UUID]bool)
	for uuid := range uuidsChan {
		if uuids[uuid] {
			t.Error("duplicate UUID found in concurrent generation")
		}
		uuids[uuid] = true
	}

	if len(uuids) != goroutines*uuidsPerGoroutine {
		t.Errorf("expected %d unique UUIDs, got %d", goroutines*uuidsPerGoroutine, len(uuids))
	}
}

func TestConcurrentGenerateMonotonic(t *testing.T) {
	ResetMonotonicState()
	const goroutines = 10
	const uuidsPerGoroutine = 100

	var wg sync.WaitGroup
	uuidsChan := make(chan UUID, goroutines*uuidsPerGoroutine)

	for i := 0; i < goroutines; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < uuidsPerGoroutine; j++ {
				uuidsChan <- GenerateMonotonic()
			}
		}()
	}

	wg.Wait()
	close(uuidsChan)

	uuids := make(map[UUID]bool)
	for uuid := range uuidsChan {
		if uuids[uuid] {
			t.Error("duplicate UUID found in concurrent monotonic generation")
		}
		uuids[uuid] = true
	}

	if len(uuids) != goroutines*uuidsPerGoroutine {
		t.Errorf("expected %d unique UUIDs, got %d", goroutines*uuidsPerGoroutine, len(uuids))
	}
}

// Benchmark tests

func BenchmarkGenerate(b *testing.B) {
	for i := 0; i < b.N; i++ {
		_ = Generate()
	}
}

func BenchmarkGenerateMonotonic(b *testing.B) {
	ResetMonotonicState()
	for i := 0; i < b.N; i++ {
		_ = GenerateMonotonic()
	}
}

func BenchmarkToCompactString(b *testing.B) {
	uuid := Generate()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = ToCompactString(uuid)
	}
}

func BenchmarkFromCompactString(b *testing.B) {
	uuid := Generate()
	compactString := ToCompactString(uuid)
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = FromCompactString(compactString)
	}
}

func BenchmarkGenerateCompactString(b *testing.B) {
	for i := 0; i < b.N; i++ {
		_ = GenerateCompactString()
	}
}

func BenchmarkGenerateParallel(b *testing.B) {
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			_ = Generate()
		}
	})
}

func BenchmarkGenerateMonotonicParallel(b *testing.B) {
	ResetMonotonicState()
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			_ = GenerateMonotonic()
		}
	})
}

// Additional edge case tests

func TestCompareMethod(t *testing.T) {
	uuid1 := GenerateWithClock(func() int64 { return 1000000000000 })
	uuid2 := GenerateWithClock(func() int64 { return 2000000000000 })

	if uuid1.Compare(uuid2) >= 0 {
		t.Error("uuid1 should be less than uuid2")
	}
	if uuid2.Compare(uuid1) <= 0 {
		t.Error("uuid2 should be greater than uuid1")
	}
	if uuid1.Compare(uuid1) != 0 {
		t.Error("uuid1 should equal itself")
	}
}

func TestMonotonicUniquenessUnderHighLoad(t *testing.T) {
	ResetMonotonicState()
	var timestamp atomic.Int64
	timestamp.Store(1234567890000)

	uuids := make(map[UUID]bool)
	for i := 0; i < 10000; i++ {
		uuid := GenerateMonotonicWithClock(func() int64 {
			return timestamp.Add(1)
		})
		if uuids[uuid] {
			t.Error("duplicate UUID found")
		}
		uuids[uuid] = true
	}

	if len(uuids) != 10000 {
		t.Errorf("expected 10000 unique UUIDs, got %d", len(uuids))
	}
}
