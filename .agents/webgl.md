# WebGL Architecture

## Overview

The WebGL platform support provides NativeBrowser functionality for Unity WebGL builds:

- **WebView** (`BrowserType.WebView`) → iframe overlay on top of the Unity canvas
- **CustomTab / SystemBrowser** → `window.open()` to a new browser tab

No custom WebGL template required. Works with any Unity default or custom template.

## File Layout

```
src/unity/Assets/Plugins/NativeBrowser/
├── Runtime/
│   ├── Internal/
│   │   └── WebGLBridge.cs          # IPlatformBridge implementation (C# ↔ jslib interop)
│   └── Plugins/
│       └── WebGL/
│           └── NativeBrowser.jslib  # JavaScript plugin (iframe management, DOM, events)
└── Editor/
    └── WebGLPluginValidator.cs      # Build validation (checks .jslib presence)
```

## Architecture

### Bridge Pattern

Same `IPlatformBridge` interface as Android and Windows:

```
NativeBrowser.cs (static API)
  ├── #if UNITY_ANDROID → AndroidBridge (JNI → Kotlin)
  ├── #elif UNITY_WEBGL  → WebGLBridge  (DllImport("__Internal") → .jslib)
  ├── #elif UNITY_STANDALONE_WIN → WindowsBridge (DllImport → Rust DLL)
  └── #else → EditorBridge (debug stubs)
```

### WebGLBridge.cs

- Platform-guarded with `#if UNITY_WEBGL && !UNITY_EDITOR`
- Uses `[DllImport("__Internal")]` P/Invoke to call `.jslib` functions
- All 8 `IPlatformBridge` methods implemented
- Each method wraps the native call in try/catch with `Debug.LogWarning`
- Input validation (null/empty checks) before calling JavaScript

### NativeBrowser.jslib

Unity's `.jslib` plugin format: `mergeInto(LibraryManager.library, { ... })`.

**Shared State** (`$NB_State`):
- `initialized`, `isOpen`, `currentType`
- DOM references: `backdrop`, `container`, `iframe`, `closeButton`
- Cross-origin tracking: `isCrossOrigin`, `iframeOrigin`

**Internal Helpers** (prefixed with `$NB_`):
| Helper | Purpose |
|--------|---------|
| `$NB_FindCanvas` | Multi-fallback canvas finder: `#unity-canvas` → `Module.canvas` → first `<canvas>` |
| `$NB_SendCallback` | Wraps `SendMessage(gameObjectName, event, json)` to C# |
| `$NB_ParseConfig` | `UTF8ToString` + `JSON.parse` |
| `$NB_InjectStyles` | Creates `<style>` element with backdrop/container/iframe CSS |
| `$NB_GetAlignmentClass` | Maps `Alignment` enum string → CSS flex class |
| `$NB_CheckCrossOrigin` | Compares URL origin to `window.location.origin` |
| `$NB_CreateDOM` | Builds backdrop → container → iframe → close button |
| `$NB_DestroyDOM` | Removes all DOM elements |
| `$NB_SetupIframeListeners` | iframe `load` event + `window.message` listener |
| `$NB_RemoveIframeListeners` | Cleanup event listeners |

