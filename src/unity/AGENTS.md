# UNITY PROJECT

## OVERVIEW

Unity 6000.3.10f1 (Unity 6) with URP 17.3.0. Fully implemented plugin with a robust C# bridge to the Android native .aar.

## STRUCTURE

```
src/unity/
├── Assets/
│   ├── Plugins/
│   │   ├── Android/
│   │   │   ├── NativeBrowser.aar       # The native library
│   │   │   └── mainTemplate.gradle     # Custom Gradle template with dependencies
│   │   └── NativeBrowser/
│   │       ├── Runtime/                # C# API and JNI bridge
│   │       ├── Editor/                 # Build scripts and custom inspectors
│   │       └── TedLiou.NativeBrowser.asmdef
│   ├── Scenes/
│   │   └── Demo.unity                  # Demonstration scene
│   ├── Tests/
│   │   ├── Editor/                     # Edit Mode tests
│   │   └── Runtime/                    # Play Mode tests
│   └── TedLiou.Demo.asmdef
├── Packages/
│   └── manifest.json
└── ProjectSettings/
```

## ARCHITECTURE

- **Namespace**: `TedLiou.NativeBrowser` (Public API) and `TedLiou.NativeBrowser.Internal` (JSON models and low-level bridge).
- **Callback Pattern**: `NativeBrowserCallbackReceiver` is a singleton that auto-creates a GameObject named "NativeBrowserCallback". It uses `DontDestroyOnLoad` to persist across scenes and receives `UnitySendMessage` from Android.
- **Asmdef**: Uses Assembly Definitions to isolate Runtime, Editor, and Demo code, improving compilation times and dependency management.

## ANDROID INTEGRATION

- **.aar Plugin**: The native code is compiled into `NativeBrowser.aar` and placed in `Assets/Plugins/Android/`.
- **mainTemplate.gradle**: Unity's custom Gradle template is used to include necessary Maven dependencies that are not bundled in the .aar:
  - `kotlin-stdlib`
  - `kotlinx-coroutines-android`
  - `androidx.browser:browser`
  - `androidx.webkit:webkit`
  - `androidx.activity:activity-ktx`

## CONVENTIONS

- **JNI Bridge**: Use `AndroidJavaClass` and `AndroidJavaObject` to communicate with `com.tedliou.android.browser.BrowserManager`.
- **Async/Callbacks**: Use Action delegates in C# to handle results from the native side.

## NOTES

- **Library/**: Contains 20k+ cached files. **NEVER** search inside it.
- **NativeBrowserCallback**: This GameObject name is hardcoded in the Android bridge; do not rename it.
- **Test Runner**: Use Unity Test Framework for both Edit Mode and Play Mode tests.
- **ProGuard**: If adding new classes called via JNI, update the ProGuard rules in the Android project.
