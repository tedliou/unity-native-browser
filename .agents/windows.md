# Windows Architecture

## Overview

Windows WebView2 support for the NativeBrowser Unity plugin. A Rust native layer (`NativeBrowserWebView.dll`) provides WebView2 integration via P/Invoke, matching the existing Android bridge pattern.

## Two Modes

| Mode | Window Style | Parent HWND | Use Case |
|------|-------------|-------------|----------|
| **Editor** | Standalone topmost window, resizable, no close/min/max buttons | None (`IntPtr.Zero`) | Unity Editor development/debugging |
| **Build** | Borderless child window, embedded in Unity's game window | `FindWindow("UnityWndClass", null)` | Standalone EXE for players |

## Package Structure

### Rust Native Layer (`src/windows/`)

| File | Responsibility |
|------|----------------|
| `src/lib.rs` | FFI entry points (`nb_*` functions), global state, command dispatch |
| `src/threading.rs` | COM STA thread with message pump, `mpsc::channel<Command>` |
| `src/window.rs` | Win32 window creation (Editor standalone / Build child), `wnd_proc` |
| `src/webview.rs` | WebView2 environment, controller, navigation, JS execution, PostMessage bridge |
| `src/callback.rs` | `UnityCallback` function pointer storage, JSON event senders |
| `src/config.rs` | `BrowserConfig` deserialization from Unity's JSON |
| `src/dpi.rs` | DPI awareness, scale-aware window sizing, screen work area |
| `src/error.rs` | `BrowserError`, `ErrorKind` enum, JSON escape utility |

**Build output**: `src/windows/target/release/NativeBrowserWebView.dll`

### C# Integration (`src/unity/Assets/Plugins/NativeBrowser/Runtime/Internal/`)

| File | Responsibility |
|------|----------------|
| `WindowsBridge.cs` | `IPlatformBridge` implementation, P/Invoke declarations, HWND lookup |
| `WindowsCallbackDispatcher.cs` | `MonoBehaviour` that drains `ConcurrentQueue` on `Update()` |

### Unity Plugin

| Path | Description |
|------|-------------|
| `Assets/Plugins/NativeBrowser/Runtime/Plugins/x86_64/NativeBrowserWebView.dll` | Release DLL (Windows x64) |
| `Assets/Plugins/NativeBrowser/Runtime/Plugins/x86_64/NativeBrowserWebView.dll.meta` | Plugin settings: Windows x64 + Editor |

## Threading Model

```
Unity Main Thread                          COM STA Thread
─────────────────                          ───────────────
C# calls WindowsBridge.Open()
  → P/Invoke nb_open()
    → mpsc::Sender.send(Command::Open)
      → PostThreadMessageW(WM_APP+1)        GetMessageW() wakes up
                                              → receiver.try_recv()
                                              → handle_command(Open)
                                                → window::create_browser_window()
                                                → WebViewInstance::create()
                                                → callback::send_page_started()
                                                   ↓
                                            UnityCallback fn ptr fires
                                              ↓
C# OnNativeCallback (from STA thread)
  → Marshal.PtrToStringAnsi()
  → ConcurrentQueue.Enqueue()
      ↓
C# Update() on main thread
  → ConcurrentQueue.TryDequeue()
  → DispatchEvent() → NativeBrowser.RaiseOn*()
```

**Key threading rules:**
- All `nb_*` FFI calls are safe from any thread (they send via channel)
- All WebView2 COM operations run exclusively on the STA thread
- Callbacks fire from the STA thread → must be queued for Unity's main thread
- `ConcurrentQueue` bridges the STA thread → Unity main thread gap
- Max 64 callbacks processed per `Update()` frame to avoid stalls

## FFI API

```rust
pub extern "C" fn nb_initialize(callback_fn: UnityCallback, is_editor: bool);
pub extern "C" fn nb_open(type_str: *const c_char, config_json: *const c_char, parent_hwnd: isize);
pub extern "C" fn nb_close();
pub extern "C" fn nb_refresh();
pub extern "C" fn nb_is_open() -> bool;
pub extern "C" fn nb_execute_js(script: *const c_char, request_id: *const c_char);
pub extern "C" fn nb_inject_js(script: *const c_char);
pub extern "C" fn nb_send_post_message(message: *const c_char);
pub extern "C" fn nb_destroy();
```

**Callback signature**: `extern "C" fn(event_name: *const c_char, json_data: *const c_char)`

## DPI Handling

| Step | Description |
|------|-------------|
| 1. Process-level | `SetProcessDpiAwarenessContext(PER_MONITOR_AWARE_V2)` at init |
| 2. Default size | 1024×768 base pixels |
| 3. Scale | Multiply by DPI factor (e.g., 2.0 for 4K 192 DPI) |
| 4. Clamp | Max 90% of working area, min 640×480 |
| 5. Center | Center on primary monitor's working area |
| 6. Post-create | Re-measure actual DPI after window creation, adjust if needed |

## Window Behavior

