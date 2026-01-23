# frozen_string_literal: true

RSpec.describe MonotonicUUIDv7 do
  before do
    UUIDv7::MonotonicGenerator.reset_state!
  end

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

    it 'generates unique UUIDs' do
      uuids = Array.new(1000) { described_class.generate }
      expect(uuids.uniq.length).to eq(1000)
    end

    it 'generates strictly ordered UUIDs within same millisecond' do
      fixed_time = 1_700_000_000_000
      uuids = Array.new(100) { described_class.generate { fixed_time } }

      expect(uuids).to eq(uuids.sort)
      expect(uuids.uniq.length).to eq(100)
    end

    it 'resets counter when timestamp advances' do
      uuid1 = described_class.generate { 1_700_000_000_000 }
      uuid2 = described_class.generate { 1_700_000_000_001 }

      ts1 = UUIDv7.timestamp(uuid1)
      ts2 = UUIDv7.timestamp(uuid2)

      expect(ts2).to be > ts1
    end

    it 'handles counter overflow by waiting for next millisecond' do
      current_time = 1_700_000_000_000
      call_count = 0

      uuids = Array.new(4100) do
        described_class.generate do
          call_count += 1
          current_time += 1 if call_count > 4096
          current_time
        end
      end

      expect(uuids.uniq.length).to eq(4100)
      expect(uuids).to eq(uuids.sort)
    end

    it 'accepts a custom clock block' do
      fixed_time = 1_700_000_000_000
      uuid = described_class.generate { fixed_time }
      timestamp = UUIDv7.timestamp(uuid)
      expect(timestamp).to eq(fixed_time)
    end
  end

  describe '.generate_compact' do
    it 'returns a 22-character string' do
      compact = described_class.generate_compact
      expect(compact.length).to eq(22)
    end

    it 'generates strictly ordered compact strings within same millisecond' do
      fixed_time = 1_700_000_000_000
      compacts = Array.new(100) { described_class.generate_compact { fixed_time } }

      expect(compacts).to eq(compacts.sort)
      expect(compacts.uniq.length).to eq(100)
    end
  end

  describe 'thread safety' do
    it 'generates unique UUIDs across threads' do
      uuids = Queue.new
      threads = Array.new(10) do
        Thread.new do
          100.times { uuids << described_class.generate }
        end
      end

      threads.each(&:join)

      all_uuids = []
      all_uuids << uuids.pop until uuids.empty?

      expect(all_uuids.uniq.length).to eq(1000)
    end
  end
end
