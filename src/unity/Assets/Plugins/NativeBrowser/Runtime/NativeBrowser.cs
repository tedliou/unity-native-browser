using System;
using UnityEngine;
using TedLiou.NativeBrowser.Internal;

namespace TedLiou.NativeBrowser
{
    public static class NativeBrowser
    {
        #if UNITY_ANDROID && !UNITY_EDITOR
        private static readonly IPlatformBridge bridge = new AndroidBridge();
#else
        private static readonly IPlatformBridge bridge = new EditorBridge();
#endif

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

            bridge.Initialize();
        }

        public static void Open(BrowserType type, BrowserConfig config)
        {
            if (config == null)
            {
                Debug.LogWarning("NativeBrowser: Open called with null config");
                return;
            }
            bridge.Open(type.ToString(), config.ToJson());
        }

        public static void Close()
        {
            bridge.Close();
        }

        public static void Refresh()
        {
            bridge.Refresh();
        }

        public static void ExecuteJavaScript(string script, string requestId = null)
        {
            bridge.ExecuteJavaScript(script, requestId);
        }

        public static void InjectJavaScript(string script)
        {
            bridge.InjectJavaScript(script);
        }

        public static bool IsOpen
        {
            get
            {
                return bridge.IsOpen();
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
