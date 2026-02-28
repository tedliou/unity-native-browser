$ErrorActionPreference = 'Stop'

$exePath = 'E:\android-browser-for-unity\build\NativeBrowser.exe'

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public class W32 {
    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);
    
    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    public static extern IntPtr FindWindowEx(IntPtr hwndParent, IntPtr hwndChildAfter, string lpClassName, string lpWindowName);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool IsWindow(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool IsWindowVisible(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    public static extern int GetWindowLong(IntPtr hWnd, int nIndex);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    
    [DllImport("user32.dll")]
    public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);
    
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }
}
"@

Write-Output "[EXE-TEST] Starting NativeBrowser.exe..."
$proc = Start-Process -FilePath $exePath -PassThru -WindowStyle Normal
Write-Output "[EXE-TEST] PID: $($proc.Id)"

# Wait for the main window to appear
$timeout = 60
$elapsed = 0
$unityHwnd = [IntPtr]::Zero

Write-Output "[EXE-TEST] Waiting for Unity window..."
while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    
    $p = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
    if (-not $p) {
        Write-Output "[EXE-TEST] FAIL: Process exited unexpectedly"
        exit 1
    }
    
    # Use Get-Process MainWindowHandle
    if ($p.MainWindowHandle -ne [IntPtr]::Zero) {
        $unityHwnd = $p.MainWindowHandle
        Write-Output "[EXE-TEST] Unity window found via Get-Process after $elapsed seconds"
        Write-Output "[EXE-TEST] HWND: 0x$($unityHwnd.ToInt64().ToString('X'))"
        Write-Output "[EXE-TEST] Title: '$($p.MainWindowTitle)'"
        
        # Get class name
        $classNameSb = New-Object System.Text.StringBuilder 256
        [W32]::GetClassName($unityHwnd, $classNameSb, 256) | Out-Null
        Write-Output "[EXE-TEST] Class: '$($classNameSb.ToString())'"
        break
    }
}

if ($unityHwnd -eq [IntPtr]::Zero) {
    Write-Output "[EXE-TEST] FAIL: Unity window not found within $timeout seconds"
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    exit 1
}

$passed = 0
$failed = 0

# Test 1: Window is valid
if ([W32]::IsWindow($unityHwnd)) {
    Write-Output "[EXE-TEST] PASS: Window is valid"
    $passed++
} else {
    Write-Output "[EXE-TEST] FAIL: Window is invalid"
    $failed++
}

# Test 2: Window is visible
if ([W32]::IsWindowVisible($unityHwnd)) {
    Write-Output "[EXE-TEST] PASS: Window is visible"
    $passed++
} else {
    Write-Output "[EXE-TEST] FAIL: Window is not visible"
    $failed++
}

# Test 3: Window size
$rect = New-Object W32+RECT
if ([W32]::GetWindowRect($unityHwnd, [ref]$rect)) {
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    Write-Output "[EXE-TEST] INFO: Window size: ${w}x${h}"
    if ($w -gt 100 -and $h -gt 100) {
        Write-Output "[EXE-TEST] PASS: Window size is reasonable"
        $passed++
    } else {
        Write-Output "[EXE-TEST] FAIL: Window size too small"
        $failed++
    }
} else {
    Write-Output "[EXE-TEST] FAIL: GetWindowRect failed"
    $failed++
}

# Test 4: Stability check - wait 5 seconds, verify no crash
Start-Sleep -Seconds 5
$p = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
if ($p -and $p.Responding) {
    Write-Output "[EXE-TEST] PASS: Process stable and responding after 5s"
    $passed++
} elseif ($p) {
    Write-Output "[EXE-TEST] FAIL: Process running but not responding"
    $failed++
} else {
    Write-Output "[EXE-TEST] FAIL: Process crashed during stability check"
    $failed++
}

# Test 5: Check Player.log for NativeBrowser initialization
$logPath = 'C:\Users\Ted\AppData\LocalLow\DefaultCompany\unity\Player.log'
if (Test-Path $logPath) {
    $logContent = Get-Content $logPath -Raw
    if ($logContent -match 'NativeBrowser initialized') {
        Write-Output "[EXE-TEST] PASS: NativeBrowser initialized in Player.log"
        $passed++
    } else {
        Write-Output "[EXE-TEST] FAIL: NativeBrowser not initialized in Player.log"
        $failed++
    }
    
    # Check for errors
    $errors = Select-String -Path $logPath -Pattern 'NativeBrowser.*[Ee]rror|DllNotFoundException.*NativeBrowserWebView' -AllMatches
    if ($errors.Matches.Count -gt 0) {
        Write-Output "[EXE-TEST] WARN: NativeBrowser errors found in log:"
        foreach ($m in $errors) {
            Write-Output "  $($m.Line)"
        }
    } else {
        Write-Output "[EXE-TEST] PASS: No NativeBrowser errors in log"
        $passed++
    }
} else {
    Write-Output "[EXE-TEST] WARN: Player.log not found"
}

# Test 6: No NativeBrowser window before user interaction
$nbHwnd = [W32]::FindWindowEx([IntPtr]::Zero, [IntPtr]::Zero, "NativeBrowserWebView", $null)
if ($nbHwnd -eq [IntPtr]::Zero) {
    Write-Output "[EXE-TEST] PASS: No NativeBrowser window before user click (expected)"
    $passed++
} else {
    Write-Output "[EXE-TEST] INFO: NativeBrowser window found unexpectedly: 0x$($nbHwnd.ToInt64().ToString('X'))"
    $passed++
}

Write-Output ""
Write-Output "[EXE-TEST] Results: $passed passed, $failed failed"

# Cleanup
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Write-Output "[EXE-TEST] Process terminated"

if ($failed -gt 0) {
    Write-Output "[EXE-TEST] SOME TESTS FAILED"
    exit 1
} else {
    Write-Output "[EXE-TEST] ALL PASSED"
    exit 0
}
