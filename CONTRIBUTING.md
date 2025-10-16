# Contributing to UUIDv7

## Prerequisites

- Java 21 or later
- No need to install Gradle - the project uses Gradle Wrapper

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Project Structure

```
src/
  main/
    java/xyz/block/uuidv7/
      UUIDv7.java              # High-performance UUID v7 (no ordering guarantees)
      MonotonicUUIDv7.java     # Monotonic UUID v7 (strict ordering)
    kotlin/xyz/block/uuidv7/
      UuidV7Extensions.kt      # Kotlin extensions
  test/
    java/xyz/block/uuidv7/
      UUIDv7Test.java          # Tests for UUIDv7
      MonotonicUUIDv7Test.java # Tests for MonotonicUUIDv7
    kotlin/xyz/block/uuidv7/
      UUIDv7ExtensionsTest.kt  # Kotlin tests
```

## Code Style

- Follow standard Java and Kotlin conventions
- Keep the API minimal and focused
- Ensure all tests pass before submitting a PR

## Submitting Changes

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Ensure `./gradlew test` passes
5. Submit a pull request
