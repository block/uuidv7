# frozen_string_literal: true

module UUIDv7
  module Base62
    ALPHABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    BASE = 62
    COMPACT_LENGTH = 22

    class << self
      def encode(uuid)
        raise InvalidUUIDError, 'UUID cannot be nil' if uuid.nil?

        hex = uuid.delete('-')
        raise InvalidUUIDError, 'Invalid UUID format' unless hex.match?(/\A[0-9a-f]{32}\z/i)

        value = hex.to_i(16)
        result = []

        while value.positive?
          value, remainder = value.divmod(BASE)
          result << ALPHABET[remainder]
        end

        result << '0' while result.length < COMPACT_LENGTH

        result.reverse.join
      end

      def decode(compact_string)
        raise InvalidCompactStringError, 'Compact string cannot be nil' if compact_string.nil?

        unless compact_string.length == COMPACT_LENGTH
          raise InvalidCompactStringError,
                "Compact string must be exactly #{COMPACT_LENGTH} characters (got #{compact_string.length})"
        end

        value = 0
        compact_string.each_char do |char|
          digit = ALPHABET.index(char)
          raise InvalidCompactStringError, "Invalid compact string character: #{char}" if digit.nil?

          value = (value * BASE) + digit
        end

        msb = (value >> 64) & 0xFFFFFFFFFFFFFFFF
        lsb = value & 0xFFFFFFFFFFFFFFFF

        hex = format('%016x%016x', msb, lsb)
        "#{hex[0, 8]}-#{hex[8, 4]}-#{hex[12, 4]}-#{hex[16, 4]}-#{hex[20, 12]}"
      end
    end
  end
end
