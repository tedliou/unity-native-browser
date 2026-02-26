# ANDROID PROJECT — NativeBrowser

## OVERVIEW

Kotlin Android project. Scaffold only (AGP 9.0.1, Gradle 9.3.1). Builds to .aar for Unity. Package: `com.tedliou.android.browser`.

## STRUCTURE

```
src/android/
├── app/
│   ├── build.gradle.kts          # Module config — currently android.application, needs → android.library for .aar
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # No Activity declared yet
│       │   ├── java/.../browser/     # Empty — feature code goes here
│       │   └── res/                  # Default resources only
│       ├── test/.../browser/         # JUnit 4 boilerplate
│       └── androidTest/.../browser/  # Instrumented test boilerplate
├── build.gradle.kts              # Root — applies AGP plugin
├── settings.gradle.kts           # Single module ":app"
├── gradle.properties             # AndroidX enabled, official Kotlin style
└── gradle/
    ├── wrapper/                  # Gradle 9.3.1
    └── libs.versions.toml        # Version catalog
```

## CONVENTIONS

- Package: `com.tedliou.android.browser`
- Java compatibility: 11
- AndroidX: required (`android.useAndroidX=true`)
- R class: non-transitive (`android.nonTransitiveRClass=true`)
- Kotlin style: official
- ProGuard: configured but minify disabled for release

## WHERE TO LOOK

| Task | File |
|------|------|
| Add dependencies | `gradle/libs.versions.toml` + `app/build.gradle.kts` |
| Add Android permissions | `app/src/main/AndroidManifest.xml` |
| Feature code | `app/src/main/java/com/tedliou/android/browser/` |
| Unit tests | `app/src/test/java/com/tedliou/android/browser/` |
| Instrumented tests | `app/src/androidTest/java/com/tedliou/android/browser/` |

## CRITICAL: .aar Conversion

The app module is currently `com.android.application`. To produce .aar:
1. Change plugin in `app/build.gradle.kts`: `android.application` → `android.library`
2. Remove `applicationId` from defaultConfig
3. Add `android.library` plugin to version catalog
4. Optionally: create separate demo app module that depends on the library

## DEPENDENCIES (current)

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.core:core-ktx | 1.17.0 | Kotlin extensions |
| androidx.appcompat | 1.7.1 | Compat |
| com.google.android.material | 1.13.0 | Material Design |
| junit | 4.13.2 | Unit tests |
| androidx.test.ext:junit | 1.3.0 | Android test runner |
| androidx.test.espresso | 3.7.0 | UI testing |

## NOTES

- WebView requires `INTERNET` permission — not yet in manifest.
- Custom Tabs requires `androidx.browser:browser` — not yet in version catalog.
- ProGuard: minify disabled; add `-keepclassmembers` for any WebView JS interface.
- Build as .aar: change `android.application` → `android.library`, remove `applicationId`.
- `android.library` plugin not yet in `libs.versions.toml` — add before conversion.
- `local.properties` is auto-generated — never commit.
- No feature Kotlin code exists yet; production code goes in `java/.../browser/`.
