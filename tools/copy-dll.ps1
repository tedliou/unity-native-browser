$src = "E:\android-browser-for-unity\src\windows\target\x86_64-pc-windows-msvc\release\NativeBrowserWebView.dll"
$dst = "E:\android-browser-for-unity\src\unity\Assets\Plugins\NativeBrowser\Runtime\Plugins\x86_64\NativeBrowserWebView.dll"

if (-not (Test-Path $src)) {
    $src = "E:\android-browser-for-unity\src\windows\target\release\NativeBrowserWebView.dll"
}

if (Test-Path $src) {
    Copy-Item -Path $src -Destination $dst -Force
    $size = [math]::Round((Get-Item $dst).Length / 1KB, 1)
    Write-Host "Copied DLL successfully ($size KB)"
} else {
    Write-Host "ERROR: DLL not found at either path"
    exit 1
}
