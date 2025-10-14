# Contributing to VeeSeven

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
    java/xyz/block/veeseven/
      UuidV7.java              # Core implementation
    kotlin/xyz/block/veeseven/
      UuidV7Extensions.kt      # Kotlin extensions
  test/
    java/xyz/block/veeseven/
      UuidV7Test.java          # Java tests
    kotlin/xyz/block/veeseven/
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
