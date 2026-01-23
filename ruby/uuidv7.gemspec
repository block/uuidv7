# frozen_string_literal: true

require_relative 'lib/uuidv7/version'

Gem::Specification.new do |spec|
  spec.name = 'uuidv7'
  spec.version = UUIDv7::VERSION
  spec.authors = ['Block, Inc.']
  spec.email = ['opensource@block.xyz']

  spec.summary = 'A minimal, high-performance UUID v7 implementation for Ruby'
  spec.description = 'UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in the most significant ' \
                     '48 bits, making UUIDs naturally sortable by creation time. This library provides both ' \
                     'high-performance and monotonic (strictly ordered) variants.'
  spec.homepage = 'https://github.com/block/uuidv7'
  spec.license = 'Apache-2.0'
  spec.required_ruby_version = '>= 3.0.0'

  spec.metadata['homepage_uri'] = spec.homepage
  spec.metadata['source_code_uri'] = 'https://github.com/block/uuidv7/tree/main/ruby'
  spec.metadata['rubygems_mfa_required'] = 'true'

  spec.files = Dir.chdir(__dir__) do
    `git ls-files -z`.split("\x0").reject do |f|
      (File.expand_path(f) == __FILE__) ||
        f.start_with?(*%w[bin/ test/ spec/ features/ .git .github appveyor Gemfile])
    end
  end
  spec.bindir = 'exe'
  spec.executables = spec.files.grep(%r{\Aexe/}) { |f| File.basename(f) }
  spec.require_paths = ['lib']
end
