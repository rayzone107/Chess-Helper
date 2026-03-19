---
kind: skill
name: repo-context
purpose: Stable repo facts to avoid repeated full-repo reads.
provides:
  - module layout
  - app entry points
  - build and validation commands
key_refs:
  - settings.gradle.kts
  - app/build.gradle.kts
  - app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/ui/theme/Theme.kt
  - app/src/main/AndroidManifest.xml
---
- Repo is a single Android module: `:app`.
- UI stack is Kotlin + Jetpack Compose + Material 3; there are no XML layouts.
- Current app entry is `MainActivity`; app logic is still template-level.
- Theme boundary is `ChessHelperTheme` in `ui/theme/Theme.kt`; colors/typography live beside it.
- Package/namespace is `com.rachitgoyal.chesshelper` across code, manifest, and tests.
- Gradle uses Kotlin DSL and a version catalog in `gradle/libs.versions.toml`.
- Use the wrapper from repo root for validation:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:connectedDebugAndroidTest`
  - `./gradlew :app:lintDebug`
- Add repositories centrally only; `settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS`.

