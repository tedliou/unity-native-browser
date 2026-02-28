$ErrorActionPreference = 'Stop'

$exePath = 'E:\android-browser-for-unity\build\NativeBrowser.exe'
$logPath = 'C:\Users\Ted\AppData\LocalLow\DefaultCompany\unity\Player.log'

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Collections.Generic;

public class Win32 {
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
    
    [DllImport("user32.dll")]
    public static extern IntPtr GetParent(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    public static extern IntPtr GetAncestor(IntPtr hwnd, uint gaFlags);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool EnumChildWindows(IntPtr hWndParent, EnumWindowsProc lpEnumFunc, IntPtr lParam);
    
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    public static extern void SetCursorPos(int x, int y);
    
    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);
    
    [DllImport("user32.dll")]
    public static extern IntPtr SendMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool GetClientRect(IntPtr hWnd, out RECT lpRect);
    
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool ScreenToClient(IntPtr hWnd, ref POINT lpPoint);
    
    [StructLayout(LayoutKind.Sequential)]
    public struct POINT { public int X, Y; }
    
    public const uint WM_LBUTTONDOWN = 0x0201;
    public const uint WM_LBUTTONUP = 0x0202;
    public const int MK_LBUTTON = 0x0001;
    
    public static IntPtr MakeLParam(int low, int high) {
        return (IntPtr)((high << 16) | (low & 0xFFFF));
    }
    
    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    
    public const int GWL_STYLE = -16;
    public const int GWL_EXSTYLE = -20;
    public const int WS_CHILD = 0x40000000;
    public const int WS_VISIBLE = 0x10000000;
    
    public const uint GA_ROOT = 2;
    
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }
    
    // Search child windows for a specific class name
    public static IntPtr FindChildByClass(IntPtr parent, string className) {
        // First try FindWindowEx (direct child only)
        IntPtr child = FindWindowEx(parent, IntPtr.Zero, className, null);
        if (child != IntPtr.Zero) return child;
        
        // Then enumerate all descendants recursively
        IntPtr found = IntPtr.Zero;
        EnumChildWindows(parent, (hWnd, lParam) => {
            StringBuilder sb = new StringBuilder(256);
            GetClassName(hWnd, sb, 256);
            if (sb.ToString() == className) {
                found = hWnd;
                return false; // stop enumeration
            }
            return true; // continue
        }, IntPtr.Zero);
        return found;
    }
}
"@

# Clear previous Player.log
if (Test-Path $logPath) { Remove-Item $logPath -Force }

$passed = 0
$failed = 0

function Test-Pass([string]$msg) {
    $script:passed++
    Write-Output ('[WEBVIEW-TEST] PASS: ' + $msg)
}

function Test-Fail([string]$msg) {
    $script:failed++
    Write-Output ('[WEBVIEW-TEST] FAIL: ' + $msg)
}

function Test-Info([string]$msg) {
    Write-Output ('[WEBVIEW-TEST] INFO: ' + $msg)
}

function Find-NativeBrowserWindow([IntPtr]$parentHwnd) {
    # In EXE mode, the WebView is a CHILD window of Unity (not top-level)
    # Try both: child of Unity, and top-level (fallback for edge cases)
    
    # 1. Search as child of Unity window
    $child = [Win32]::FindChildByClass($parentHwnd, 'NativeBrowserWebView')
    if ($child -ne [IntPtr]::Zero) { return $child }
    
    # 2. Fallback: search as top-level window
    $toplevel = [Win32]::FindWindow('NativeBrowserWebView', $null)
    return $toplevel
}

# ── Phase 1: Launch and wait for Unity window ──
Write-Output '[WEBVIEW-TEST] Phase 1: Starting NativeBrowser.exe...'
$proc = Start-Process -FilePath $exePath -PassThru -WindowStyle Normal
Test-Info ('PID: ' + $proc.Id)

$timeout = 60
$elapsed = 0
$unityHwnd = [IntPtr]::Zero

while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    
    $p = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
    if (-not $p) {
        Test-Fail 'Process exited unexpectedly during startup'
        exit 1
    }
    
    if ($p.MainWindowHandle -ne [IntPtr]::Zero) {
        $unityHwnd = $p.MainWindowHandle
        $classNameSb = New-Object System.Text.StringBuilder 256
        [Win32]::GetClassName($unityHwnd, $classNameSb, 256) | Out-Null
        Test-Info ('Unity HWND: 0x' + $unityHwnd.ToInt64().ToString('X') + ', Class: ' + $classNameSb.ToString())
        break
    }
}

if ($unityHwnd -eq [IntPtr]::Zero) {
    Test-Fail 'Unity window not found within timeout'
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    exit 1
}
Test-Pass 'Unity window found'

# Wait for NativeBrowser.Initialize
Start-Sleep -Seconds 5

# ── Phase 2: Verify no WebView exists before user action ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 2: Pre-click verification...'

