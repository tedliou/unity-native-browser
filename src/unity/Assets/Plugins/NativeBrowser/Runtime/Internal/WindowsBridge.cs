using System;
using System.Runtime.InteropServices;
using UnityEngine;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Windows-specific platform bridge implementation using P/Invoke to the Rust WebView2 DLL.
    /// Handles all native function calls for browser operations on Windows (Editor and Standalone).
    /// </summary>
    internal class WindowsBridge : IPlatformBridge
    {
        private const string DllName = "NativeBrowserWebView";

        // ─── P/Invoke Declarations ─────────────────────────────────────────────

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_initialize")]
        private static extern void NbInitialize(
            WindowsCallbackDispatcher.NativeCallbackDelegate callback,
            [MarshalAs(UnmanagedType.U1)] bool isEditor
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_open")]
        private static extern void NbOpen(
            [MarshalAs(UnmanagedType.LPStr)] string typeStr,
            [MarshalAs(UnmanagedType.LPStr)] string configJson,
            IntPtr parentHwnd
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_close")]
        private static extern void NbClose();

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_refresh")]
        private static extern void NbRefresh();

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_is_open")]
        [return: MarshalAs(UnmanagedType.U1)]
        private static extern bool NbIsOpen();

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_execute_js")]
        private static extern void NbExecuteJs(
            [MarshalAs(UnmanagedType.LPStr)] string script,
            [MarshalAs(UnmanagedType.LPStr)] string requestId
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_inject_js")]
        private static extern void NbInjectJs(
            [MarshalAs(UnmanagedType.LPStr)] string script
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_send_post_message")]
        private static extern void NbSendPostMessage(
            [MarshalAs(UnmanagedType.LPStr)] string message
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, EntryPoint = "nb_destroy")]
        private static extern void NbDestroy();

        // ─── State ─────────────────────────────────────────────────────────────

        /// <summary>
        /// Prevent the callback delegate from being garbage collected while the DLL holds a pointer to it.
        /// </summary>
        private static WindowsCallbackDispatcher.NativeCallbackDelegate callbackDelegate;

        private static bool initialized;

        // ─── IPlatformBridge Implementation ─────────────────────────────────────

        /// <summary>
        /// Initialize the Windows native browser bridge.
        /// Spawns the WebView2 STA thread and registers the callback for event delivery.
        /// </summary>
        public void Initialize()
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (initialized)
            {
                return;
            }

            try
            {
                // Ensure the callback dispatcher exists on the main thread
                var dispatcher = WindowsCallbackDispatcher.Instance;
                dispatcher.gameObject.SetActive(true);

                // Pin the delegate to prevent GC
                callbackDelegate = WindowsCallbackDispatcher.OnNativeCallback;

                bool isEditor = Application.isEditor;
                NbInitialize(callbackDelegate, isEditor);

                initialized = true;
            }
            catch (DllNotFoundException ex)
            {
                Debug.LogWarning($"NativeBrowser: Windows DLL not found: {ex.Message}");
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: Initialize failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Open a browser with the specified type and configuration.
        /// In Editor mode, opens a standalone topmost window.
        /// In Build mode, embeds the WebView as a child window of Unity's game window.
        /// </summary>
        public void Open(string type, string configJson)
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized)
            {
                Debug.LogWarning("NativeBrowser: Not initialized — call Initialize() first");
                return;
            }

            if (string.IsNullOrEmpty(configJson))
            {
                Debug.LogWarning("NativeBrowser: Open called with null or empty config");
                return;
            }

            try
            {
                // In build mode, pass Unity's main window HWND for embedding.
                // In editor mode, pass IntPtr.Zero — the DLL creates a standalone window.
                IntPtr parentHwnd = IntPtr.Zero;
                if (!Application.isEditor)
                {
                    parentHwnd = GetUnityWindowHandle();
                }

                NbOpen(type, configJson, parentHwnd);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: Open failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Close the currently open browser.
        /// </summary>
        public void Close()
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return;

            try
            {
                NbClose();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: Close failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Refresh the current page in the browser.
        /// </summary>
        public void Refresh()
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return;

            try
            {
                NbRefresh();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: Refresh failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Check if a browser is currently open.
        /// </summary>
        public bool IsOpen()
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return false;

            try
            {
                return NbIsOpen();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: IsOpen failed: {ex.Message}");
                return false;
            }
#else
            return false;
#endif
        }

        /// <summary>
        /// Execute JavaScript in the browser and return the result via callback.
        /// </summary>
        public void ExecuteJavaScript(string script, string requestId)
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return;

            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: ExecuteJavaScript called with empty script");
                return;
            }

            try
            {
                NbExecuteJs(script, requestId ?? "");
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: ExecuteJavaScript failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Inject JavaScript into the browser to be executed on every page load.
        /// </summary>
        public void InjectJavaScript(string script)
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return;

            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: InjectJavaScript called with empty script");
                return;
            }

            try
            {
                NbInjectJs(script);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: InjectJavaScript failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Send a message to the web content via JavaScript postMessage.
        /// </summary>
        public void SendPostMessage(string message)
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            if (!initialized) return;

            if (string.IsNullOrEmpty(message))
            {
                Debug.LogWarning("NativeBrowser: SendPostMessage called with null or empty message");
                return;
            }

            try
            {
                NbSendPostMessage(message);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: SendPostMessage failed: {ex.Message}");
            }
#endif
        }

        // ─── Helpers ───────────────────────────────────────────────────────────

        /// <summary>
        /// Get Unity's main game window HWND for embedding the WebView as a child window.
        /// Used only in standalone build mode, NOT in editor mode.
        /// </summary>
        private static IntPtr GetUnityWindowHandle()
        {
            try
            {
                // In standalone build, find the Unity window by class name
                IntPtr hwnd = FindWindow("UnityWndClass", null);
                if (hwnd == IntPtr.Zero)
                {
                    Debug.LogWarning("NativeBrowser: Could not find Unity window (UnityWndClass)");
                }
                return hwnd;
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: GetUnityWindowHandle failed: {ex.Message}");
                return IntPtr.Zero;
            }
        }

        [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern IntPtr FindWindow(string lpClassName, string lpWindowName);

        /// <summary>
        /// Cleanup native resources when the application quits.
        /// Called via Application.quitting or domain unload.
        /// </summary>
        internal static void Shutdown()
        {
            if (!initialized) return;

            try
            {
                NbDestroy();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: Shutdown failed: {ex.Message}");
            }
            finally
            {
                initialized = false;
                callbackDelegate = null;
            }
        }

        // Register shutdown hook via static constructor
        static WindowsBridge()
        {
            Application.quitting += Shutdown;
        }
    }
}
