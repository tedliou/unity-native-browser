using UnityEngine;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Editor mode stub implementation of IPlatformBridge.
    /// Provides no-op implementations for all browser operations with appropriate logging.
    /// Used when running in the Unity Editor or on non-Android platforms.
    /// </summary>
    internal class EditorBridge : IPlatformBridge
    {
        public void Initialize()
        {
            // No-op: Initialize succeeds silently in Editor mode
        }

        public void Open(string type, string configJson)
        {
            Debug.LogWarning($"NativeBrowser: Open is Android-only and was called in editor or non-Android platform");
        }

        public void Close()
        {
            Debug.LogWarning($"NativeBrowser: Close is Android-only and was called in editor or non-Android platform");
        }

        public void Refresh()
        {
            Debug.LogWarning($"NativeBrowser: Refresh is Android-only and was called in editor or non-Android platform");
        }

        public bool IsOpen()
        {
            Debug.LogWarning($"NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            return false;
        }

        public void ExecuteJavaScript(string script, string requestId)
        {
            Debug.LogWarning($"NativeBrowser: ExecuteJavaScript is Android-only and was called in editor or non-Android platform");
        }

        public void InjectJavaScript(string script)
        {
            Debug.LogWarning($"NativeBrowser: InjectJavaScript is Android-only and was called in editor or non-Android platform");
        }
    }
}
