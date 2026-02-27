using System;
using UnityEngine;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Android-specific platform bridge implementation encapsulating all JNI calls.
    /// This class handles all AndroidJavaClass interactions for browser operations on Android.
    /// </summary>
    internal class AndroidBridge : IPlatformBridge
    {
        private const string BridgeClassName = "com.tedliou.android.browser.BrowserManager";
        private const string UnityPlayerClassName = "com.unity3d.player.UnityPlayer";

        /// <summary>
        /// Initialize the Android bridge and prepare for browser operations.
        /// </summary>
        public void Initialize()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (AndroidJavaClass unityPlayer = new AndroidJavaClass(UnityPlayerClassName))
                using (AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("initialize", activity);
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: Initialize failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Open a browser with the specified type and configuration.
        /// </summary>
        public void Open(string type, string configJson)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (string.IsNullOrEmpty(configJson))
            {
                Debug.LogWarning("NativeBrowser: Open called with null or empty config");
                return;
            }
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("open", ToAndroidType(type), configJson);
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: Open failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Close the currently open browser.
        /// </summary>
        public void Close()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("close");
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: Close failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Refresh the current page in the browser.
        /// </summary>
        public void Refresh()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("refresh");
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: Refresh failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Execute JavaScript in the browser and return the result via callback.
        /// </summary>
        public void ExecuteJavaScript(string script, string requestId)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: ExecuteJavaScript called with empty script");
                return;
            }
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("executeJavaScript", script, requestId);
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: ExecuteJavaScript failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Inject JavaScript into the browser to be executed immediately.
        /// </summary>
        public void InjectJavaScript(string script)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (string.IsNullOrEmpty(script))
            {
                Debug.LogWarning("NativeBrowser: InjectJavaScript called with empty script");
                return;
            }
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("injectJavaScript", script);
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: InjectJavaScript failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Send a message to the web content via JavaScript postMessage.
        /// </summary>
        public void SendPostMessage(string message)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (string.IsNullOrEmpty(message))
            {
                Debug.LogWarning("NativeBrowser: SendPostMessage called with null or empty message");
                return;
            }
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("sendPostMessage", message);
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: SendPostMessage failed: {exception.Message}");
            }
#endif
        }

        /// <summary>
        /// Check if a browser is currently open.
        /// </summary>
        public bool IsOpen()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    return bridgeClass.CallStatic<bool>("isOpen");
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: IsOpen failed: {exception.Message}");
                return false;
            }
#else
            return false;
#endif
        }

        /// <summary>
        /// Convert BrowserType enum name to Android-specific type string.
        /// </summary>
        private static string ToAndroidType(string type)
        {
            switch (type)
            {
                case "WebView":
                    return "WEBVIEW";
                case "CustomTab":
                    return "CUSTOM_TAB";
                case "SystemBrowser":
                    return "SYSTEM_BROWSER";
                default:
                    return "WEBVIEW";
            }
        }
    }
}
