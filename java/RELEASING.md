# Releasing to Maven Central

## Prerequisites

Ensure GitHub repository has these secrets configured:
- `SONATYPE_CENTRAL_USERNAME` - Maven Central username
- `SONATYPE_CENTRAL_PASSWORD` - Maven Central password
- `GPG_SECRET_KEY` - GPG signing key (armored)
- `GPG_SECRET_PASSPHRASE` - GPG key passphrase

## Version Selection

Follow semantic versioning (MAJOR.MINOR.PATCH):
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

## Release Process

1. **Create release branch**:
   ```bash
   git checkout -b release-X.Y.Z
   ```

2. **Update version** in `build.gradle.kts` (remove `-SNAPSHOT`):
   ```kotlin
   version = "X.Y.Z"
   ```

3. **Commit, push, and create PR**:
   ```bash
   git add build.gradle.kts
   git commit -m "Release version X.Y.Z"
   git push origin release-X.Y.Z
   ```
   Open PR, get approval, merge to main.

4. **Create and push tag** (from main):
   ```bash
   git checkout main
   git pull origin main
   git tag X.Y.Z
   git push origin X.Y.Z
   ```

5. **GitHub Actions** automatically:
   - Runs `./gradlew publish`
   - Signs artifacts with GPG
   - Publishes to Maven Central (auto-release enabled)

6. **Bump to next snapshot** (new PR):
   ```bash
   git checkout -b bump-snapshot
   ```
   Update `build.gradle.kts`:
   ```kotlin
   version = "X.Y+1.0-SNAPSHOT"
   ```
   ```bash
   git add build.gradle.kts
   git commit -m "Bump to X.Y+1.0-SNAPSHOT"
   git push origin bump-snapshot
   ```
   Open PR, merge to main.

## Verification

Check release status at:
- GitHub Actions: https://github.com/block/uuidv7/actions
- Maven Central: https://central.sonatype.com/artifact/xyz.block/uuidv7
