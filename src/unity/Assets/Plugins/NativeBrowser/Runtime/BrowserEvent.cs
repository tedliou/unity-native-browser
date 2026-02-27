using System;
using UnityEngine;

namespace TedLiou.NativeBrowser
{
    /// <summary>
    /// Event data when a WebView page starts loading.
    /// </summary>
    [Serializable]
    public class PageStartedEvent
    {
        /// <summary>
        /// The URL of the page being loaded.
        /// </summary>
        public string url;
    }

    /// <summary>
    /// Event data when a WebView page finishes loading.
    /// </summary>
    [Serializable]
    public class PageFinishedEvent
    {
        /// <summary>
        /// The URL of the page that finished loading.
        /// </summary>
        public string url;
    }

    /// <summary>
    /// Event data when a WebView encounters an error.
    /// </summary>
    [Serializable]
    public class BrowserErrorEvent
    {
        /// <summary>
        /// The error type (e.g., "LOAD_ERROR", "NETWORK_ERROR").
        /// </summary>
        public string type;

        /// <summary>
        /// The error message describing what went wrong.
        /// </summary>
        public string message;

        /// <summary>
        /// The URL where the error occurred.
        /// </summary>
        public string url;

        /// <summary>
        /// Request ID associated with the error (if applicable).
        /// </summary>
        public string requestId;
    }

    /// <summary>
    /// Event data when a WebView receives a PostMessage from JavaScript.
    /// </summary>
    [Serializable]
    public class PostMessageEvent
    {
        /// <summary>
        /// The message received from JavaScript.
        /// </summary>
        public string message;
    }

    /// <summary>
    /// Event data when JavaScript execution returns a result.
    /// </summary>
    [Serializable]
    public class JsResultEvent
    {
        /// <summary>
        /// Unique request ID that correlates with the ExecuteJavaScript call.
        /// </summary>
        public string requestId;

        /// <summary>
        /// The result value returned from JavaScript execution.
        /// </summary>
        public string result;
    }

    /// <summary>
    /// Event data when a deep link is triggered in the WebView.
    /// </summary>
    [Serializable]
    public class DeepLinkEvent
    {
        /// <summary>
        /// The deep link URL that was triggered.
        /// </summary>
        public string url;
    }
}
