# UNITY PROJECT

## OVERVIEW

Unity 6000.3.10f1 (Unity 6) with URP 17.3.0. Scaffold only — no custom scripts or plugin code yet. Consumes Android .aar via `Assets/Plugins/Android/`.

## STRUCTURE

```
src/unity/
├── Assets/
│   ├── Scenes/               # Default scene only
│   └── Settings/             # URP renderer settings
├── Packages/
│   └── manifest.json         # Package dependencies
├── ProjectSettings/          # Unity project config
└── Library/                  # ⚠️ HUGE cache — always exclude from searches
```

## WHERE TO LOOK

| Task | Location |
|------|----------|
| C# scripts | `Assets/` — none yet, create feature scripts here |
| Plugin .aar | `Assets/Plugins/Android/` — standard Unity convention for Android plugins |
| Package manifest | `Packages/manifest.json` |
| Test framework | Already included: `com.unity.test-framework` 1.6.0 |
| Input system | Already included: `com.unity.inputsystem` 1.18.0 |

## KEY PACKAGES (non-default)

| Package | Version | Relevance |
|---------|---------|-----------|
| com.unity.test-framework | 1.6.0 | Required for Edit/Play Mode tests |
| com.unity.inputsystem | 1.18.0 | New Input System active |

## CONVENTIONS

- Unity C# scripts go in `Assets/` subdirectories
- Android .aar plugin: place in `Assets/Plugins/Android/`
- Tests: `Assets/Tests/Editor/` (Edit Mode) and `Assets/Tests/Runtime/` (Play Mode)
- AndroidJNI module already included — used for C# ↔ Java bridge via `AndroidJavaClass`/`AndroidJavaObject`

## NOTES

- `Library/` contains 20k+ cached files. NEVER search inside it.
- Project uses URP — irrelevant to browser plugin but affects rendering pipeline.
- `com.unity.modules.androidjni` already in manifest — JNI bridge available via `AndroidJavaClass`/`AndroidJavaObject`.
- No `.asmdef` files yet — add Assembly Definitions when creating test targets.
- Place .aar at `Assets/Plugins/Android/<name>.aar` — Unity auto-imports.
- Tests go in `Assets/Tests/Editor/` (Edit Mode) and `Assets/Tests/Runtime/` (Play Mode).
- C# bridge scripts: call Android with `AndroidJavaObject`, receive callbacks via `UnitySendMessage`.
- `com.unity.test-framework` 1.6.0 and `com.unity.inputsystem` 1.18.0 already installed.
- `src/unity/Library/` should be gitignored — currently tracked (clean up).
