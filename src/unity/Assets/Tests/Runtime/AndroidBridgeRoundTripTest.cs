using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime test for the complete Unity→Android→Unity round-trip.
    /// Requires a real Android device or emulator — skipped on non-Android platforms.
    /// </summary>
    public class AndroidBridgeRoundTripTest
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
        [Timeout(15000)]
        public IEnumerator Open_WebView_ReceivesOnPageFinished_WithCorrectUrl()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            // Arrange
            NativeBrowser.Initialize();
            const string targetUrl = "https://www.google.com";
            var config = new BrowserConfig(targetUrl);

            string receivedUrl = null;
            bool callbackReceived = false;

            Action<string> handler = (url) =>
            {
                receivedUrl = url;
                callbackReceived = true;
            };

            NativeBrowser.OnPageFinished += handler;

            try
            {
                // Act
                NativeBrowser.Open(BrowserType.WebView, config);

                // Wait up to 10 seconds for the callback
                float elapsed = 0f;
                while (!callbackReceived && elapsed < 10f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                // Assert
                Assert.IsTrue(callbackReceived, "OnPageFinished callback was not received within 10 seconds");
                Assert.IsNotNull(receivedUrl, "Received URL should not be null");
                Assert.IsTrue(
                    receivedUrl.Contains("google.com"),
                    $"Expected URL to contain 'google.com' but got: {receivedUrl}");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= handler;
            }
#else
            Assert.Ignore("AndroidBridgeRoundTripTest requires UNITY_ANDROID and a real device/emulator.");
            yield break;
#endif
        }
    }
}
