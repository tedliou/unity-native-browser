$logFile = "E:\android-browser-for-unity\test-results\editmode.log"
$xmlFile = "E:\android-browser-for-unity\test-results\editmode.xml"
$timeout = 600  # 10 minutes

# Clean old results
if (Test-Path $xmlFile) { Remove-Item $xmlFile -Force }
if (Test-Path $logFile) { Remove-Item $logFile -Force }

# Launch Unity
$proc = Start-Process -FilePath "C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Unity.exe" `
    -ArgumentList "-batchmode -nographics -projectPath `"E:\android-browser-for-unity\src\unity`" -runTests -testPlatform EditMode -testResults `"$xmlFile`" -logFile `"$logFile`"" `
    -PassThru
Write-Host "Unity launched PID=$($proc.Id)"

$start = Get-Date
while ((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds -lt $timeout) {
    Start-Sleep -Seconds 10
    
    # Check if XML result file exists and has content
    if (Test-Path $xmlFile) {
        $size = (Get-Item $xmlFile).Length
        if ($size -gt 100) {
            Write-Host "Test results XML found ($size bytes)"
            Start-Sleep -Seconds 3
            
            [xml]$xml = Get-Content $xmlFile
            $root = $xml.'test-run'
            Write-Host "Total: $($root.total), Passed: $($root.passed), Failed: $($root.failed), Skipped: $($root.skipped)"
            Write-Host "Result: $($root.result)"
            
            if ([int]$root.failed -gt 0) {
                Write-Host "`nFAILURES:"
                $xml.'test-run'.SelectNodes("//test-case[@result='Failed']") | ForEach-Object {
                    Write-Host "  FAILED: $($_.fullname)"
                    if ($_.failure -and $_.failure.message) {
                        Write-Host "    Message: $($_.failure.message.'#text')"
                    }
                }
            }
            
            # Kill Unity (it hangs on DNS)
            Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
            Start-Sleep -Seconds 2
            Write-Host "Done."
            exit 0
        }
    }
    
    # Check if Unity exited on its own (crash?)
    if ($proc.HasExited) {
        Write-Host "Unity exited with code $($proc.ExitCode)"
        if (Test-Path $xmlFile) {
            [xml]$xml = Get-Content $xmlFile
            $root = $xml.'test-run'
            Write-Host "Total: $($root.total), Passed: $($root.passed), Failed: $($root.failed), Skipped: $($root.skipped)"
        } else {
            Write-Host "No XML results found. Check log."
        }
        exit 0
    }
    
    $elapsed = [math]::Round((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds)
    Write-Host "Waiting... ($elapsed`s)"
}

Write-Host "TIMEOUT after $timeout seconds"
Get-Process -Name Unity -ErrorAction SilentlyContinue | Stop-Process -Force
exit 1
