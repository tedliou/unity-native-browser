param([string]$BuildDir = "E:\webgl-build")

$frameworkFile = Get-ChildItem "$BuildDir\Build\*.framework.js.br" | Select-Object -First 1
if (-not $frameworkFile) {
    Write-Output "ERROR: No framework.js.br found in $BuildDir\Build\"
    exit 1
}

Write-Output "Decompressing: $($frameworkFile.FullName)"
$bytes = [System.IO.File]::ReadAllBytes($frameworkFile.FullName)
$inputMs = New-Object System.IO.MemoryStream(@(,$bytes))
$outputMs = New-Object System.IO.MemoryStream
$bs = New-Object System.IO.Compression.BrotliStream($inputMs, [System.IO.Compression.CompressionMode]::Decompress)
$bs.CopyTo($outputMs)
$text = [System.Text.Encoding]::UTF8.GetString($outputMs.ToArray())
$bs.Dispose()
$inputMs.Dispose()
$outputMs.Dispose()

Write-Output "Framework JS size: $($text.Length) chars"
Write-Output ""

$patterns = @("NB_Initialize", "NB_Open", "NB_Close", "NB_IsOpen", "NB_ExecuteJavaScript", "NB_InjectJavaScript", "NB_SendPostMessage", "NB_Refresh", "NB_State")
foreach ($p in $patterns) {
    if ($text.Contains($p)) {
        Write-Output "  FOUND: $p"
    } else {
        Write-Output "  MISSING: $p"
    }
}
