# frozen_string_literal: true

require 'securerandom'

module UUIDv7
  module MonotonicGenerator
    COUNTER_MAX = 0xFFF # 12 bits = 4095

    @mutex = Mutex.new
    @last_timestamp = 0
    @counter = 0

    class << self
      def generate(&clock)
        @mutex.synchronize do
          timestamp = clock ? clock.call : (Time.now.to_f * 1000).to_i
          counter_value = nil

          if timestamp == @last_timestamp
            @counter = (@counter + 1) & COUNTER_MAX

            if @counter.zero?
              loop do
                timestamp = clock ? clock.call : (Time.now.to_f * 1000).to_i
                break if timestamp != @last_timestamp

                sleep(0.0001) unless clock
              end
              @counter = SecureRandom.random_number(COUNTER_MAX + 1)
            end
            counter_value = @counter
          else
            @counter = SecureRandom.random_number(COUNTER_MAX + 1)
            counter_value = @counter
            @last_timestamp = timestamp
          end

          rand_b = SecureRandom.random_number(2**62)
          Generator.build(timestamp, counter_value, rand_b)
        end
      end

      def generate_compact(&clock)
        Base62.encode(generate(&clock))
      end

      def reset_state!
        @mutex.synchronize do
          @last_timestamp = 0
          @counter = 0
        end
      end
    end
  end
end
