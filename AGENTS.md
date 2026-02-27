# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-26
**Status:** Production — v1.0 complete

## OVERVIEW

Android native browser plugin (.aar) for Unity. Provides WebView, Custom Tabs, and system browser launch from Unity C# → Android Kotlin bridge. Primary language: Kotlin (Android side), C# (Unity side).

## STRUCTURE

```
.
├── src/
│   ├── android/          # Android Gradle project → builds .aar
│   └── unity/            # Unity 6000.3.10f1 project (URP)
├── tools/                # Automation scripts (build, test, deploy, CI) — empty
├── docs/                 # Developer docs (EN + zh-TW)
├── .sisyphus/            # Sisyphus-specific configuration and logs
└── README.md             # Spec in zh-TW
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Android core | `src/android/app/src/main/java/com/tedliou/android/browser/core/` | IBrowser, config, types |
| Android bridge | `src/android/app/src/main/java/com/tedliou/android/browser/bridge/` | UnitySendMessage bridge |
| Android WebView | `src/android/app/src/main/java/com/tedliou/android/browser/webview/` | WebView implementation |
| Android Custom Tab | `src/android/app/src/main/java/com/tedliou/android/browser/customtab/` | CustomTab implementation |
| Android build config | `src/android/app/build.gradle.kts` | AGP 9.0.1, compileSdk 36, minSdk 28 |
| Unity Runtime | `src/unity/Assets/Plugins/NativeBrowser/Runtime/` | C# API and Internal bridge |
| Unity Editor | `src/unity/Assets/Plugins/NativeBrowser/Editor/` | Build scripts and inspectors |
| Unity Tests | `src/unity/Assets/Tests/` | Edit Mode and Play Mode tests |
| Unity Plugins | `src/unity/Assets/Plugins/Android/` | .aar and mainTemplate.gradle |
| Version catalog | `src/android/gradle/libs.versions.toml` | Dependency versions |

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

## IMPLEMENTED FEATURES

- WebView: open, close, refresh, PostMessage receive, JS execute/inject, configurable size/alignment, tap-outside-to-close, deep link interception
- Custom Tabs: full integration for Chrome/system custom tabs
- System Browser: launch fallback
- Testing: Unit tests ≥85% coverage; integration tests with mock web pages (Edit + Play Mode)

## CONVENTIONS

- **Language**: Android = Kotlin (`kotlin.code.style=official`), Unity = C#
- **Comments/logs**: English only
- **Docs**: English + Traditional Chinese (zh-TW) when feature is complete
- **Architecture**: Clean, design-pattern-driven
- **Git**: Clean commit history; non-tracked files stay out of VCS
- **Dead code**: Remove unused code/packages; upgrade to newer versions when available
- **Agent file reads**: Return path + line numbers — NEVER full file content via subagent
- **Scripting**: Prefer scripts over manual repetitive work (saves tokens + time)
- **UI thread**: All WebView operations via runOnUiThread
- **UnitySendMessage**: callback receiver GameObject name must be NativeBrowserCallback

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
JAVA_HOME="C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" E:/android-browser-for-unity/src/android/gradlew.bat assembleRelease

# Unity APK build (from root)
"C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" -quit -batchmode -nographics -projectPath "E:\android-browser-for-unity\src\unity" -executeMethod TedLiou.Build.BuildScript.BuildAndroid -buildTarget Android -logFile - 2>&1

# Android unit tests (from src/android/)
./gradlew test
```

## NOTES

- **.aar build pipeline**: Android project builds .aar, which is then copied to Unity Plugins directory.
- **Unity custom Gradle template**: Uses `mainTemplate.gradle` at `Assets/Plugins/Android/` for dependencies.
- **BackPressInterceptLayout**: Custom layout to handle Android back button within WebView.
- **Two-device testing**: Verified on both Android emulator and real physical devices.
- Final deliverable: .aar imported into Unity, APK running on Android VM.

## CHILD AGENTS

- [`src/android/AGENTS.md`](src/android/AGENTS.md) — Android Gradle project specifics
- [`src/unity/AGENTS.md`](src/unity/AGENTS.md) — Unity project specifics
