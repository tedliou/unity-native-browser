using System;
using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using UnityEngine;
using UnityEngine.Scripting;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Thread-safe callback dispatcher for Windows WebView2 native layer.
    /// The Rust DLL fires callbacks from a COM STA thread; this class queues them
    /// and drains on Unity's main thread via Update().
    /// </summary>
    internal class WindowsCallbackDispatcher : MonoBehaviour
    {
        private static WindowsCallbackDispatcher instance;
        private static readonly ConcurrentQueue<string> callbackQueue = new ConcurrentQueue<string>();

        /// <summary>
        /// Gets or creates the singleton instance, attached to the NativeBrowserCallback GameObject.
        /// </summary>
        public static WindowsCallbackDispatcher Instance
        {
            get
            {
                if (instance == null)
                {
                    instance = FindObjectOfType<WindowsCallbackDispatcher>();

                    if (instance == null)
                    {
                        // Reuse the existing NativeBrowserCallback GameObject if possible
                        var receiver = NativeBrowserCallbackReceiver.Instance;
                        instance = receiver.gameObject.GetComponent<WindowsCallbackDispatcher>();
                        if (instance == null)
                        {
                            instance = receiver.gameObject.AddComponent<WindowsCallbackDispatcher>();
                        }
                    }
                }
                return instance;
            }
        }

        /// <summary>
        /// The delegate type matching the Rust UnityCallback signature:
        /// typedef void (*UnityCallback)(const char* event_name, const char* json_data);
        /// </summary>
        /// <param name="eventName">Null-terminated UTF-8 event name pointer.</param>
        /// <param name="jsonData">Null-terminated UTF-8 JSON data pointer.</param>
        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void NativeCallbackDelegate(IntPtr eventName, IntPtr jsonData);

        /// <summary>
        /// Static callback invoked from the Rust STA thread.
        /// Marshals the string pointers and enqueues for main-thread processing.
        /// </summary>
        [Preserve]
        [AOT.MonoPInvokeCallback(typeof(NativeCallbackDelegate))]
        public static void OnNativeCallback(IntPtr eventNamePtr, IntPtr jsonDataPtr)
        {
            try
            {
                string eventName = Marshal.PtrToStringAnsi(eventNamePtr) ?? "";
                string jsonData = Marshal.PtrToStringAnsi(jsonDataPtr) ?? "";
                callbackQueue.Enqueue(eventName + "\n" + jsonData);
            }
            catch (Exception ex)
            {
                // Cannot log from background thread safely in all Unity versions
                // Enqueue an error event instead
                callbackQueue.Enqueue("OnError\n{\"type\":\"CALLBACK_ERROR\",\"message\":\"" +
                    ex.Message.Replace("\"", "\\\"") + "\",\"url\":\"\",\"requestId\":\"\"}");
            }
        }

        /// <summary>
        /// Manually drain the callback queue and log all events.
        /// Use in batchmode or tests where Update() is not called.
        /// Returns the number of events processed.
        /// </summary>
        public static int DrainCallbacks()
        {
            int processed = 0;
            while (callbackQueue.TryDequeue(out string payload))
            {
                processed++;
                int separatorIndex = payload.IndexOf('\n');
                if (separatorIndex < 0)
                {
                    Debug.Log($"WindowsCallbackDispatcher: raw payload: {payload}");
                    continue;
                }

                string eventName = payload.Substring(0, separatorIndex);
                string jsonData = payload.Substring(separatorIndex + 1);
                Debug.Log($"WindowsCallbackDispatcher: DrainCallbacks [{eventName}] {jsonData}");

                try
                {
                    DispatchEvent(eventName, jsonData);
                }
                catch (Exception ex)
                {
                    Debug.LogWarning($"WindowsCallbackDispatcher: Failed to dispatch {eventName}: {ex.Message}");
                }
            }
            return processed;
        }

        private void Update()
        {
            int processed = 0;
            const int maxPerFrame = 64;

            while (processed < maxPerFrame && callbackQueue.TryDequeue(out string payload))
            {
                processed++;
                int separatorIndex = payload.IndexOf('\n');
                if (separatorIndex < 0) continue;

                string eventName = payload.Substring(0, separatorIndex);
                string jsonData = payload.Substring(separatorIndex + 1);

                try
                {
                    DispatchEvent(eventName, jsonData);
                }
                catch (Exception ex)
                {
                    Debug.LogWarning($"WindowsCallbackDispatcher: Failed to dispatch {eventName}: {ex.Message}");
                }
            }
        }

        /// <summary>
        /// Route a native event to the appropriate NativeBrowser event raiser.
        /// Event names and JSON shapes match the Android bridge exactly.
        /// </summary>
        private static void DispatchEvent(string eventName, string jsonData)
        {
            switch (eventName)
            {
                case "OnPageStarted":
                {
                    var data = JsonUtility.FromJson<UrlMessage>(jsonData);
                    NativeBrowser.RaiseOnPageStarted(data.url);
                    break;
                }
                case "OnPageFinished":
                {
                    var data = JsonUtility.FromJson<UrlMessage>(jsonData);
                    NativeBrowser.RaiseOnPageFinished(data.url);
                    break;
                }
                case "OnError":
                {
                    var data = JsonUtility.FromJson<ErrorMessage>(jsonData);
                    NativeBrowser.RaiseOnError(data.message, data.url);
                    break;
                }
                case "OnPostMessage":
                {
                    var data = JsonUtility.FromJson<PostMessageData>(jsonData);
                    NativeBrowser.RaiseOnPostMessage(data.message);
                    break;
                }
                case "OnJsResult":
                {
                    var data = JsonUtility.FromJson<JsResultData>(jsonData);
                    NativeBrowser.RaiseOnJsResult(data.requestId, data.result);
                    break;
                }
                case "OnDeepLink":
                {
                    var data = JsonUtility.FromJson<UrlMessage>(jsonData);
                    NativeBrowser.RaiseOnDeepLink(data.url);
                    break;
                }
                case "OnClosed":
                {
                    NativeBrowser.RaiseOnClosed();
                    break;
                }
                default:
                {
                    Debug.LogWarning($"WindowsCallbackDispatcher: Unknown event '{eventName}'");
                    break;
                }
            }
        }

        /// <summary>
        /// Clear the callback queue. Called on destroy to prevent stale events.
        /// </summary>
        private void OnDestroy()
        {
            while (callbackQueue.TryDequeue(out _)) { }
            instance = null;
        }
    }
}
