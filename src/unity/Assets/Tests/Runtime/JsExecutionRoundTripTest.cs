using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime test for JavaScript execution round-trip.
    /// Opens about:blank, executes JS, and verifies the OnJsResult callback.
    /// Requires a real Android device or emulator — skipped on non-Android platforms.
    /// </summary>
    public class JsExecutionRoundTripTest
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
        public IEnumerator ExecuteJavaScript_DocumentTitle_ReceivesResult()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            // Arrange
            NativeBrowser.Initialize();
            var config = new BrowserConfig("about:blank");

            bool pageLoaded = false;
            string receivedRequestId = null;
            string receivedResult = null;

            Action<string> pageFinishedHandler = (url) => pageLoaded = true;
            Action<string, string> jsResultHandler = (requestId, result) =>
            {
                receivedRequestId = requestId;
                receivedResult = result;
            };

            NativeBrowser.OnPageFinished += pageFinishedHandler;
            NativeBrowser.OnJsResult += jsResultHandler;

            try
            {
                // Act: open about:blank
                NativeBrowser.Open(BrowserType.WebView, config);

                // Wait for page to finish loading (up to 10 seconds)
                float elapsed = 0f;
                while (!pageLoaded && elapsed < 10f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                Assert.IsTrue(pageLoaded, "Page did not finish loading within 10 seconds");

                // Execute JS and wait for result (up to 5 seconds)
                NativeBrowser.ExecuteJavaScript("document.title", "req-1");

                elapsed = 0f;
                while (receivedRequestId == null && elapsed < 5f)
                {
                    yield return null;
                    elapsed += Time.deltaTime;
                }

                // Assert
                Assert.IsNotNull(receivedRequestId, "OnJsResult callback was not received within 5 seconds");
                Assert.AreEqual("req-1", receivedRequestId,
                    $"Expected requestId 'req-1' but got: {receivedRequestId}");
                Assert.IsNotNull(receivedResult,
                    "JS result should not be null");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= pageFinishedHandler;
                NativeBrowser.OnJsResult -= jsResultHandler;
            }
#else
            Assert.Ignore("JsExecutionRoundTripTest requires UNITY_ANDROID and a real device/emulator.");
            yield break;
#endif
        }
    }
}
