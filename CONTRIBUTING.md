# Contributing to UUIDv7

Thank you for your interest in contributing to UUIDv7!

## Multi-Language Repository

This repository contains UUID v7 implementations for multiple languages. Each language has its own subdirectory with its own build system, tests, and documentation.

## Language-Specific Guides

For detailed contribution guidelines for each language, see:

- **Java**: [java/CONTRIBUTING.md](java/CONTRIBUTING.md)
- **Ruby**: Coming soon
- **Go**: Coming soon
- **JavaScript**: Coming soon
- **Swift**: Coming soon

## General Guidelines

### Design Principles

All language implementations should follow these core principles:

1. **Minimal API Surface**: Work with the language's standard UUID type when possible
2. **Two Variants**: Provide both standard (high-performance) and monotonic (strictly ordered) implementations
3. **Timestamp Extraction**: Allow extracting the embedded timestamp from UUID v7
4. **Compact Strings**: Support Base62 encoding for shorter, URL-safe representations
5. **No External Dependencies**: Minimize or eliminate external dependencies

### Testing

Each implementation should include:
- Unit tests for UUID generation
- Tests for timestamp extraction
- Tests for compact string encoding/decoding
- Tests for monotonic ordering (if applicable)
- Performance benchmarks

### Documentation

Each language directory should have:
- README.md with installation, usage examples, and API reference
- CONTRIBUTING.md with language-specific build/test instructions
- Inline documentation following the language's conventions

### Code Style

- Follow the standard conventions for each language
- Keep APIs consistent across languages where possible
- Prefer clarity and simplicity over cleverness

## Submitting Changes

1. Fork the repository
2. Create a feature branch in the style `username-feature-name`
3. Make your changes with tests
4. Ensure all tests pass
5. Update documentation
6. Submit a pull request

## Questions?

Open an issue for discussion before starting work on major features.
