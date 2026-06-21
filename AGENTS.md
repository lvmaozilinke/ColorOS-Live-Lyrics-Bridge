# Repository Guidelines

## Project Structure & Module Organization

This is a Gradle Kotlin DSL Android project for an LSPosed/libxposed API 102 module.

- `app/src/main/java/io/github/andrealtb/lockscreenlyrics/` contains player adapters, lyric parsing, metadata contracts, and SystemUI hooks.
- `app/src/main/resources/META-INF/xposed/` defines the module entry point, metadata, and static scope.
- `app/src/test/java/.../lockscreenlyrics/` contains JVM unit tests.
- `libxposed-api-stubs/` provides compile-only API 102 classes; do not package or add runtime behavior here.
- `docs/` documents the player-facing `lyricInfo` integration contract.
- `.github/workflows/` contains debug and signed-release automation. `GIF.gif` is the README demonstration asset.

## Build, Test, and Development Commands

JDK 21 is required, although Android output targets Java 17 bytecode.

```powershell
.\scripts\gradle-local.cmd :app:assembleDebug
.\scripts\gradle-local.cmd :app:testDebugUnitTest
adb install -r .gradle-local-build\app\outputs\apk\debug\app-debug.apk
adb logcat -v time -s LockscreenLyrics
```

`scripts\gradle-local.cmd` discovers JDK 21 from `SALT_LYRIC_JAVA_HOME`, `JAVA_HOME`, or common local JDK locations, runs Gradle through a temporary ASCII drive letter, bypasses the local PowerShell script execution policy for this helper only, stores the wrapper cache in `.gradle-user-home/`, and writes build outputs to `.gradle-local-build/`. This avoids Windows/Gradle test-worker classpath corruption when the repository path contains Chinese characters and avoids stale `app/build` output locks. `assembleDebug` produces the test APK. `testDebugUnitTest` runs the JUnit 4 suite. After installation, enable the module for System UI and the target player in LSPosed, then restart affected processes.

## Coding Style & Naming Conventions

Use four-space indentation and standard Java brace placement. Prefer `final` for immutable values, `UPPER_SNAKE_CASE` for constants, `lowerCamelCase` for methods and fields, and descriptive class names such as `SaltPlayerAdapter`. Keep reflection and hook failures guarded: SystemUI must degrade safely instead of crashing. Preserve fixed lyric-item geometry unless a change explicitly addresses scroll stability.

No formatter is enforced; run `git diff --check` before committing.

## Testing Guidelines

Use JUnit 4 and name test classes `*Test.java`; test methods should describe behavior, for example `explicitSuffixDoesNotChangeTrackIdentity`. Add deterministic parser or identity regressions for bug fixes. Fixture-dependent tests must use an explicit system property and `Assume` when the fixture is absent. There is no formal coverage threshold.

## Commit & Pull Request Guidelines

History uses short, imperative subjects such as `Fix lockscreen lyric rendering` and `Build lyrics core with JDK 21`. Use `[skip ci]` only for documentation-only changes. Pull requests should explain affected processes, list build/test results, link relevant issues, and include screenshots or a short recording for visual lyric changes. Include focused `adb logcat` excerpts for hook or timing changes.

## Security & Configuration

Keep signing credentials in environment variables or repository secrets. Never commit keystores, passwords, device logs containing personal media paths, `local.properties`, or generated APKs.
