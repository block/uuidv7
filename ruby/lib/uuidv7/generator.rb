# frozen_string_literal: true

require 'securerandom'

module UUIDv7
  module Generator
    class << self
      def generate(&clock)
        timestamp = clock ? clock.call : (Time.now.to_f * 1000).to_i
        rand_a = rand(4096)
        rand_b = SecureRandom.random_number(2**62)

        build(timestamp, rand_a, rand_b)
      end

      def generate_compact(&clock)
        Base62.encode(generate(&clock))
      end

      def timestamp(uuid)
        raise InvalidUUIDError, 'UUID cannot be nil' if uuid.nil?

        hex = uuid.delete('-')
        raise InvalidUUIDError, 'Invalid UUID format' unless hex.match?(/\A[0-9a-f]{32}\z/i)

        msb = hex[0, 16].to_i(16)
        version = (msb >> 12) & 0xF

        raise InvalidUUIDError, "UUID is not version 7 (got version #{version})" unless version == 7

        msb >> 16
      end

      def build(timestamp, rand_a, rand_b)
        rand_a &= 0xFFF

        msb = (timestamp << 16) | rand_a
        lsb = rand_b

        msb = (msb & 0xFFFFFFFFFFFF0FFF) | 0x0000000000007000
        lsb = (lsb & 0x3FFFFFFFFFFFFFFF) | 0x8000000000000000

        format_uuid(msb, lsb)
      end

      private

      def format_uuid(msb, lsb)
        hex = format('%016x%016x', msb, lsb)
        "#{hex[0, 8]}-#{hex[8, 4]}-#{hex[12, 4]}-#{hex[16, 4]}-#{hex[20, 12]}"
      end
    end
  end
end
