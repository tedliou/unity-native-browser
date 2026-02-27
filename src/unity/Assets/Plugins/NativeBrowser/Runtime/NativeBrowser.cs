using System;
using UnityEngine;

namespace TedLiou.NativeBrowser
{
    public static class NativeBrowser
    {
        private const string BridgeClassName = "com.tedliou.android.browser.BrowserManager";
        private const string UnityPlayerClassName = "com.unity3d.player.UnityPlayer";

        // Events fired when Android native callbacks are received
        // Subscribe to these events to handle browser lifecycle and interactions

        /// <summary>
        /// Fired when a page starts loading in the WebView.
        /// </summary>
        public static event Action<string> OnPageStarted;

        /// <summary>
        /// Fired when a page finishes loading in the WebView.
        /// </summary>
        public static event Action<string> OnPageFinished;

        /// <summary>
        /// Fired when an error occurs in the WebView.
        /// Parameters: (errorMessage, url)
        /// </summary>
        public static event Action<string, string> OnError;

        /// <summary>
        /// Fired when a PostMessage is received from JavaScript in the WebView.
        /// </summary>
        public static event Action<string> OnPostMessage;

        /// <summary>
        /// Fired when JavaScript execution returns a result.
        /// Parameters: (requestId, result)
        /// </summary>
        public static event Action<string, string> OnJsResult;

        /// <summary>
        /// Fired when a deep link is triggered in the WebView.
        /// </summary>
        public static event Action<string> OnDeepLink;

        /// <summary>
        /// Fired when the browser is closed.
        /// </summary>
        public static event Action OnClosed;

        public static void Initialize()
        {
            // Initialize the callback receiver GameObject
            // This must be done before any Android calls to ensure callbacks are received
            var receiver = NativeBrowserCallbackReceiver.Instance;
            receiver.gameObject.SetActive(true);

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
#else
            LogEditorOnly("Initialize");
#endif
        }

        public static void Open(BrowserType type, BrowserConfig config)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (config == null)
            {
                Debug.LogWarning("NativeBrowser: Open called with null config");
                return;
            }
            try
            {
                using (AndroidJavaClass bridgeClass = new AndroidJavaClass(BridgeClassName))
                {
                    bridgeClass.CallStatic("open", ToAndroidType(type), config.ToJson());
                }
            }
            catch (Exception exception)
            {
                Debug.LogWarning($"NativeBrowser: Open failed: {exception.Message}");
            }
#else
            LogEditorOnly("Open");
#endif
        }

        public static void Close()
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
#else
            LogEditorOnly("Close");
#endif
        }

        public static void Refresh()
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
#else
            LogEditorOnly("Refresh");
#endif
        }

        public static void ExecuteJavaScript(string script, string requestId = null)
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
#else
            LogEditorOnly("ExecuteJavaScript");
#endif
        }

        public static void InjectJavaScript(string script)
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
#else
            LogEditorOnly("InjectJavaScript");
#endif
        }

        public static bool IsOpen
        {
            get
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
                LogEditorOnly("IsOpen");
                return false;
#endif
            }
        }

        private static string ToAndroidType(BrowserType type)
        {
            switch (type)
            {
                case BrowserType.WebView:
                    return "WEBVIEW";
                case BrowserType.CustomTab:
                    return "CUSTOM_TAB";
                case BrowserType.SystemBrowser:
                    return "SYSTEM_BROWSER";
                default:
                    return "WEBVIEW";
            }
        }

        internal static string GetAlignmentString(Alignment alignment)
        {
            switch (alignment)
            {
                case Alignment.CENTER:
                    return "CENTER";
                case Alignment.LEFT:
                    return "LEFT";
                case Alignment.RIGHT:
                    return "RIGHT";
                case Alignment.TOP:
                    return "TOP";
                case Alignment.BOTTOM:
                    return "BOTTOM";
                case Alignment.TOP_LEFT:
                    return "TOP_LEFT";
                case Alignment.TOP_RIGHT:
                    return "TOP_RIGHT";
                case Alignment.BOTTOM_LEFT:
                    return "BOTTOM_LEFT";
                case Alignment.BOTTOM_RIGHT:
                    return "BOTTOM_RIGHT";
                default:
                    return "CENTER";
            }
        }

        private static void LogEditorOnly(string methodName)
        {
            Debug.LogWarning($"NativeBrowser: {methodName} is Android-only and was called in editor or non-Android platform");
        }

        // Internal raise helpers — allow NativeBrowserCallbackReceiver (same assembly) to fire events
        internal static void RaiseOnPageStarted(string url)   => OnPageStarted?.Invoke(url);
        internal static void RaiseOnPageFinished(string url)  => OnPageFinished?.Invoke(url);
        internal static void RaiseOnError(string msg, string url) => OnError?.Invoke(msg, url);
        internal static void RaiseOnPostMessage(string msg)   => OnPostMessage?.Invoke(msg);
        internal static void RaiseOnJsResult(string rid, string res) => OnJsResult?.Invoke(rid, res);
        internal static void RaiseOnDeepLink(string url)      => OnDeepLink?.Invoke(url);
        internal static void RaiseOnClosed()                  => OnClosed?.Invoke();
    }
}
