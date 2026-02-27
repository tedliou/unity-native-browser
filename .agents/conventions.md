# Code Conventions

## Languages

| Context | Language |
|---------|----------|
| Android native | Kotlin (`kotlin.code.style=official`) |
| Unity scripts | C# |
| Comments and logs | English only |
| Documentation | English + Traditional Chinese (zh-TW) |
| Git commit messages | English |

## Architecture Principles

- **Interface-based design**: `IBrowser` contract for all browser types
- **Design patterns**: Strategy, Observer, Singleton, Sealed Class errors
- **Clean separation**: core (interfaces) → impl (webview/customtab/system) → bridge (Unity relay)
- **Fail fast**: `BrowserConfig` validates in init block; typed exceptions, no generic catch-all

## Android Conventions

- **All WebView operations**: `runOnUiThread { ... }` — Unity calls arrive on GL thread
- **Activity references**: Always `WeakReference<Activity>` — prevents memory leaks
- **UnityPlayer access**: Reflection only (`Class.forName`) — no compile-time dependency
- **Callback delivery**: JSON via `UnitySendMessage("NativeBrowserCallback", methodName, json)`
- **Logging**: `BrowserLogger.d(SUBTAG, msg)` — tag format `NativeBrowser:{subtag}`
- **URL sanitization**: Query params stripped from logs to prevent token leakage
- **KDoc**: 100% coverage on all public APIs

## Unity Conventions

- **Namespace**: `TedLiou.NativeBrowser` (public), `TedLiou.NativeBrowser.Internal` (internal)
- **Platform guard**: `#if UNITY_ANDROID && !UNITY_EDITOR` around all JNI calls
- **Callback receiver**: Singleton MonoBehaviour, `DontDestroyOnLoad`, `[Preserve]` attribute
- **JSON**: `JsonUtility.FromJson<T>()` — Unity built-in, no external deps
- **Assembly defs**: Separate Runtime, Editor, Tests.Editor, Tests.Runtime

## Git Conventions

- Clean, atomic commits with descriptive messages
- Non-tracked files stay out of VCS
- Never commit `local.properties` or `src/unity/Library/`

## Anti-Patterns (DO NOT)

| Anti-Pattern | Why |
|-------------|-----|
| File operations outside project directory | Security risk |
| Suppress warnings/errors | Masks real problems |
| Search inside `src/unity/Library/` | 20k+ cached Unity files, not project code |
| Commit `src/android/local.properties` | Auto-generated, contains `sdk.dir` |
| Commit `src/unity/Library/` | Unity cache, regenerated on open |
| Add JS interface without ProGuard rule | Will be stripped by R8/ProGuard |
| Hold strong Activity reference | Memory leak — use `WeakReference` |
| Call WebView ops from non-UI thread | Will crash |
| Use `as any` / `@ts-ignore` equivalents | Type safety violation |
| Generic `catch (Exception)` | Use sealed `BrowserException` hierarchy |

## Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Kotlin package | lowercase dot-separated | `com.tedliou.android.browser.core` |
| Kotlin class | PascalCase | `WebViewBrowser` |
| Kotlin method | camelCase | `openBrowser()` |
| C# namespace | PascalCase dot-separated | `TedLiou.NativeBrowser` |
| C# class | PascalCase | `NativeBrowser` |
| C# event | PascalCase with `On` prefix | `OnPageStarted` |
| Logger subtag | PascalCase short name | `"WebView"`, `"Bridge"` |
| GameObject | PascalCase | `"NativeBrowserCallback"` |
