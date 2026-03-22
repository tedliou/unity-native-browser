using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// PlayMode tests verifying NativeBrowser.IsOpen always returns false on non-Android platforms.
    /// On Android, IsOpen reflects the real browser state via the native bridge.
    /// In the Unity Editor and on non-Android platforms, the EditorBridge/WindowsBridge stub
    /// always returns false because no real browser window is managed.
    /// </summary>
    public class IsOpenStateTest
    {
        [TearDown]
        public void TearDown()
        {
            var go = GameObject.Find("NativeBrowserCallback");
            if (go != null)
                UnityEngine.Object.DestroyImmediate(go);

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // Best-effort close on Windows to avoid leaving a window open
            try { NativeBrowser.Close(); } catch { }
#endif
        }

        [Test]
        public void IsOpen_BeforeInitialize_ReturnsFalse()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            Assert.Ignore("Test targets non-Android platforms only");
#endif
#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // Windows bridge returns false when no browser is open
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false before Initialize on Windows");
#else
            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false before Initialize on non-Android platforms");
#endif
        }

        [Test]
        public void IsOpen_AfterInitialize_ReturnsFalse()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            Assert.Ignore("Test targets non-Android platforms only");
#endif
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Initialize when no browser is open on Windows");
#else
            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Initialize on non-Android platforms");
#endif
        }

        [UnityTest]
        public IEnumerator IsOpen_AfterOpen_ReturnsFalse_OnNonAndroid()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            Assert.Ignore("Test targets non-Android platforms only");
            yield break;
#endif
            NativeBrowser.Initialize();
            yield return null;

            var config = new BrowserConfig("https://example.com")
            {
                width = 0.9f,
                height = 0.8f
            };

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, Open actually creates a window — close it immediately
            NativeBrowser.Open(BrowserType.WebView, config);
            yield return null;
            try { NativeBrowser.Close(); } catch { }
            yield return null;
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Close on Windows");
#else
            // EditorBridge logs warning and does nothing — IsOpen stays false
            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: Open is Android-only and was called in editor or non-Android platform");
            NativeBrowser.Open(BrowserType.WebView, config);
            yield return null;

            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Open on non-Android platforms");
#endif
        }

        [UnityTest]
        public IEnumerator IsOpen_AfterClose_ReturnsFalse_OnNonAndroid()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            Assert.Ignore("Test targets non-Android platforms only");
            yield break;
#endif
            NativeBrowser.Initialize();
            yield return null;

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            try { NativeBrowser.Close(); } catch { }
            yield return null;
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Close on Windows");
#else
            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: Close is Android-only and was called in editor or non-Android platform");
            NativeBrowser.Close();
            yield return null;

            LogAssert.Expect(LogType.Warning,
                "NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            Assert.IsFalse(NativeBrowser.IsOpen,
                "IsOpen should return false after Close on non-Android platforms");
#endif
        }
    }
}
