# frozen_string_literal: true

require_relative 'uuidv7/version'
require_relative 'uuidv7/base62'
require_relative 'uuidv7/generator'
require_relative 'uuidv7/monotonic_generator'

module UUIDv7
  class Error < StandardError; end
  class InvalidUUIDError < Error; end
  class InvalidCompactStringError < Error; end

  class << self
    def generate(&clock)
      Generator.generate(&clock)
    end

    def generate_compact(&clock)
      Generator.generate_compact(&clock)
    end

    def timestamp(uuid)
      Generator.timestamp(uuid)
    end

    def to_compact(uuid)
      Base62.encode(uuid)
    end

    def from_compact(compact_string)
      Base62.decode(compact_string)
    end
  end
end

module MonotonicUUIDv7
  class << self
    def generate(&clock)
      UUIDv7::MonotonicGenerator.generate(&clock)
    end

    def generate_compact(&clock)
      UUIDv7::MonotonicGenerator.generate_compact(&clock)
    end
  end
end
