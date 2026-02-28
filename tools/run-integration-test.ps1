$logFile = "E:\android-browser-for-unity\test-results\integration.log"
$timeout = 600  # 10 minutes

# Clean old results
if (Test-Path $logFile) { Remove-Item $logFile -Force }

# Launch Unity
$proc = Start-Process -FilePath "C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" `
    -ArgumentList "-batchmode -nographics -projectPath `"E:\android-browser-for-unity\src\unity`" -executeMethod TedLiou.Build.WindowsIntegrationTest.RunEditorTest -logFile `"$logFile`"" `
    -PassThru
Write-Host "Unity launched PID=$($proc.Id)"

$start = Get-Date
while ((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds -lt $timeout) {
    Start-Sleep -Seconds 10
    
    if (Test-Path $logFile) {
        $content = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
        if ($content -match "\[WindowsIntegrationTest\] =+ RESULTS =+") {
            Write-Host "Integration test completed!"
            Start-Sleep -Seconds 2
            
            # Extract results
            $lines = Get-Content $logFile
            $inResults = $false
            foreach ($line in $lines) {
                if ($line -match "\[WindowsIntegrationTest\]") {
                    Write-Host $line
                }
            }
            
            Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
            Start-Sleep -Seconds 2
            Write-Host "Done."
            exit 0
        }
        
        if ($content -match "Fatal Error") {
            Write-Host "FATAL ERROR detected!"
            Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
            exit 1
        }
    }
    
    if ($proc.HasExited) {
        Write-Host "Unity exited with code $($proc.ExitCode)"
        if (Test-Path $logFile) {
            $lines = Get-Content $logFile
            foreach ($line in $lines) {
                if ($line -match "\[WindowsIntegrationTest\]") {
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
