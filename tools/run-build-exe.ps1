$logFile = "E:\android-browser-for-unity\test-results\build.log"
$timeout = 600  # 10 minutes

# Clean old results
if (Test-Path $logFile) { Remove-Item $logFile -Force }

# Launch Unity build
$proc = Start-Process -FilePath "C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" `
    -ArgumentList "-batchmode -nographics -projectPath `"E:\android-browser-for-unity\src\unity`" -executeMethod TedLiou.Build.BuildScript.BuildWindows -logFile `"$logFile`"" `
    -PassThru
Write-Host "Unity build launched PID=$($proc.Id)"

$start = Get-Date
while ((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds -lt $timeout) {
    Start-Sleep -Seconds 10
    
    if (Test-Path $logFile) {
        $content = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
        
        # Check for build success
        if ($content -match "\[BuildWindows\] Build succeeded" -or $content -match "Build completed with a result of 'Succeeded'") {
            Write-Host "BUILD SUCCEEDED!"
            Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
            Start-Sleep -Seconds 2
            
            # Verify EXE exists
            $exePath = "E:\android-browser-for-unity\build\NativeBrowser.exe"
            if (Test-Path $exePath) {
                $size = [math]::Round((Get-Item $exePath).Length / 1MB, 1)
                Write-Host "EXE built: $exePath ($size MB)"
            } else {
                Write-Host "WARNING: EXE not found at $exePath"
            }
            
            # Verify DLL in build output
            $dllPath = "E:\android-browser-for-unity\build\NativeBrowser_Data\Plugins\x86_64\NativeBrowserWebView.dll"
            if (Test-Path $dllPath) {
                $size = [math]::Round((Get-Item $dllPath).Length / 1KB, 1)
                Write-Host "DLL in build: $dllPath ($size KB)"
            } else {
                Write-Host "WARNING: DLL not found in build output"
            }
            
            Write-Host "Done."
            exit 0
        }
        
        # Check for build failure
        if ($content -match "\[BuildWindows\] Build FAILED" -or $content -match "Build completed with a result of 'Failed'") {
            Write-Host "BUILD FAILED!"
            # Show error lines
            $lines = Get-Content $logFile
            foreach ($line in $lines) {
                if ($line -match "error|Error|FAILED|BuildWindows") {
                    Write-Host $line
                }
            }
            Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
            exit 1
        }
    }
    
    if ($proc.HasExited) {
        Write-Host "Unity exited with code $($proc.ExitCode)"
        if (Test-Path $logFile) {
            $content = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
            if ($content -match "Build completed") {
                Write-Host "Build completed (from log)"
            }
            $lines = Get-Content $logFile
            foreach ($line in $lines) {
                if ($line -match "\[BuildWindows\]|Build completed") {
                    Write-Host $line
                }
            }
        }
        exit 0
    }
    
    $elapsed = [math]::Round((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds)
    Write-Host "Waiting... ($elapsed`s)"
}

Write-Host "TIMEOUT after $timeout seconds"
Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
exit 1
