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
      UuidV7.java              # Core implementation
    kotlin/xyz/block/uuidv7/
      UuidV7Extensions.kt      # Kotlin extensions
  test/
    java/xyz/block/uuidv7/
      UuidV7Test.java          # Java tests
    kotlin/xyz/block/uuidv7/
      UuidV7ExtensionsTest.kt  # Kotlin tests
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
