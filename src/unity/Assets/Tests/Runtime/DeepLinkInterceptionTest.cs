using System;
using System.Collections;
using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime test for deep link interception in WebView.
    /// Loads mock_deeplink.html which navigates to myapp://test/deeplink on load.
    /// Requires a real Android device or emulator — skipped on non-Android platforms.
    /// </summary>
    public class DeepLinkInterceptionTest
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
        [Timeout(20000)]
        public IEnumerator DeepLink_TriggeredByPage_ReceivesOnDeepLinkCallback()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            // Arrange
            NativeBrowser.Initialize();
            const string mockPageUrl = "file:///android_asset/mock_deeplink.html";
            var config = new BrowserConfig(mockPageUrl)
            {
                deepLinkPatterns = new List<string> { "myapp://" },
                closeOnDeepLink = false
            };

            string receivedDeepLinkUrl = null;

            Action<string> deepLinkHandler = (url) => receivedDeepLinkUrl = url;
            NativeBrowser.OnDeepLink += deepLinkHandler;

            try
            {
                // Act: open the mock page — it will immediately navigate to myapp://
                NativeBrowser.Open(BrowserType.WebView, config);

                // Wait up to 10 seconds for the deep link callback
                float elapsed = 0f;
                while (receivedDeepLinkUrl == null && elapsed < 10f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                // Assert
                Assert.IsNotNull(receivedDeepLinkUrl,
                    "OnDeepLink callback was not received within 10 seconds");
                Assert.IsTrue(
                    receivedDeepLinkUrl.StartsWith("myapp://"),
                    $"Expected URL to start with 'myapp://' but got: {receivedDeepLinkUrl}");
            }
            finally
            {
                NativeBrowser.OnDeepLink -= deepLinkHandler;
            }
#else
            Assert.Ignore("DeepLinkInterceptionTest requires UNITY_ANDROID and a real device/emulator.");
            yield break;
#endif
        }
    }
}
