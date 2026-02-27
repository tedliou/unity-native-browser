# PROJECT KNOWLEDGE BASE

**Project**: Android native browser plugin (.aar) for Unity
**Status**: Production — v1.0.0

## Quick Reference

| Topic | File |
|-------|------|
| Project overview & map | [.agents/overview.md](.agents/overview.md) |
| Android architecture | [.agents/android.md](.agents/android.md) |
| Unity architecture | [.agents/unity.md](.agents/unity.md) |
| Build commands & pipeline | [.agents/build.md](.agents/build.md) |
| Known bugs & fixes | [.agents/troubleshooting.md](.agents/troubleshooting.md) |
| Code conventions | [.agents/conventions.md](.agents/conventions.md) |
| SSH signing setup | [.agents/guides/ssh-signing-setup.md](.agents/guides/ssh-signing-setup.md) |

## Where to Look

| Task | Location |
|------|----------|
| Android core interfaces | `src/android/app/src/main/java/com/tedliou/android/browser/core/` |
| Android bridge | `src/android/app/src/main/java/com/tedliou/android/browser/bridge/` |
| Android WebView | `src/android/app/src/main/java/com/tedliou/android/browser/webview/` |
| Android Custom Tab | `src/android/app/src/main/java/com/tedliou/android/browser/customtab/` |
| Android build config | `src/android/app/build.gradle.kts` |
| Unity C# API | `src/unity/Assets/Plugins/NativeBrowser/Runtime/` |
| Unity editor scripts | `src/unity/Assets/Plugins/NativeBrowser/Editor/` |
| Unity tests | `src/unity/Assets/Tests/` |
| Unity .aar + Gradle | `src/unity/Assets/Plugins/Android/` |
| Version catalog | `src/android/gradle/libs.versions.toml` |
| Developer docs | `docs/en/` and `docs/zh-tw/` |
| Automation scripts | `tools/` |

## Commands

```bash
# Android .aar build
JAVA_HOME="C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" E:/android-browser-for-unity/src/android/gradlew.bat assembleRelease

# Android unit tests
JAVA_HOME="C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" E:/android-browser-for-unity/src/android/gradlew.bat test

# Unity APK build (headless)
"C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" -quit -batchmode -nographics -projectPath "E:\android-browser-for-unity\src\unity" -executeMethod TedLiou.Build.BuildScript.BuildAndroid -buildTarget Android -logFile - 2>&1
```

## Anti-Patterns

- NEVER search inside `src/unity/Library/` — 20k+ cached files
- NEVER commit `src/android/local.properties`
- All WebView operations MUST use `runOnUiThread`
- When adding JS interfaces → update BOTH ProGuard files
