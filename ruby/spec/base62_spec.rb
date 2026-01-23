# frozen_string_literal: true

RSpec.describe UUIDv7::Base62 do
  describe '.encode / .decode' do
    it 'encodes to exactly 22 characters' do
      uuid = UUIDv7.generate
      compact = described_class.encode(uuid)
      expect(compact.length).to eq(22)
    end

    it 'uses only Base62 alphabet' do
      uuid = UUIDv7.generate
      compact = described_class.encode(uuid)
      expect(compact).to match(/\A[0-9A-Za-z]+\z/)
    end

    it 'round-trips through encode/decode' do
      100.times do
        uuid = UUIDv7.generate
        compact = described_class.encode(uuid)
        decoded = described_class.decode(compact)
        expect(decoded).to eq(uuid)
      end
    end

    it 'handles minimum UUID value' do
      uuid = '00000000-0000-7000-8000-000000000000'
      compact = described_class.encode(uuid)
      decoded = described_class.decode(compact)
      expect(decoded).to eq(uuid)
    end

    it 'handles maximum-ish UUID value' do
      uuid = 'ffffffff-ffff-7fff-bfff-ffffffffffff'
      compact = described_class.encode(uuid)
      decoded = described_class.decode(compact)
      expect(decoded).to eq(uuid)
    end

    it 'preserves lexicographic ordering for time-ordered UUIDs' do
      uuids = Array.new(100) do |i|
        UUIDv7.generate { 1_700_000_000_000 + i }
      end

      compacts = uuids.map { |u| described_class.encode(u) }
      expect(compacts).to eq(compacts.sort)
    end
  end

  describe '.encode' do
    it 'raises error for nil UUID' do
      expect { described_class.encode(nil) }.to raise_error(UUIDv7::InvalidUUIDError)
    end

    it 'raises error for invalid UUID format' do
      expect { described_class.encode('not-a-uuid') }.to raise_error(UUIDv7::InvalidUUIDError)
    end
  end

  describe '.decode' do
    it 'raises error for nil input' do
      expect { described_class.decode(nil) }.to raise_error(UUIDv7::InvalidCompactStringError)
    end

    it 'raises error for wrong length' do
      expect { described_class.decode('abc') }.to raise_error(UUIDv7::InvalidCompactStringError, /exactly 22/)
    end

    it 'raises error for invalid characters' do
      expect { described_class.decode('!!!!!!!!!!!!!!!!!!!!--') }.to raise_error(UUIDv7::InvalidCompactStringError)
    end
  end
end