$nbHwnd = Find-NativeBrowserWindow $unityHwnd
if ($nbHwnd -eq [IntPtr]::Zero) {
    Test-Pass 'No NativeBrowser window before click (expected)'
} else {
    Test-Fail 'NativeBrowser window exists before any user action'
}

# ── Phase 3: Click the Open WebView button ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 3: Clicking Open WebView button...'

$rect = New-Object Win32+RECT
[Win32]::GetWindowRect($unityHwnd, [ref]$rect) | Out-Null
$winW = $rect.Right - $rect.Left
$winH = $rect.Bottom - $rect.Top
Test-Info ('Unity window: ' + $winW + 'x' + $winH + ' at (' + $rect.Left + ',' + $rect.Top + ')')

# Use SendMessage to click at specific client coordinates
# The CanvasScaler has reference 1080x1920 with matchWidthOrHeight=0.5
# Layout: padding(16) + Title(60) + spacing(12) + Status(40) + spacing(12) + Button(80px center at +40)
# In reference coords: first button center Y = 16+60+12+40+12+40 = 180
# With canvas offset 20px from edges, button center X = 540 (center)
#
# Get client rect to compute actual pixel positions
$clientRect = New-Object Win32+RECT
[Win32]::GetClientRect($unityHwnd, [ref]$clientRect) | Out-Null
$clientW = $clientRect.Right
$clientH = $clientRect.Bottom
Test-Info ('Client rect: ' + $clientW + 'x' + $clientH)

# Try a wider range of Y positions (10% to 35% of client height)
# Also use SendMessage for more reliable input delivery to Unity
$clickYRatios = @(0.17, 0.14, 0.20, 0.12, 0.23, 0.10, 0.25, 0.30, 0.35)
$webviewOpened = $false

