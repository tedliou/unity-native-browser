using System;
using UnityEngine;

namespace TedLiou.NativeBrowser
{
    public static class NativeBrowser
    {
        private const string BridgeClassName = "com.tedliou.android.browser.BrowserManager";
        private const string UnityPlayerClassName = "com.unity3d.player.UnityPlayer";

        public static void Initialize()
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
    }
}
