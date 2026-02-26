# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-26
**Status:** Greenfield — scaffold only, no feature code yet

## OVERVIEW

Android native browser plugin (.aar) for Unity. Provides WebView, Custom Tabs, and system browser launch from Unity C# → Android Kotlin bridge. Primary language: Kotlin (Android side), C# (Unity side).

## STRUCTURE

```
.
├── src/
│   ├── android/          # Android Gradle project → builds .aar
│   └── unity/            # Unity 6000.3.10f1 project (URP)
├── tools/                # Automation scripts (build, test, deploy, CI) — empty
├── .agents/              # AI reference docs — planned, not created
├── docs/                 # Developer docs (EN + zh-TW) — planned, not created
└── README.md             # Spec in zh-TW
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Android native code | `src/android/app/src/main/java/com/tedliou/android/browser/` | Kotlin, package `com.tedliou.android.browser` |
| Android build config | `src/android/app/build.gradle.kts` | AGP 9.0.1, compileSdk 36, minSdk 28 |
| Version catalog | `src/android/gradle/libs.versions.toml` | Dependency versions |
| Unity project | `src/unity/` | Unity 6000.3.10f1, URP 17.3.0 |
| Unity packages | `src/unity/Packages/manifest.json` | Input System, Test Framework included |
| Unit tests (Android) | `src/android/app/src/test/` | JUnit 4, boilerplate only |
| Instrumented tests | `src/android/app/src/androidTest/` | AndroidJUnit4, boilerplate only |
| Build/CI scripts | `tools/` | Empty — to be created |

## TECH STACK

| Component | Version |
|-----------|---------|
| Gradle | 9.3.1 |
| AGP | 9.0.1 |
| Android compileSdk | 36 |
| Android minSdk | 28 |
| Kotlin code style | official |
| Unity | 6000.3.10f1 |
| URP | 17.3.0 |
| Java compat | 11 |

## PLANNED FEATURES (from README spec)

- WebView: open/close/refresh, PostMessage receive, JS execute/inject, configurable size/alignment, tap-outside-to-close, deep link interception
- Custom Tabs: partial feature set
- System Browser: launch only
- Unit tests ≥85% coverage; integration tests with mock web pages (Edit + Play Mode)

## CONVENTIONS

- **Language**: Android = Kotlin (`kotlin.code.style=official`), Unity = C#
- **Comments/logs**: English only
- **Docs**: English + Traditional Chinese (zh-TW) when feature is complete
- **Architecture**: Clean, design-pattern-driven
- **Git**: Clean commit history; non-tracked files stay out of VCS
- **Dead code**: Remove unused code/packages; upgrade to newer versions when available
- **Agent file reads**: Return path + line numbers — NEVER full file content via subagent
- **Scripting**: Prefer scripts over manual repetitive work (saves tokens + time)

## ANTI-PATTERNS (THIS PROJECT)

- NO file operations outside project directory
- NO suppressing warnings/errors
- NEVER search inside `src/unity/Library/` — 20k+ cached Unity files, not project code
- NEVER commit `src/android/local.properties` — auto-generated, contains sdk.dir
- NEVER commit `src/unity/Library/` — Unity cache, should be gitignored
- When adding WebView JS interfaces → add corresponding ProGuard `-keep` rules

## COMMANDS

```bash
# Android build (from src/android/)
./gradlew assembleDebug
./gradlew assembleRelease

# Android unit tests
./gradlew test

# Android instrumented tests
./gradlew connectedAndroidTest

# Unity (headless — verify access first)
# Build/test commands TBD in tools/
```

## NOTES

- **Greenfield**: scaffold only — no feature Kotlin or C# bridge code written yet.
- **Critical conversion needed**: `src/android/app/build.gradle.kts` uses `android.application`; must change to `android.library` + remove `applicationId` to produce .aar.
- **AndroidManifest**: No `INTERNET` permission yet (required for WebView); no Activity declared.
- **Missing deps**: `androidx.browser:browser` not yet in version catalog (needed for Custom Tabs).
- **Unity side**: `com.unity.modules.androidjni` already in manifest — `AndroidJavaClass`/`AndroidJavaObject` available.
- **No .asmdef files yet** — will need Assembly Definitions for test isolation in Unity.
- **`tools/`** is empty — build/test/copy/clean scripts planned for CI.
- Final deliverable: .aar imported into Unity, APK running on Android VM.

## CHILD AGENTS

- [`src/android/AGENTS.md`](src/android/AGENTS.md) — Android Gradle project specifics
- [`src/unity/AGENTS.md`](src/unity/AGENTS.md) — Unity project specifics
