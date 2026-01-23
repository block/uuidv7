# frozen_string_literal: true

RSpec.describe UUIDv7 do
  describe '.generate' do
    it 'returns a valid UUID string' do
      uuid = described_class.generate
      expect(uuid).to match(/\A[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\z/i)
    end

    it 'generates version 7 UUIDs' do
      uuid = described_class.generate
      version = uuid[14].to_i(16)
      expect(version).to eq(7)
    end

    it 'generates RFC 4122 variant UUIDs' do
      uuid = described_class.generate
      variant_nibble = uuid[19].to_i(16)
      expect(variant_nibble).to be_between(8, 11)
    end

    it 'generates unique UUIDs' do
      uuids = Array.new(1000) { described_class.generate }
      expect(uuids.uniq.length).to eq(1000)
    end

    it 'accepts a custom clock block' do
      fixed_time = 1_700_000_000_000
      uuid = described_class.generate { fixed_time }
      timestamp = described_class.timestamp(uuid)
      expect(timestamp).to eq(fixed_time)
    end

    it 'embeds current time in UUID' do
      before = (Time.now.to_f * 1000).to_i
      uuid = described_class.generate
      after = (Time.now.to_f * 1000).to_i

      timestamp = described_class.timestamp(uuid)
      expect(timestamp).to be_between(before, after)
    end
  end

  describe '.generate_compact' do
    it 'returns a 22-character string' do
      compact = described_class.generate_compact
      expect(compact.length).to eq(22)
    end

    it 'uses only Base62 characters' do
      compact = described_class.generate_compact
      expect(compact).to match(/\A[0-9A-Za-z]{22}\z/)
    end

    it 'accepts a custom clock block' do
      fixed_time = 1_700_000_000_000
      compact = described_class.generate_compact { fixed_time }
      uuid = described_class.from_compact(compact)
      timestamp = described_class.timestamp(uuid)
      expect(timestamp).to eq(fixed_time)
    end
  end

  describe '.timestamp' do
    it 'extracts timestamp from UUID v7' do
      fixed_time = 1_700_000_000_000
      uuid = described_class.generate { fixed_time }
      expect(described_class.timestamp(uuid)).to eq(fixed_time)
    end

    it 'raises error for nil UUID' do
      expect { described_class.timestamp(nil) }.to raise_error(UUIDv7::InvalidUUIDError, 'UUID cannot be nil')
    end

    it 'raises error for non-v7 UUID' do
      uuid_v4 = '550e8400-e29b-41d4-a716-446655440000'
      expect { described_class.timestamp(uuid_v4) }.to raise_error(UUIDv7::InvalidUUIDError, /not version 7/)
    end

    it 'raises error for invalid format' do
      expect { described_class.timestamp('not-a-uuid') }.to raise_error(UUIDv7::InvalidUUIDError)
    end
  end

  describe '.to_compact / .from_compact' do
    it 'round-trips correctly' do
      uuid = described_class.generate
      compact = described_class.to_compact(uuid)
      decoded = described_class.from_compact(compact)
      expect(decoded).to eq(uuid)
    end

    it 'preserves lexicographic ordering' do
      uuids = Array.new(100) do |i|
        described_class.generate { 1_700_000_000_000 + i }
      end
      compacts = uuids.map { |u| described_class.to_compact(u) }

      expect(compacts).to eq(compacts.sort)
    end

    it 'raises error for nil UUID' do
      expect { described_class.to_compact(nil) }.to raise_error(UUIDv7::InvalidUUIDError)
    end

    it 'raises error for nil compact string' do
      expect { described_class.from_compact(nil) }.to raise_error(UUIDv7::InvalidCompactStringError)
    end

    it 'raises error for wrong length compact string' do
      expect { described_class.from_compact('abc') }.to raise_error(UUIDv7::InvalidCompactStringError, /exactly 22/)
    end

    it 'raises error for invalid characters' do
      expect { described_class.from_compact("!@\#$%^&*()_+{}[]|\\:;<") }.to raise_error(UUIDv7::InvalidCompactStringError)
    end
  end
end
