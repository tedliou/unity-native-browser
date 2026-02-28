# PROJECT KNOWLEDGE BASE

**Project**: Native browser plugin for Unity (Android + Windows + WebGL)
**Status**: Multi-platform — v1.1.0-dev

## Quick Reference

| Topic | File |
|-------|------|
| Project overview & map | [.agents/overview.md](.agents/overview.md) |
| Android architecture | [.agents/android.md](.agents/android.md) |
| Unity architecture | [.agents/unity.md](.agents/unity.md) |
| Build commands & pipeline | [.agents/build.md](.agents/build.md) |
| Known bugs & fixes | [.agents/troubleshooting.md](.agents/troubleshooting.md) |
| Code conventions | [.agents/conventions.md](.agents/conventions.md) |
| Windows architecture | [.agents/windows.md](.agents/windows.md) |
| WebGL architecture | [.agents/webgl.md](.agents/webgl.md) |

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
| Windows Rust native layer | `src/windows/src/` |
| Windows DLL plugin | `src/unity/Assets/Plugins/NativeBrowser/Runtime/Plugins/x86_64/` |
| Windows build config | `src/windows/Cargo.toml` |
| WebGL C# bridge | `src/unity/Assets/Plugins/NativeBrowser/Runtime/Internal/WebGLBridge.cs` |
| WebGL JavaScript plugin | `src/unity/Assets/Plugins/NativeBrowser/Runtime/Plugins/WebGL/NativeBrowser.jslib` |
| WebGL build validator | `src/unity/Assets/Plugins/NativeBrowser/Editor/WebGLPluginValidator.cs` |
| WebGL test server | `tools/webgl-test-server.py` |

## Commands

```bash
# Android .aar build
JAVA_HOME="<UNITY_INSTALL>/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" src/android/gradlew assembleRelease

# Android unit tests
JAVA_HOME="<UNITY_INSTALL>/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" src/android/gradlew test

# Unity APK build (headless)
"<UNITY_INSTALL>/Editor/Unity" -quit -batchmode -nographics -projectPath src/unity -executeMethod TedLiou.Build.BuildScript.BuildAndroid -buildTarget Android -logFile - 2>&1
```

```powershell
# Windows DLL build
.\tools\build-windows.ps1

# Windows Rust unit tests
cd src\windows && cargo test
```

```bash
# WebGL build (requires WebGL Build Support module)
"<UNITY_INSTALL>/Editor/Unity" -quit -batchmode -nographics -projectPath src/unity -executeMethod TedLiou.Build.BuildScript.BuildWebGL -buildTarget WebGL -logFile - 2>&1

# WebGL test server
python tools/webgl-test-server.py [BUILD_DIR] [--port PORT]
```

## Anti-Patterns

- NEVER search inside `src/unity/Library/` — 20k+ cached files
- NEVER commit `src/android/local.properties`
- All WebView operations MUST use `runOnUiThread`
- When adding JS interfaces → update BOTH ProGuard files
- All WebView2 COM operations MUST run on the STA thread (enforced by threading.rs)
- Close Unity before rebuilding the Windows DLL (file lock)
- WebGL .jslib helpers MUST be prefixed with `$` (Emscripten dependency system)
- WebGL iframe cross-origin: use `postMessage`, never `contentWindow` property access