### Editor Mode
- `WS_OVERLAPPEDWINDOW` minus `WS_MINIMIZEBOX | WS_MAXIMIZEBOX`
- `WS_EX_TOPMOST` — always on top
- `WM_CLOSE` blocked in `wnd_proc`
- `SC_CLOSE`, `SC_MINIMIZE`, `SC_MAXIMIZE` blocked via `WM_SYSCOMMAND`
- System menu close button grayed out via `EnableMenuItem`
- Resizable (drag borders), resets to default on restart

### Build Mode
- `WS_CHILD | WS_CLIPCHILDREN | WS_VISIBLE` — borderless child
- Parented to Unity's `UnityWndClass` HWND
- Fills parent client area
- No title bar, no border — looks embedded in the game

## Callback JSON Shapes

Identical to Android side:

| Event | JSON Shape |
|-------|-----------|
| `OnPageStarted` | `{"url":"..."}` |
| `OnPageFinished` | `{"url":"..."}` |
| `OnError` | `{"type":"...","message":"...","url":"...","requestId":"..."}` |
| `OnPostMessage` | `{"message":"..."}` |
| `OnJsResult` | `{"requestId":"...","result":"..."}` |
| `OnDeepLink` | `{"url":"..."}` |
| `OnClosed` | `{}` |

## Bridge Selection

In `NativeBrowser.cs`:

```csharp
#if UNITY_ANDROID && !UNITY_EDITOR
    private static readonly IPlatformBridge bridge = new AndroidBridge();
#elif UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
    private static readonly IPlatformBridge bridge = new WindowsBridge();
#else
    private static readonly IPlatformBridge bridge = new EditorBridge();
#endif
```

## Dependencies

### Rust Crates (`Cargo.toml`)

| Crate | Version | Purpose |
|-------|---------|---------|
| `webview2-com` | 0.38 | WebView2 COM bindings, async handler creation |
| `windows` | 0.61 | Win32 API: windowing, COM, DPI, threading |
| `serde` + `serde_json` | 1 | JSON deserialization for BrowserConfig |
| `once_cell` | 1 | Lazy static initialization |

### Windows Features (from `Cargo.toml`)

- `Win32_Foundation` — HWND, RECT, BOOL, error codes
- `Win32_UI_WindowsAndMessaging` — Window creation, message pump, system commands
- `Win32_Graphics_Gdi` — UpdateWindow, stock brushes
- `Win32_System_LibraryLoader` — GetModuleHandleW
- `Win32_System_Threading` — GetCurrentThreadId
- `Win32_System_Com` — CoInitializeEx, CoUninitialize
- `Win32_UI_HiDpi` — GetDpiForWindow, SetProcessDpiAwarenessContext
- `Win32_UI_Shell` — Shell utilities

## Build Commands

```powershell
# Build release DLL + copy to Unity
.\tools\build-windows.ps1

# Debug build
.\tools\build-windows.ps1 -Debug

# Build only (no copy)
.\tools\build-windows.ps1 -SkipCopy

# Manual cargo commands
cd src\windows
cargo test                    # Run 24 unit tests
cargo build --release         # Build release DLL
```

**Output**: `src\windows\target\release\NativeBrowserWebView.dll` → copied to `src\unity\Assets\Plugins\NativeBrowser\Runtime\Plugins\x86_64\`

## Testing

### Rust Unit Tests (24 tests)

| Module | Tests | Coverage |
|--------|-------|----------|
| `lib.rs` | 4 | `cstr_to_str` null/valid, `IS_OPEN` default, `IS_EDITOR` default |
| `config.rs` | 4 | Full config, minimal config, invalid JSON, empty JSON |
| `error.rs` | 4 | ErrorKind display, BrowserError to_json, empty fields, escape chars |
| `callback.rs` | 2 | No-callback safety, set/clear callback |
| `dpi.rs` | 4 | 1x sizing, 2x sizing, minimum enforcement, centered position |
| `threading.rs` | 2 | Spawn/destroy lifecycle, multiple commands |
| `webview.rs` | 2 | PostMessage bridge JS validation, struct type check |
| `window.rs` | 2 | UTF-16 conversion, WindowMode equality |

### C# Unit Tests (Unity Editor)

| File | Tests | Coverage |
|------|-------|----------|
| `WindowsBridgeApiTest.cs` | 20 | Type structure, IPlatformBridge conformance, P/Invoke declarations, DLL imports |
| `WindowsCallbackDispatcherTest.cs` | 20+ | Singleton, delegate signature, callback queue, all 7 events, ordering, cleanup |

### Integration Tests

| File | Tests | Coverage |
|------|-------|----------|
| `NativeBrowserIntegrationTest.cs` | Updated | Platform-conditional branches for Windows vs Editor bridge |

## Known Issues

| Issue | Workaround |
|-------|-----------|
| WebView2 Runtime not installed | End-user must install [WebView2 Runtime](https://developer.microsoft.com/en-us/microsoft-edge/webview2/) (bundled with Windows 11, optional on Windows 10) |
| Unity Editor HWND unavailable | Editor mode uses standalone window (by design) |
| Thread crash on exit | `Application.quitting` hook calls `NbDestroy()` to gracefully shutdown STA thread |
| DLL locked during rebuild | Close Unity before rebuilding the DLL |
