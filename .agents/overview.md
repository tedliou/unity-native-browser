# Project Overview

Native browser plugin for Unity. Provides WebView, Custom Tabs, and system browser launch across Android, Windows, and WebGL platforms.

## Status

**v1.1.0** — Multi-platform (Android production, Windows + WebGL in development).

## Repository Map

```
.
├── src/android/          → Android Gradle project (Kotlin) → builds .aar
├── src/unity/            → Unity 6 project (C#, URP)
├── src/windows/          → Windows Rust project → builds DLL (WebView2)
├── tools/                → Build scripts, test server, automation
├── docs/                 → Developer docs (EN + zh-TW)
├── .agents/              → AI knowledge base (this directory)
└── README.md
```

## Agent Knowledge Index

| File | Topic |
|------|-------|
| [overview.md](overview.md) | This file — project map and quick reference |
| [android.md](android.md) | Android architecture, packages, threading, ProGuard |
| [unity.md](unity.md) | Unity architecture, C# API, callback pattern, Gradle template |
| [build.md](build.md) | Build commands, environment, pipeline steps |
| [troubleshooting.md](troubleshooting.md) | Known bugs, root causes, and fix locations |
| [conventions.md](conventions.md) | Code style, anti-patterns, naming rules |
| [windows.md](windows.md) | Windows architecture, WebView2, Rust native layer |
| [webgl.md](webgl.md) | WebGL architecture, .jslib, iframe overlay, test server |

## Tech Stack

| Component | Version |
|-----------|---------|
| Gradle | 9.3.1 |
| AGP | 9.0.1 |
| Android compileSdk | 36 |
| Android minSdk | 28 |
| Kotlin code style | official |
| Unity | 6 (6000.x) |
| URP | 17.3.0 |
| Java compat | 11 |

## Features

- **WebView**: open, close, refresh, PostMessage, JS execute/inject, configurable size/alignment, tap-outside-to-close, deep link interception, back button navigation
- **Custom Tabs**: Chrome Custom Tabs with toolbar color, animations, share, warmup/prefetch
- **System Browser**: ACTION_VIEW fallback
- **Events**: page lifecycle, errors, PostMessage, JS results, deep links, closed
- **Testing**: Android unit tests (Robolectric + MockK), Unity Edit Mode + Play Mode tests
- **Windows WebView2**: in-app browser via WebView2 COM, Rust native DLL
- **WebGL**: iframe overlay for WebView, `window.open()` for CustomTab/SystemBrowser, no template setup required