**Exported Functions** (called from C# via DllImport):
| Function | Behavior |
|----------|----------|
| `NB_WebGL_Initialize` | Store callback target name, pre-create DOM, setup listeners |
| `NB_WebGL_Open` | WebView → iframe overlay with size/alignment; CustomTab/SystemBrowser → `window.open()` |
| `NB_WebGL_Close` | Hide backdrop, clear iframe src, fire `OnClosed` |
| `NB_WebGL_Refresh` | Same-origin: `location.reload()`; cross-origin: re-set `src` |
| `NB_WebGL_IsOpen` | Return `NB_State.isOpen` |
| `NB_WebGL_ExecuteJavaScript` | Same-origin: `iframe.contentWindow.eval()`; cross-origin: log warning |
| `NB_WebGL_InjectJavaScript` | Same-origin: create `<script>` in iframe; cross-origin: log warning |
| `NB_WebGL_SendPostMessage` | `iframe.contentWindow.postMessage()` — works cross-origin |

### DOM Structure

```
<body>
  ... (existing page content, Unity canvas) ...
  <div id="nb-backdrop" class="nb-visible nb-align-center">     ← semi-transparent overlay
    <div id="nb-container" style="width:Xpx; height:Ypx">       ← white rounded box
      <button id="nb-close-btn">×</button>                       ← close button
      <iframe id="nb-iframe" src="..." sandbox="..."></iframe>    ← web content
    </div>
  </div>
</body>
```

### Alignment

9-point alignment via CSS flex classes on `#nb-backdrop`:

| Alignment | CSS |
|-----------|-----|
| CENTER | `align-items: center; justify-content: center` |
| LEFT | `align-items: center; justify-content: flex-start; padding-left: 16px` |
| TOP_RIGHT | `align-items: flex-start; justify-content: flex-end; padding: 16px` |
| (etc.) | Similar flex patterns |

### Size Calculation

Container size is calculated relative to the Unity canvas (or viewport fallback):
```javascript
var refWidth = canvas ? canvas.clientWidth : window.innerWidth;
var w = Math.round(refWidth * config.width);  // config.width is 0..1 float
```

### Cross-Origin Handling

| Feature | Same-Origin | Cross-Origin |
|---------|-------------|--------------|
| iframe display | ✅ | ✅ |
| `load` event | ✅ (with URL) | ✅ (URL from `src` attr) |
| `postMessage` | ✅ | ✅ (with target origin) |
| `ExecuteJavaScript` | ✅ | ❌ (logs warning, fires OnError) |
| `InjectJavaScript` | ✅ | ❌ (logs warning, fires OnError) |

### Event Flow

```
[C#] NativeBrowser.Open(WebView, config)
  → [C#] WebGLBridge.Open(type, configJson)
    → [JS] NB_WebGL_Open(typePtr, configJsonPtr)
      → Creates/shows iframe overlay
      → Fires OnPageStarted via SendMessage

[JS] iframe 'load' event
  → [JS] NB_SendCallback("OnPageFinished", json)
    → [C#] NativeBrowserCallbackReceiver.OnPageFinished(json)
      → [C#] NativeBrowser.RaiseOnPageFinished(url)

[JS] window 'message' event (from iframe postMessage)
  → [JS] NB_SendCallback("OnPostMessage", json)
    → [C#] NativeBrowserCallbackReceiver.OnPostMessage(json)
      → [C#] NativeBrowser.RaiseOnPostMessage(message)
```

### Ignored Config Properties

These `BrowserConfig` properties are silently ignored on WebGL:
- `deepLinkPatterns` — no deep link concept in browser
- `closeOnDeepLink` — no deep link concept
- `userAgent` — cannot set iframe user agent

## Editor Script

`WebGLPluginValidator.cs` — runs `[InitializeOnLoad]` when build target is WebGL:
- Checks `.jslib` file exists in the package
- Warns if missing (would cause `EntryPointNotFoundException` at runtime)
- Menu: **Tools > NativeBrowser > Validate WebGL Setup**

## Testing

### Test Server

`tools/webgl-test-server.py` — Python 3 HTTP server (stdlib only):

```bash
python tools/webgl-test-server.py [BUILD_DIR] [--port PORT] [--cross-origin-port PORT2]
```

Features:
- Serves Unity WebGL build with required headers (`Cross-Origin-Opener-Policy`, `Cross-Origin-Embedder-Policy`)
- Same-origin test page at `/test/same-origin`
- Cross-origin test page on separate port at `/test/cross-origin`
- Log collection API: `POST /api/logs`, `GET /api/logs`, `GET /api/logs/clear`
- Health check: `GET /api/health`

### Build Requirements

| Tool | Requirement |
|------|-------------|
| Unity | 6000.x with **WebGL Build Support** module installed |
| Python | 3.x (for test server — stdlib only, no pip packages needed) |

### Build Command

```bash
"<UNITY_INSTALL>/Editor/Unity" -quit -batchmode -nographics \
  -projectPath E:/test-project \
  -executeMethod TestProject.Build.BuildScript.BuildWebGL \
  -buildTarget WebGL -logFile - 2>&1
```

## Anti-Patterns

- **DO NOT** use `document.getElementById("unity-canvas")` only — not all templates use this ID
- **DO NOT** set `z-index` lower than 10000 — Unity canvas and other overlays may compete
- **DO NOT** try to access `iframe.contentWindow` properties on cross-origin — use `postMessage` instead
- **DO NOT** forget `sandbox` attribute — omitting it is a security risk
- All `.jslib` helper functions must be prefixed with `$` for Emscripten's dependency system
