using System;
using System.Runtime.InteropServices;
using UnityEngine;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// WebGL-specific platform bridge implementation using .jslib interop.
    /// Manages an iframe overlay for WebView mode and window.open for CustomTab/SystemBrowser.
    /// </summary>
    internal class WebGLBridge : IPlatformBridge
    {
#if UNITY_WEBGL && !UNITY_EDITOR
        // ─── P/Invoke Declarations (.jslib) ──────────────────────────────────────

        [DllImport("__Internal")]
        private static extern void NB_WebGL_Initialize(string gameObjectName);

        [DllImport("__Internal")]
        private static extern void NB_WebGL_Open(string type, string configJson);

        [DllImport("__Internal")]
        private static extern void NB_WebGL_Close();

        [DllImport("__Internal")]
        private static extern void NB_WebGL_Refresh();

        [DllImport("__Internal")]
        private static extern bool NB_WebGL_IsOpen();

        [DllImport("__Internal")]
        private static extern void NB_WebGL_ExecuteJavaScript(string script, string requestId);

        [DllImport("__Internal")]
        private static extern void NB_WebGL_InjectJavaScript(string script);

        [DllImport("__Internal")]
        private static extern void NB_WebGL_SendPostMessage(string message);
#endif

        // ─── IPlatformBridge Implementation ──────────────────────────────────────

        /// <summary>
        /// Initialize the WebGL bridge. Passes the callback GameObject name to JavaScript
        /// so that SendMessage calls route to the correct receiver.
        /// </summary>
        public void Initialize()
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            try
            {
                NB_WebGL_Initialize("NativeBrowserCallback");
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL Initialize failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Open a browser with the specified type and configuration.
        /// WebView → iframe overlay on the Unity canvas.
        /// CustomTab / SystemBrowser → window.open() in a new tab.
        /// </summary>
        public void Open(string type, string configJson)
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            if (string.IsNullOrEmpty(configJson))
            {
                Debug.LogWarning("NativeBrowser: Open called with null or empty config");
                return;
            }

            try
            {
                NB_WebGL_Open(type, configJson);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL Open failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Close the currently open browser overlay or tab.
        /// </summary>
        public void Close()
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            try
            {
                NB_WebGL_Close();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL Close failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Refresh the current iframe page. Only effective for WebView type.
        /// </summary>
        public void Refresh()
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            try
            {
                NB_WebGL_Refresh();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL Refresh failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Check if a browser overlay is currently open.
        /// </summary>
        public bool IsOpen()
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            try
            {
                return NB_WebGL_IsOpen();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL IsOpen failed: {ex.Message}");
                return false;
            }
#else
            return false;
#endif
        }

        /// <summary>
        /// Execute JavaScript inside the iframe. Cross-origin iframes will log a warning.
        /// </summary>
        public void ExecuteJavaScript(string script, string requestId)
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: ExecuteJavaScript called with empty script");
                return;
            }

            try
            {
                NB_WebGL_ExecuteJavaScript(script, requestId ?? "");
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL ExecuteJavaScript failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Inject JavaScript into the iframe. Cross-origin iframes will log a warning.
        /// </summary>
        public void InjectJavaScript(string script)
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: InjectJavaScript called with empty script");
                return;
            }

            try
            {
                NB_WebGL_InjectJavaScript(script);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL InjectJavaScript failed: {ex.Message}");
            }
#endif
        }

        /// <summary>
        /// Send a postMessage to the iframe content. Works cross-origin.
        /// </summary>
        public void SendPostMessage(string message)
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            if (string.IsNullOrEmpty(message))
            {
                Debug.LogWarning("NativeBrowser: SendPostMessage called with null or empty message");
                return;
            }

            try
            {
                NB_WebGL_SendPostMessage(message);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowser: WebGL SendPostMessage failed: {ex.Message}");
            }
#endif
        }
    }
}
