using System;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Platform-specific bridge interface that defines the contract for browser operations.
    /// Implementations (Android, Editor) provide platform-specific implementations of these methods.
    /// </summary>
    internal interface IPlatformBridge
    {
        /// <summary>
        /// Initialize the platform bridge and prepare for browser operations.
        /// This must be called once before any other bridge operations.
        /// </summary>
        void Initialize();

        /// <summary>
        /// Open a browser with the specified type and configuration.
        /// </summary>
        /// <param name="type">The browser type as a string ("WEBVIEW", "CUSTOM_TAB", "SYSTEM_BROWSER").</param>
        /// <param name="configJson">The browser configuration serialized as JSON.</param>
        void Open(string type, string configJson);

        /// <summary>
        /// Close the currently open browser.
        /// This is a no-op if no browser is currently open.
        /// </summary>
        void Close();

        /// <summary>
        /// Refresh the current page in the browser.
        /// This only affects WebView browsers.
        /// </summary>
        void Refresh();

        /// <summary>
        /// Check if a browser is currently open.
        /// </summary>
        /// <returns>True if a browser is open, false otherwise.</returns>
        bool IsOpen();

        /// <summary>
        /// Execute JavaScript in the browser and return the result via callback.
        /// </summary>
        /// <param name="script">The JavaScript code to execute.</param>
        /// <param name="requestId">A unique identifier for this request, used to match results from callbacks. Can be null.</param>
        void ExecuteJavaScript(string script, string requestId);

        /// <summary>
        /// Inject JavaScript into the browser to be executed immediately.
        /// Results are not returned; use ExecuteJavaScript if a result is needed.
        /// </summary>
        /// <param name="script">The JavaScript code to inject and execute.</param>
        void InjectJavaScript(string script);

        /// <summary>
        /// Send a message to the web content via JavaScript postMessage.
        /// Only works when the current browser is a WebView.
        /// </summary>
        /// <param name="message">The message string to send to web content.</param>
        void SendPostMessage(string message);
    }
}