foreach ($yRatio in $clickYRatios) {
    $cx = [int]($clientW / 2)
    $cy = [int]($clientH * $yRatio)
    Test-Info ('SendMessage click at client coords: (' + $cx + ',' + $cy + ')')
    
    # Bring window to foreground
    [Win32]::SetForegroundWindow($unityHwnd) | Out-Null
    Start-Sleep -Milliseconds 300
    
    # Send mouse click via SendMessage (client coordinates)
    $lParam = [Win32]::MakeLParam($cx, $cy)
    $wParam = [IntPtr]([Win32]::MK_LBUTTON)
    [Win32]::SendMessage($unityHwnd, [Win32]::WM_LBUTTONDOWN, $wParam, $lParam) | Out-Null
    Start-Sleep -Milliseconds 100
    [Win32]::SendMessage($unityHwnd, [Win32]::WM_LBUTTONUP, [IntPtr]::Zero, $lParam) | Out-Null
    
    # Also try mouse_event as fallback (screen coordinates)
    $screenX = $rect.Left + $cx
    $screenY = $rect.Top + $cy
    [Win32]::SetCursorPos($screenX, $screenY)
    Start-Sleep -Milliseconds 100
    [Win32]::mouse_event([Win32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 100
    [Win32]::mouse_event([Win32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    
    # Wait for WebView to open
    Start-Sleep -Seconds 3
    
    # Check Player.log to see if click worked
    if (Test-Path $logPath) {
        $logContent = Get-Content $logPath -Raw
        if ($logContent -match 'Opening WebView') {
            Test-Info 'Player.log confirms OpenWebView was called'
            $webviewOpened = $true
            break
        }
    }
}

if (-not $webviewOpened) {
    Test-Info 'Click verification via Player.log was inconclusive (will verify via window detection in Phase 4)'
}

# ── Phase 4: Wait for WebView window to appear ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 4: Searching for WebView window...'

# Give WebView2 time to initialize (environment creation can take a few seconds)
Start-Sleep -Seconds 5

$webviewHwnd = [IntPtr]::Zero
$searchTimeout = 30
$searchElapsed = 0

while ($searchElapsed -lt $searchTimeout) {
    $p = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
    if (-not $p) {
        Test-Fail 'Process crashed while waiting for WebView!'
        exit 1
    }
    
    # Search as child of Unity window AND as top-level
    $webviewHwnd = Find-NativeBrowserWindow $unityHwnd
    if ($webviewHwnd -ne [IntPtr]::Zero) {
        Test-Info ('NativeBrowser window found after ' + $searchElapsed + ' seconds')
        Test-Info ('WebView HWND: 0x' + $webviewHwnd.ToInt64().ToString('X'))
        break
    }
    
    Start-Sleep -Seconds 2
    $searchElapsed += 2
}

if ($webviewHwnd -ne [IntPtr]::Zero) {
    Test-Pass 'NativeBrowser WebView window found'
} else {
    Test-Fail 'NativeBrowser WebView window NOT found (searched both child and top-level)'
    
    # Dump relevant log lines for debugging
    if (Test-Path $logPath) {
        $nbLines = Select-String -Path $logPath -Pattern 'NativeBrowser|WebView|DllNotFoundException|Error' -AllMatches
        if ($nbLines) {
            Test-Info 'Relevant Player.log lines:'
            foreach ($line in $nbLines) {
                Test-Info ('  ' + $line.Line.Trim())
            }
        }
    }
}

# ── Phase 5: Verify WebView properties ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 5: WebView window properties...'

if ($webviewHwnd -ne [IntPtr]::Zero) {
    # Test: valid window
    if ([Win32]::IsWindow($webviewHwnd)) {
        Test-Pass 'WebView window is valid'
    } else {
        Test-Fail 'WebView window is invalid'
    }
    
    # Test: visible
    if ([Win32]::IsWindowVisible($webviewHwnd)) {
        Test-Pass 'WebView window is visible'
    } else {
        Test-Fail 'WebView window is NOT visible'
    }
    
    # Test: WS_CHILD style (embedded in Unity)
    $style = [Win32]::GetWindowLong($webviewHwnd, [Win32]::GWL_STYLE)
    $styleHex = '0x{0:X8}' -f $style
    Test-Info ('WebView style: ' + $styleHex)
    
    $isChild = ($style -band [Win32]::WS_CHILD) -ne 0
    if ($isChild) {
        Test-Pass 'WebView has WS_CHILD style (embedded mode correct)'
    } else {
        Test-Fail 'WebView does NOT have WS_CHILD style - not properly embedded'
    }
    
    # Test: parent is Unity HWND (or a descendant of it)
    $parentHwnd = [Win32]::GetParent($webviewHwnd)
    Test-Info ('WebView parent HWND: 0x' + $parentHwnd.ToInt64().ToString('X'))
    
    $rootHwnd = [Win32]::GetAncestor($webviewHwnd, [Win32]::GA_ROOT)
    Test-Info ('WebView root ancestor: 0x' + $rootHwnd.ToInt64().ToString('X'))
    
    if ($parentHwnd -eq $unityHwnd) {
        Test-Pass 'WebView is direct child of Unity window'
    } elseif ($rootHwnd -eq $unityHwnd) {
        Test-Pass 'WebView is descendant of Unity window'
    } else {
        Test-Fail 'WebView is NOT a child/descendant of Unity window'
    }
    
    # Test: reasonable size
    $wvRect = New-Object Win32+RECT
    if ([Win32]::GetWindowRect($webviewHwnd, [ref]$wvRect)) {
        $wvW = $wvRect.Right - $wvRect.Left
        $wvH = $wvRect.Bottom - $wvRect.Top
        Test-Info ('WebView size: ' + $wvW + 'x' + $wvH)
        
        if ($wvW -gt 50 -and $wvH -gt 50) {
            Test-Pass ('WebView size is reasonable: ' + $wvW + 'x' + $wvH)
        } else {
            Test-Fail ('WebView size too small: ' + $wvW + 'x' + $wvH)
        }
    }
}

# ── Phase 6: Stability check ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 6: Stability check...'

Start-Sleep -Seconds 5
$p = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
if ($p -and $p.Responding) {
    Test-Pass 'Process stable and responding with WebView open'
} elseif ($p) {
    Test-Fail 'Process running but not responding'
} else {
    Test-Fail 'Process CRASHED with WebView open!'
}

# ── Phase 7: Player.log analysis ──
Write-Output ''
Write-Output '[WEBVIEW-TEST] Phase 7: Player.log analysis...'

if (Test-Path $logPath) {
    $logContent = Get-Content $logPath -Raw
    
    if ($logContent -match 'NativeBrowser initialized') {
        Test-Pass 'NativeBrowser initialized'
    } else {
        Test-Fail 'NativeBrowser not initialized'
    }
    
    if ($logContent -match 'Page started|Page finished|OnPageStarted|OnPageFinished') {
        Test-Pass 'Page load events detected'
    } else {
        Test-Info 'No page load events (WebView may not have opened)'
    }
    
    if ($logContent -match 'DllNotFoundException.*NativeBrowserWebView') {
        Test-Fail 'DllNotFoundException for NativeBrowserWebView!'
    } else {
        Test-Pass 'No DllNotFoundException'
    }
} else {
    Test-Info 'Player.log not found'
}

# ── Cleanup ──
Write-Output ''
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Write-Output '[WEBVIEW-TEST] Process terminated'
Write-Output ''

$resultLine = '[WEBVIEW-TEST] Results: ' + $passed + ' passed, ' + $failed + ' failed'
Write-Output '======================================='
Write-Output $resultLine
Write-Output '======================================='

if ($failed -gt 0) {
    Write-Output '[WEBVIEW-TEST] SOME TESTS FAILED'
    exit 1
} else {
    Write-Output '[WEBVIEW-TEST] ALL PASSED'
    exit 0
}
