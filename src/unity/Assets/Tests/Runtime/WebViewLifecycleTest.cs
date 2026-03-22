using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime test for WebView lifecycle: open → refresh → close.
    /// Verifies IsOpen state transitions at each stage.
    /// Requires a real Android device or emulator — skipped on non-Android platforms.
    /// </summary>
    public class WebViewLifecycleTest
    {
        [TearDown]
        public void TearDown()
        {
            NativeBrowser.Close();
            var go = GameObject.Find("NativeBrowserCallback");
            if (go != null)
                UnityEngine.Object.DestroyImmediate(go);
        }

        [UnityTest]
        [Timeout(30000)]
        public IEnumerator WebView_OpenRefreshClose_IsOpenStateCorrect()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            // Arrange
            NativeBrowser.Initialize();
            var config = new BrowserConfig("about:blank");

            bool pageLoaded = false;
            Action<string> pageFinishedHandler = (url) => pageLoaded = true;
            NativeBrowser.OnPageFinished += pageFinishedHandler;

            try
            {
                // Act: Open
                NativeBrowser.Open(BrowserType.WebView, config);
                yield return null;

                // Verify IsOpen == true after opening
                Assert.IsTrue(NativeBrowser.IsOpen, "IsOpen should be true after Open()");

                // Wait for page to load before refresh (up to 10 seconds)
                float elapsed = 0f;
                while (!pageLoaded && elapsed < 10f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                // Refresh — should not crash
                Assert.DoesNotThrow(() => NativeBrowser.Refresh(), "Refresh should not throw");
                yield return new WaitForSeconds(0.5f);

                // IsOpen should still be true after refresh
                Assert.IsTrue(NativeBrowser.IsOpen, "IsOpen should remain true after Refresh()");

                // Close
                NativeBrowser.Close();
                yield return new WaitForSeconds(0.5f);

                // Verify IsOpen == false after closing
                Assert.IsFalse(NativeBrowser.IsOpen, "IsOpen should be false after Close()");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= pageFinishedHandler;
            }
#else
            Assert.Ignore("WebViewLifecycleTest requires UNITY_ANDROID and a real device/emulator.");
            yield break;
#endif
        }
    }
}
