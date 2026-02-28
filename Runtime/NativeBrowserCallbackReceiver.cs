using System;
using UnityEngine;
using UnityEngine.Scripting;

namespace TedLiou.NativeBrowser
{
    /// <summary>
    /// MonoBehaviour that receives native browser callbacks (Android UnitySendMessage, Windows dispatcher).
    /// Automatically instantiated by NativeBrowser.Initialize() and persists across scenes.
    /// Override virtual methods to add custom callback handling; always call base to preserve event pipeline.
    /// </summary>
    public class NativeBrowserCallbackReceiver : MonoBehaviour
    {
        private static NativeBrowserCallbackReceiver instance;

        /// <summary>
        /// Gets or creates the singleton instance of the callback receiver.
        /// Creates a persistent GameObject named "NativeBrowserCallback" that matches Android bridge configuration.
        /// </summary>
        public static NativeBrowserCallbackReceiver Instance
        {
            get
            {
                if (instance == null)
                {
                    // Check if an instance already exists in the scene
                    instance = FindObjectOfType<NativeBrowserCallbackReceiver>();
                    
                    if (instance == null)
                    {
                        // Create new GameObject with exact name expected by Android bridge
                        GameObject go = new GameObject("NativeBrowserCallback");
                        instance = go.AddComponent<NativeBrowserCallbackReceiver>();
                        if (!Application.isBatchMode) DontDestroyOnLoad(go);
                    }
                }
                return instance;
            }
        }

        /// <summary>
        /// Called when a WebView page starts loading.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnPageStarted", json)
        /// JSON format: {"url": "https://..."}
        /// </summary>
        [Preserve]
        public virtual void OnPageStarted(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<UrlMessage>(json);
                NativeBrowser.RaiseOnPageStarted(data.url);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnPageStarted failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when a WebView page finishes loading.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnPageFinished", json)
        /// JSON format: {"url": "https://..."}
        /// </summary>
        [Preserve]
        public virtual void OnPageFinished(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<UrlMessage>(json);
                NativeBrowser.RaiseOnPageFinished(data.url);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnPageFinished failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when a WebView encounters an error.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnError", json)
        /// JSON format: {"message": "...", "url": "..."}
        /// </summary>
        [Preserve]
        public virtual void OnError(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<ErrorMessage>(json);
                NativeBrowser.RaiseOnError(data.message, data.url);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnError failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when a WebView receives a PostMessage from JavaScript.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnPostMessage", json)
        /// JSON format: {"message": "..."}
        /// </summary>
        [Preserve]
        public virtual void OnPostMessage(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<PostMessageData>(json);
                NativeBrowser.RaiseOnPostMessage(data.message);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnPostMessage failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when JavaScript execution returns a result.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnJsResult", json)
        /// JSON format: {"requestId": "...", "result": "..."}
        /// </summary>
        [Preserve]
        public virtual void OnJsResult(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<JsResultData>(json);
                NativeBrowser.RaiseOnJsResult(data.requestId, data.result);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnJsResult failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when a deep link is triggered in the WebView.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnDeepLink", json)
        /// JSON format: {"url": "https://..."}
        /// </summary>
        [Preserve]
        public virtual void OnDeepLink(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<UrlMessage>(json);
                NativeBrowser.RaiseOnDeepLink(data.url);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnDeepLink failed to parse JSON: {ex.Message}");
            }
        }

        /// <summary>
        /// Called when the browser is closed.
        /// Invoked by Android: UnitySendMessage("NativeBrowserCallback", "OnClosed", json)
        /// JSON format: {}
        /// </summary>
        [Preserve]
        public virtual void OnClosed(string json)
        {
            try
            {
                // OnClosed doesn't need data parsing - just fire the event
                NativeBrowser.RaiseOnClosed();
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"NativeBrowserCallbackReceiver: OnClosed failed: {ex.Message}");
            }
        }
    }

    // Internal JSON message classes for JsonUtility deserialization
    // These match the JSON format sent by Android BrowserBridge.kt

    [Serializable]
    internal class UrlMessage
    {
        public string url;
    }

    [Serializable]
    internal class ErrorMessage
    {
        public string type;
        public string message;
        public string url;
        public string requestId;
    }

    [Serializable]
    internal class PostMessageData
    {
        public string message;
    }

    [Serializable]
    internal class JsResultData
    {
        public string requestId;
        public string result;
    }
}
