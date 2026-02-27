# Build Commands & Pipeline

## Environment Requirements

| Tool | Path / Version |
|------|---------------|
| Unity | `C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe` |
| JDK (for Gradle) | Unity bundled: `C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK` |
| Android SDK | `C:\Users\Ted\AppData\Local\Android\Sdk` |
| Gradle | 9.3.1 (wrapper in `src/android/gradle/wrapper/`) |
| AGP | 9.0.1 |

## Build .aar (Android Library)

```bash
# From repository root (Windows)
set JAVA_HOME=C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK
E:\android-browser-for-unity\src\android\gradlew.bat assembleRelease
```

**Output**: `src/android/app/build/outputs/aar/app-release.aar`

## Copy .aar to Unity

```bash
copy src\android\app\build\outputs\aar\app-release.aar src\unity\Assets\Plugins\Android\NativeBrowser.aar
```

Or use: `tools/copy-aar.sh`

## Run Android Unit Tests

```bash
set JAVA_HOME=C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK
E:\android-browser-for-unity\src\android\gradlew.bat test
```

## Build Unity APK (Headless)

```bash
"C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" -quit -batchmode -nographics -projectPath "E:\android-browser-for-unity\src\unity" -executeMethod TedLiou.Build.BuildScript.BuildAndroid -buildTarget Android -logFile - 2>&1
```

**Output**: `build/NativeBrowser.apk`

## Deploy to Device

```bash
adb install -r build/NativeBrowser.apk
```

## Full Pipeline

1. Build .aar → `gradlew assembleRelease`
2. Copy .aar → `copy ... NativeBrowser.aar`
3. Run Android tests → `gradlew test`
4. Build APK → Unity headless build
5. Deploy → `adb install`

Automation scripts in [`tools/`](../tools/):
- `build.sh` — Android .aar build
- `test.sh` — Run Android unit tests
- `copy-aar.sh` — Copy .aar to Unity plugins
- `deploy.sh` — Build APK + adb install
- `clean.sh` — Clean build artifacts

## JaCoCo Coverage

```bash
# Generate coverage reports
gradlew test jacocoTestReport

# Verify 85%+ threshold
gradlew jacocoTestCoverageVerification
```

Reports: `src/android/app/build/reports/jacoco/jacocoTestReport/html/index.html`

## CI/CD

GitHub Actions workflow at [`.github/workflows/`](../.github/workflows/).

## Known Build Issues

| Issue | Solution |
|-------|----------|
| `JAVA_HOME not set` | Set to Unity's bundled OpenJDK path (see above) |
| Gradle daemon lock files | `gradlew --stop` then retry |
| `local.properties` missing | Auto-created by Gradle; contains `sdk.dir`, do NOT commit |
| AGP 9.0 + Kotlin plugin | Do NOT add `kotlin-android` to root `build.gradle.kts` |
