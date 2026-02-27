# Build Commands & Pipeline

## Environment Requirements

| Tool | Requirement |
|------|-------------|
| Unity | Unity 6 (6000.x) — set `UNITY_INSTALL` to your Unity editor root |
| JDK (for Gradle) | Unity bundled: `<UNITY_INSTALL>/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK` |
| Android SDK | Standard Android SDK (auto-detected via `local.properties`) |
| Gradle | 9.3.1 (wrapper in `src/android/gradle/wrapper/`) |
| AGP | 9.0.1 |

## Build .aar (Android Library)

```bash
# From repository root
JAVA_HOME="<UNITY_INSTALL>/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" src/android/gradlew assembleRelease
```

**Output**: `src/android/app/build/outputs/aar/app-release.aar`

## Copy .aar to Unity

```bash
copy src\android\app\build\outputs\aar\app-release.aar src\unity\Assets\Plugins\Android\NativeBrowser.aar
```

Or use: `tools/copy-aar.sh`

## Run Android Unit Tests

```bash
JAVA_HOME="<UNITY_INSTALL>/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" src/android/gradlew test
```

## Build Unity APK (Headless)

```bash
"<UNITY_INSTALL>/Editor/Unity" -quit -batchmode -nographics -projectPath src/unity -executeMethod TedLiou.Build.BuildScript.BuildAndroid -buildTarget Android -logFile - 2>&1
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
- `create-release.sh` — Build all release artifacts (`.aar` + `.tgz`), optionally publish GitHub Release

## Release Pipeline

### Automated (CI)

Push a version tag to trigger the full release:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

This triggers `.github/workflows/release.yml` which:
1. Builds `.aar` from source
2. Packs UPM `.tgz` tarball
3. Creates GitHub Release with `.aar` + `.tgz` attached
4. Splits UPM package to the `upm` branch via `git subtree split`
5. Tags the `upm` branch with the version

### Manual (Local)

```bash
./tools/create-release.sh            # Build artifacts only
./tools/create-release.sh --publish  # Build + create GitHub Release
./tools/create-release.sh --draft    # Build + create draft release
```

### Version Management

Single source of truth: `src/unity/Assets/Plugins/NativeBrowser/package.json`

When releasing:
1. Update `version` in `package.json`
2. Update `CHANGELOG.md`
3. Commit, tag (`v{version}`), push

## JaCoCo Coverage

```bash
# Generate coverage reports
gradlew test jacocoTestReport

# Verify 85%+ threshold
gradlew jacocoTestCoverageVerification
```

Reports: `src/android/app/build/reports/jacoco/jacocoTestReport/html/index.html`

## CI/CD

GitHub Actions workflows at [`.github/workflows/`](../.github/workflows/):

| Workflow | File | Jobs |
|---------|------|------|
| Android CI | `android.yml` | `build` (assembleRelease), `test` (unit tests), `coverage` (JaCoCo ≥85%) |
| Android Lint | `lint.yml` | `lint` (Android Lint) |
| Release | `release.yml` | `build-aar`, `release` (GitHub Release), `publish-upm` (subtree split) |

Release workflow triggers: push tag `v*`.
Triggers: push to `master`, pull requests to `master`.

### Monitor CI from Local

```bash
# List recent workflow runs
gh run list --repo tedliou/unity-native-browser --limit 5

# View specific run details
gh run view <run-id> --repo tedliou/unity-native-browser

# View failure logs
gh run view <run-id> --repo tedliou/unity-native-browser --log-failed

# Watch a run in real-time
gh run watch <run-id> --repo tedliou/unity-native-browser
```

Requires: [GitHub CLI](https://cli.github.com/) (`gh`) authenticated via `gh auth login`.

## Known Build Issues

| Issue | Solution |
|-------|----------|
| `JAVA_HOME not set` | Set to Unity's bundled OpenJDK path (see above) |
| Gradle daemon lock files | `gradlew --stop` then retry |
| `local.properties` missing | Auto-created by Gradle; contains `sdk.dir`, do NOT commit |
| AGP 9.0 + Kotlin plugin | Do NOT add `kotlin-android` to root `build.gradle.kts` |
