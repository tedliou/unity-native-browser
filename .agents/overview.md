# Project Overview

Android native browser plugin (.aar) for Unity. Provides WebView, Custom Tabs, and system browser launch via a C# → Kotlin bridge.

## Status

**v1.0.0** — Production complete.

## Repository Map

```
.
├── src/android/          → Android Gradle project (Kotlin) → builds .aar
├── src/unity/            → Unity 6000.3.10f1 project (C#, URP)
├── tools/                → Shell scripts (build, test, deploy, clean, copy-aar)
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
| [guides/ssh-signing-setup.md](guides/ssh-signing-setup.md) | GitHub Verified commits via SSH signing |

## Tech Stack

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

## Features

- **WebView**: open, close, refresh, PostMessage, JS execute/inject, configurable size/alignment, tap-outside-to-close, deep link interception, back button navigation
- **Custom Tabs**: Chrome Custom Tabs with toolbar color, animations, share, warmup/prefetch
- **System Browser**: ACTION_VIEW fallback
- **Events**: page lifecycle, errors, PostMessage, JS results, deep links, closed
- **Testing**: Android unit tests (Robolectric + MockK), Unity Edit Mode + Play Mode tests
