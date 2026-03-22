using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime test for PostMessage round-trip between Unity and WebView JavaScript.
    /// Loads mock_postmessage.html which responds with "pong" when it receives "ping".
    /// Requires a real Android device or emulator — skipped on non-Android platforms.
    /// </summary>
    public class PostMessageRoundTripTest
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
        public IEnumerator SendPostMessage_Ping_ReceivesPong()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            // Arrange
            NativeBrowser.Initialize();
            const string mockPageUrl = "file:///android_asset/mock_postmessage.html";
            var config = new BrowserConfig(mockPageUrl);

            bool pageLoaded = false;
            string receivedMessage = null;

            Action<string> pageFinishedHandler = (url) => pageLoaded = true;
            Action<string> postMessageHandler = (msg) => receivedMessage = msg;

            NativeBrowser.OnPageFinished += pageFinishedHandler;
            NativeBrowser.OnPostMessage += postMessageHandler;

            try
            {
                // Act: open the mock page
                NativeBrowser.Open(BrowserType.WebView, config);

                // Wait for page to finish loading (up to 10 seconds)
                float elapsed = 0f;
                while (!pageLoaded && elapsed < 10f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                Assert.IsTrue(pageLoaded, "Page did not finish loading within 10 seconds");

                // Send "ping" and wait for "pong" response (up to 5 seconds)
                NativeBrowser.SendPostMessage("ping");

                elapsed = 0f;
                while (receivedMessage == null && elapsed < 5f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                // Assert
                Assert.IsNotNull(receivedMessage, "OnPostMessage callback was not received within 5 seconds");
                Assert.AreEqual("pong", receivedMessage,
                    $"Expected 'pong' response but got: {receivedMessage}");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= pageFinishedHandler;
                NativeBrowser.OnPostMessage -= postMessageHandler;
            }
#else
            Assert.Ignore("PostMessageRoundTripTest requires UNITY_ANDROID and a real device/emulator.");
            yield break;
#endif
        }
    }
}
