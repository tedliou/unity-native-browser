using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Integration tests for the NativeBrowser static API.
    /// Tests behavior in Unity Editor across all supported platforms.
    /// On Windows Editor, the WindowsBridge is active (with native DLL).
    /// On non-Windows/non-Android Editor, the EditorBridge stub is active.
    /// </summary>
    public class NativeBrowserIntegrationTest
    {
        [TearDown]
        public void TearDown()
        {
            // Clean up callback receiver GameObject
            var go = GameObject.Find("NativeBrowserCallback");
            if (go != null)
            {
                UnityEngine.Object.DestroyImmediate(go);
            }
        }

        [Test]
        public void Initialize_InEditor_SucceedsWithoutException()
        {
            // Act & Assert: Initialize should not throw exception even in Editor
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Initialize();
            }, "Initialize should work in Editor without crashing");

            // Verify GameObject was created
            var go = GameObject.Find("NativeBrowserCallback");
            Assert.IsNotNull(go, "Callback receiver GameObject should be created");
        }

        [Test]
        public void Close_InEditor_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, WindowsBridge handles Close via native DLL — no "Android-only" warning
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Close();
            }, "Close should handle Windows Editor environment gracefully");
#else
            // On non-Windows Editor, EditorBridge logs "Android-only" warning
            LogAssert.Expect(LogType.Warning, "NativeBrowser: Close is Android-only and was called in editor or non-Android platform");
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Close();
            }, "Close should handle Editor environment gracefully");
#endif
        }

        [Test]
        public void Refresh_InEditor_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Refresh();
            }, "Refresh should handle Windows Editor environment gracefully");
#else
            LogAssert.Expect(LogType.Warning, "NativeBrowser: Refresh is Android-only and was called in editor or non-Android platform");
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Refresh();
            }, "Refresh should handle Editor environment gracefully");
#endif
        }

        [Test]
        public void ExecuteJavaScript_InEditor_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.ExecuteJavaScript("console.log('test');", "req-1");
            }, "ExecuteJavaScript should handle Windows Editor environment gracefully");
#else
            LogAssert.Expect(LogType.Warning, "NativeBrowser: ExecuteJavaScript is Android-only and was called in editor or non-Android platform");
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.ExecuteJavaScript("console.log('test');", "req-1");
            }, "ExecuteJavaScript should handle Editor environment gracefully");
#endif
        }

        [Test]
        public void InjectJavaScript_InEditor_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.InjectJavaScript("window.testValue = 123;");
            }, "InjectJavaScript should handle Windows Editor environment gracefully");
#else
            LogAssert.Expect(LogType.Warning, "NativeBrowser: InjectJavaScript is Android-only and was called in editor or non-Android platform");
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.InjectJavaScript("window.testValue = 123;");
            }, "InjectJavaScript should handle Editor environment gracefully");
#endif
        }

        [Test]
        public void IsOpen_InEditor_ReturnsFalse()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, IsOpen calls the DLL — should return false when no browser is open
            bool isOpen = NativeBrowser.IsOpen;
            Assert.IsFalse(isOpen, "IsOpen should return false when no browser is open");
#else
            LogAssert.Expect(LogType.Warning, "NativeBrowser: IsOpen is Android-only and was called in editor or non-Android platform");
            bool isOpen = NativeBrowser.IsOpen;
            Assert.IsFalse(isOpen, "IsOpen should return false in Editor");
#endif
        }

        [Test]
        public void Open_InEditor_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();
            var config = new BrowserConfig("https://example.com")
            {
                width = 0.9f,
                height = 0.8f,
                alignment = Alignment.CENTER
            };

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, Open calls the DLL to create a standalone window
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Open(BrowserType.WebView, config);
            }, "Open should handle Windows Editor environment gracefully");

            // Clean up: close the browser if it was opened
            try { NativeBrowser.Close(); } catch { }
#else
            LogAssert.Expect(LogType.Warning, "NativeBrowser: Open is Android-only and was called in editor or non-Android platform");
            Assert.DoesNotThrow(() => 
            {
                NativeBrowser.Open(BrowserType.WebView, config);
            }, "Open should handle Editor environment gracefully");
