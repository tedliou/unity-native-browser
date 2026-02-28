using System;
using System.Runtime.InteropServices;
using System.Threading;
using UnityEditor;
using UnityEngine;
using NativeBrowserApi = TedLiou.NativeBrowser.NativeBrowser;

namespace TedLiou.Build
{
    /// <summary>
    /// Headless integration test for Windows WebView2.
    /// Invoke via: Unity -batchmode -executeMethod TedLiou.Build.WindowsIntegrationTest.RunEditorTest
    /// Tests the full lifecycle: Initialize → Open → verify window → Close → verify cleanup.
    /// </summary>
    public static class WindowsIntegrationTest
    {
        // Win32 API for verifying the WebView window was created
        [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern IntPtr FindWindowEx(IntPtr hwndParent, IntPtr hwndChildAfter, string lpClassName, string lpWindowName);

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool IsWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern int GetWindowLong(IntPtr hWnd, int nIndex);

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

        [StructLayout(LayoutKind.Sequential)]
        private struct RECT
        {
            public int Left, Top, Right, Bottom;
        }

        private const int GWL_STYLE = -16;
        private const int GWL_EXSTYLE = -20;
        private const int WS_VISIBLE = 0x10000000;
        private const int WS_THICKFRAME = 0x00040000; // Resizable
        private const int WS_EX_TOPMOST = 0x00000008; // Always on top
        private const int WS_MAXIMIZEBOX = 0x00010000;
        private const int WS_MINIMIZEBOX = 0x00020000;

        private static int testsPassed = 0;
        private static int testsFailed = 0;

        /// <summary>
        /// Main entry point for the Editor integration test.
        /// </summary>
        public static void RunEditorTest()
        {
            Debug.Log("[WindowsIntegrationTest] Starting Windows WebView2 Editor integration test...");

            try
            {
                TestInitialize();
                TestOpenWebView();
                TestWindowProperties();
                TestIsOpen();
                TestCloseWebView();
                TestVerifyCleanup();
            }
            catch (Exception ex)
            {
                Fail("UNHANDLED_EXCEPTION", ex.Message);
            }

            Debug.Log($"[WindowsIntegrationTest] Results: {testsPassed} passed, {testsFailed} failed");

            if (testsFailed > 0)
            {
                Debug.LogError($"[WindowsIntegrationTest] FAILED — {testsFailed} test(s) failed");
                EditorApplication.Exit(1);
            }
            else
            {
                Debug.Log($"[WindowsIntegrationTest] ALL PASSED — {testsPassed} test(s)");
                EditorApplication.Exit(0);
            }
        }

        private static void TestInitialize()
        {
            Debug.Log("[WindowsIntegrationTest] Test: Initialize...");
            try
            {
                NativeBrowserApi.Initialize();
                Pass("Initialize");
            }
            catch (Exception ex)
            {
                Fail("Initialize", ex.Message);
            }
        }

        private static void TestOpenWebView()
        {
            Debug.Log("[WindowsIntegrationTest] Test: Open WebView...");
            try
            {
                var config = new TedLiou.NativeBrowser.BrowserConfig("https://example.com")
                {
                    width = 0.9f,
                    height = 0.8f,
                    alignment = TedLiou.NativeBrowser.Alignment.CENTER,
                    enableJavaScript = true
                };
                NativeBrowserApi.Open(TedLiou.NativeBrowser.BrowserType.WebView, config);

                // Give the Rust STA thread time to create the window and WebView2
                Thread.Sleep(5000);

                // Drain callbacks to surface any errors from the Rust layer
                int drained = TedLiou.NativeBrowser.Internal.WindowsCallbackDispatcher.DrainCallbacks();
                Debug.Log($"[WindowsIntegrationTest] Drained {drained} callback(s) after Open");

                Pass("Open_WebView");
            }
            catch (Exception ex)
            {
                Fail("Open_WebView", ex.Message);
            }
        }

        private static void TestWindowProperties()
        {
            Debug.Log("[WindowsIntegrationTest] Test: Window properties...");
            try
            {
                // Find the NativeBrowser window by its window title
                IntPtr hwnd = FindWindowEx(IntPtr.Zero, IntPtr.Zero, null, "NativeBrowser");

                if (hwnd == IntPtr.Zero)
                {
                    // Fallback: try finding by class name instead of title
                    hwnd = FindWindowEx(IntPtr.Zero, IntPtr.Zero, "NativeBrowserWebView", null);
                    Debug.Log($"[WindowsIntegrationTest] FindWindow by class: 0x{hwnd.ToInt64():X}");
                }

                if (hwnd == IntPtr.Zero)
                {
                    // Also check IsOpen for diagnostics
                    bool isOpen = NativeBrowserApi.IsOpen;
                    Debug.LogError($"[WindowsIntegrationTest] Window not found. IsOpen={isOpen}");
                    // Drain any remaining callbacks for error messages
                    TedLiou.NativeBrowser.Internal.WindowsCallbackDispatcher.DrainCallbacks();
                    Fail("Window_Exists", "Could not find NativeBrowser window by title or class name");
                    return;
                }

                // Test 1: Window exists and is valid
                if (IsWindow(hwnd))
                {
                    Pass("Window_Exists");
                }
                else
                {
                    Fail("Window_Exists", "HWND is invalid");
                    return;
                }

                // Test 2: Window is visible
                if (IsWindowVisible(hwnd))
                {
                    Pass("Window_Visible");
                }
                else
                {
                    Fail("Window_Visible", "Window is not visible");
                }

                // Test 3: Window style - resizable (WS_THICKFRAME)
                int style = GetWindowLong(hwnd, GWL_STYLE);
                if ((style & WS_THICKFRAME) != 0)
                {
                    Pass("Window_Resizable");
                }
                else
                {
                    Fail("Window_Resizable", $"Missing WS_THICKFRAME. Style: 0x{style:X8}");
                }

                // Test 4: Window style - no maximize button
                if ((style & WS_MAXIMIZEBOX) == 0)
                {
                    Pass("Window_NoMaximize");
                }
                else
                {
                    Fail("Window_NoMaximize", $"WS_MAXIMIZEBOX present. Style: 0x{style:X8}");
                }

                // Test 5: Window style - no minimize button
                if ((style & WS_MINIMIZEBOX) == 0)
                {
                    Pass("Window_NoMinimize");
                }
                else
                {
                    Fail("Window_NoMinimize", $"WS_MINIMIZEBOX present. Style: 0x{style:X8}");
                }

                // Test 6: Extended style - topmost (WS_EX_TOPMOST)
                int exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
                if ((exStyle & WS_EX_TOPMOST) != 0)
                {
                    Pass("Window_Topmost");
                }
                else
                {
                    Fail("Window_Topmost", $"Missing WS_EX_TOPMOST. ExStyle: 0x{exStyle:X8}");
                }

                // Test 7: Window size is reasonable (around 1024x768 scaled for DPI)
                if (GetWindowRect(hwnd, out RECT rect))
                {
                    int w = rect.Right - rect.Left;
                    int h = rect.Bottom - rect.Top;
                    Debug.Log($"[WindowsIntegrationTest] Window size: {w}x{h}");

                    // Allow wide range for DPI scaling: 512-3072 width, 384-2304 height
                    if (w >= 512 && w <= 3072 && h >= 384 && h <= 2304)
                    {
                        Pass("Window_Size");
                    }
                    else
                    {
                        Fail("Window_Size", $"Unexpected size: {w}x{h}");
                    }
                }
                else
                {
                    Fail("Window_Size", "GetWindowRect failed");
                }
            }
            catch (Exception ex)
            {
                Fail("Window_Properties", ex.Message);
            }
        }

        private static void TestIsOpen()
        {
            Debug.Log("[WindowsIntegrationTest] Test: IsOpen...");
            try
            {
                if (NativeBrowserApi.IsOpen)
                {
                    Pass("IsOpen_True");
                }
                else
                {
                    Fail("IsOpen_True", "IsOpen returned false while browser should be open");
                }
            }
            catch (Exception ex)
            {
                Fail("IsOpen_True", ex.Message);
            }
        }

        private static void TestCloseWebView()
        {
            Debug.Log("[WindowsIntegrationTest] Test: Close WebView...");
            try
            {
                NativeBrowserApi.Close();
                Thread.Sleep(1000);
                Pass("Close_WebView");
            }
            catch (Exception ex)
            {
                Fail("Close_WebView", ex.Message);
            }
        }

        private static void TestVerifyCleanup()
        {
            Debug.Log("[WindowsIntegrationTest] Test: Verify cleanup...");
            try
            {
                // IsOpen should return false after close
                if (!NativeBrowserApi.IsOpen)
                {
                    Pass("IsOpen_False_After_Close");
                }
                else
                {
                    Fail("IsOpen_False_After_Close", "IsOpen still returns true after Close()");
                }

                // Window should be gone
                IntPtr hwnd = FindWindowEx(IntPtr.Zero, IntPtr.Zero, null, "NativeBrowser");
                if (hwnd == IntPtr.Zero)
                {
                    Pass("Window_Destroyed");
                }
                else
                {
                    Fail("Window_Destroyed", "NativeBrowser window still exists after Close()");
                }
            }
            catch (Exception ex)
            {
                Fail("Verify_Cleanup", ex.Message);
            }
        }

        private static void Pass(string testName)
        {
            testsPassed++;
            Debug.Log($"[WindowsIntegrationTest] PASS: {testName}");
        }

        private static void Fail(string testName, string reason)
        {
            testsFailed++;
            Debug.LogError($"[WindowsIntegrationTest] FAIL: {testName} — {reason}");
        }
    }
}
