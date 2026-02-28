<#
.SYNOPSIS
    Build the Windows WebView2 native DLL and copy to Unity.

.DESCRIPTION
    Compiles the Rust native browser plugin (NativeBrowserWebView.dll) in release mode
    and copies the output to the Unity Plugins directory.

.PARAMETER Debug
    Build in debug mode instead of release.

.PARAMETER SkipCopy
    Build only, do not copy to Unity.

.EXAMPLE
    .\tools\build-windows.ps1              # Release build + copy
    .\tools\build-windows.ps1 -Debug       # Debug build + copy
    .\tools\build-windows.ps1 -SkipCopy    # Release build only
#>

param(
    [switch]$Debug,
    [switch]$SkipCopy
)

$ErrorActionPreference = "Stop"

# ─── Paths ──────────────────────────────────────────────────────────────────

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$RustProject = Join-Path $ProjectRoot "src\windows"
$UnityPlugins = Join-Path $ProjectRoot "src\unity\Assets\Plugins\x86_64"

# ─── Prerequisites ──────────────────────────────────────────────────────────

function Test-Prerequisites {
    Write-Host "Checking prerequisites..." -ForegroundColor Yellow

    # Check Rust/Cargo
    if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
        Write-Host "ERROR: cargo not found. Install Rust from https://rustup.rs" -ForegroundColor Red
        exit 1
    }

    $cargoVersion = & cargo --version 2>&1
    Write-Host "  Cargo: $cargoVersion" -ForegroundColor Yellow

    # Check Rust target
    $targets = & rustup target list --installed 2>&1
    if ($targets -notmatch "x86_64-pc-windows-msvc") {
        Write-Host "WARNING: x86_64-pc-windows-msvc target may not be installed" -ForegroundColor Yellow
        Write-Host "  Run: rustup target add x86_64-pc-windows-msvc" -ForegroundColor Yellow
    }
}

# ─── Build ──────────────────────────────────────────────────────────────────

function Invoke-Build {
    $buildType = if ($Debug) { "debug" } else { "release" }
    Write-Host "`n=== Windows WebView2 DLL Build ($buildType) ===" -ForegroundColor Green

    $buildArgs = @("build", "--target", "x86_64-pc-windows-msvc")
    if (-not $Debug) {
        $buildArgs += "--release"
    }

    Write-Host "Building Rust project..." -ForegroundColor Yellow
    Push-Location $RustProject
    try {
        & cargo @buildArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: Cargo build failed (exit code $LASTEXITCODE)" -ForegroundColor Red
            exit 1
        }
    }
    finally {
        Pop-Location
    }

    # Verify output
    $profile = if ($Debug) { "debug" } else { "release" }
    $dllPath = Join-Path $RustProject "target\x86_64-pc-windows-msvc\$profile\NativeBrowserWebView.dll"

    # Fallback: check without target triple (when default target is used)
    if (-not (Test-Path $dllPath)) {
        $dllPath = Join-Path $RustProject "target\$profile\NativeBrowserWebView.dll"
    }

    if (-not (Test-Path $dllPath)) {
        Write-Host "ERROR: DLL not found at expected path" -ForegroundColor Red
        Write-Host "  Checked: target\x86_64-pc-windows-msvc\$profile\NativeBrowserWebView.dll" -ForegroundColor Red
        Write-Host "  Checked: target\$profile\NativeBrowserWebView.dll" -ForegroundColor Red
        exit 1
    }

    $dllSize = (Get-Item $dllPath).Length / 1KB
    Write-Host "  Built: $dllPath ($([math]::Round($dllSize, 1)) KB)" -ForegroundColor Green

    return $dllPath
}

# ─── Copy ───────────────────────────────────────────────────────────────────

function Copy-ToUnity {
    param([string]$DllPath)

    Write-Host "`nCopying DLL to Unity..." -ForegroundColor Yellow

    # Ensure destination directory exists
    if (-not (Test-Path $UnityPlugins)) {
        New-Item -ItemType Directory -Path $UnityPlugins -Force | Out-Null
        Write-Host "  Created: $UnityPlugins" -ForegroundColor Yellow
    }

    $dest = Join-Path $UnityPlugins "NativeBrowserWebView.dll"
    Copy-Item -Path $DllPath -Destination $dest -Force
    Write-Host "  Copied: $dest" -ForegroundColor Green
}

# ─── Run Tests ──────────────────────────────────────────────────────────────

function Invoke-Tests {
    Write-Host "`nRunning Rust unit tests..." -ForegroundColor Yellow

    Push-Location $RustProject
    try {
        & cargo test
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: Rust tests failed (exit code $LASTEXITCODE)" -ForegroundColor Red
            exit 1
        }
        Write-Host "  All tests passed" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}

# ─── Main ───────────────────────────────────────────────────────────────────

function Main {
    Test-Prerequisites
    Invoke-Tests
    $dllPath = Invoke-Build

    if (-not $SkipCopy) {
        Copy-ToUnity -DllPath $dllPath
    }

    Write-Host "`nBuild successful!" -ForegroundColor Green
    if (-not $SkipCopy) {
        Write-Host "  Output: src\unity\Assets\Plugins\x86_64\NativeBrowserWebView.dll" -ForegroundColor Green
    }
}

Main