#endif
        }

        [Test]
        public void EventSubscribe_WorksWithoutException()
        {
            // Arrange
            NativeBrowser.Initialize();
            bool pageStartedFired = false;
            bool pageFinishedFired = false;
            bool errorFired = false;
            bool postMessageFired = false;
            bool jsResultFired = false;
            bool deepLinkFired = false;
            bool closedFired = false;

            // Act: Subscribe to all events
            Assert.DoesNotThrow(() =>
            {
                NativeBrowser.OnPageStarted += (url) => { pageStartedFired = true; };
                NativeBrowser.OnPageFinished += (url) => { pageFinishedFired = true; };
                NativeBrowser.OnError += (msg, url) => { errorFired = true; };
                NativeBrowser.OnPostMessage += (msg) => { postMessageFired = true; };
                NativeBrowser.OnJsResult += (reqId, result) => { jsResultFired = true; };
                NativeBrowser.OnDeepLink += (url) => { deepLinkFired = true; };
                NativeBrowser.OnClosed += () => { closedFired = true; };
            }, "Event subscription should work without exceptions");

            // Assert: Events should be subscribed (not null internally, but we can't check that directly)
            // The fact that no exception was thrown is sufficient validation
            Assert.Pass("All events subscribed successfully");
        }

        [Test]
        public void EventUnsubscribe_WorksWithoutException()
        {
            // Arrange
            NativeBrowser.Initialize();
            Action<string> pageStartedHandler = (url) => { };
            Action<string> pageFinishedHandler = (url) => { };
            Action<string, string> errorHandler = (msg, url) => { };
            Action<string> postMessageHandler = (msg) => { };
            Action<string, string> jsResultHandler = (reqId, result) => { };
            Action<string> deepLinkHandler = (url) => { };
            Action closedHandler = () => { };

            // Subscribe first
            NativeBrowser.OnPageStarted += pageStartedHandler;
            NativeBrowser.OnPageFinished += pageFinishedHandler;
            NativeBrowser.OnError += errorHandler;
            NativeBrowser.OnPostMessage += postMessageHandler;
            NativeBrowser.OnJsResult += jsResultHandler;
            NativeBrowser.OnDeepLink += deepLinkHandler;
            NativeBrowser.OnClosed += closedHandler;

            // Act: Unsubscribe from all events
            Assert.DoesNotThrow(() =>
            {
                NativeBrowser.OnPageStarted -= pageStartedHandler;
                NativeBrowser.OnPageFinished -= pageFinishedHandler;
                NativeBrowser.OnError -= errorHandler;
                NativeBrowser.OnPostMessage -= postMessageHandler;
                NativeBrowser.OnJsResult -= jsResultHandler;
                NativeBrowser.OnDeepLink -= deepLinkHandler;
                NativeBrowser.OnClosed -= closedHandler;
            }, "Event unsubscription should work without exceptions");

            // Assert: The fact that no exception was thrown is sufficient validation
            Assert.Pass("All events unsubscribed successfully");
        }

        [UnityTest]
        public IEnumerator MultipleInitialize_DoesNotCreateMultipleReceivers()
        {
            // Act: Call Initialize multiple times
            NativeBrowser.Initialize();
            yield return null;
            
            NativeBrowser.Initialize();
            yield return null;
            
            NativeBrowser.Initialize();
            yield return null;

            // Assert: Only one callback receiver GameObject should exist
            var allObjects = GameObject.FindObjectsOfType<GameObject>();
            int count = 0;
            foreach (var obj in allObjects)
            {
                if (obj.name == "NativeBrowserCallback")
                {
                    count++;
                }
            }
            
            Assert.AreEqual(1, count, "Multiple Initialize calls should not create duplicate callback receivers");
        }

        [Test]
        public void ExecuteJavaScript_WithEmptyScript_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, empty script triggers a warning from WindowsBridge
            LogAssert.Expect(LogType.Warning, "NativeBrowser: ExecuteJavaScript called with empty script");
            NativeBrowser.ExecuteJavaScript("");
#else
            // On non-Windows Editor, EditorBridge logs "Android-only" warning
            LogAssert.Expect(LogType.Warning, "NativeBrowser: ExecuteJavaScript is Android-only and was called in editor or non-Android platform");
            NativeBrowser.ExecuteJavaScript("");
#endif
        }

        [Test]
        public void InjectJavaScript_WithEmptyScript_HandlesGracefully()
        {
            // Arrange
            NativeBrowser.Initialize();

#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
            // On Windows, empty script triggers a warning from WindowsBridge
            LogAssert.Expect(LogType.Warning, "NativeBrowser: InjectJavaScript called with empty script");
            NativeBrowser.InjectJavaScript("");
#else
            // On non-Windows Editor, EditorBridge logs "Android-only" warning
            LogAssert.Expect(LogType.Warning, "NativeBrowser: InjectJavaScript is Android-only and was called in editor or non-Android platform");
            NativeBrowser.InjectJavaScript("");
#endif
        }

        [Test]
        public void BrowserConfig_Serialization_ProducesValidJson()
        {
            // Arrange
            var config = new BrowserConfig("https://test.example.com")
            {
                width = 0.75f,
                height = 0.85f,
                alignment = Alignment.LEFT,
                closeOnTapOutside = true
            };

            // Act
            string json = config.ToJson();

            // Assert: JSON should be valid and contain expected fields
            Assert.IsNotNull(json, "ToJson should return non-null string");
            Assert.IsTrue(json.Length > 0, "ToJson should return non-empty string");
            Assert.IsTrue(json.Contains("https://test.example.com"), "JSON should contain URL");
            Assert.IsTrue(json.Contains("0.75") || json.Contains("0,75"), "JSON should contain width value");
        }
    }
}
