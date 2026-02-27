using System;
using System.Collections;
using System.Reflection;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Tests for the internal NativeBrowserCallbackReceiver MonoBehaviour singleton.
    /// Uses reflection to access the internal class and test its behavior.
    /// </summary>
    public class NativeBrowserCallbackReceiverTest
    {
        private Type receiverType;
        private GameObject receiverGameObject;

        [SetUp]
        public void Setup()
        {
            // Get the internal NativeBrowserCallbackReceiver type via reflection
            var assembly = typeof(NativeBrowser).Assembly;
            receiverType = assembly.GetType("TedLiou.NativeBrowser.NativeBrowserCallbackReceiver");
            Assert.IsNotNull(receiverType, "NativeBrowserCallbackReceiver type should be accessible via reflection");
        }

        [TearDown]
        public void TearDown()
        {
            // Clean up any created GameObjects to avoid state leakage between tests
            var go = GameObject.Find("NativeBrowserCallback");
            if (go != null)
            {
                UnityEngine.Object.DestroyImmediate(go);
            }
            receiverGameObject = null;
        }

        [UnityTest]
        public IEnumerator Initialize_CreatesGameObjectWithCorrectName()
        {
            // Act: Initialize should create the callback receiver GameObject
            NativeBrowser.Initialize();
            yield return null; // Wait one frame for GameObject creation

            // Assert: GameObject should exist with exact name
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            Assert.IsNotNull(receiverGameObject, "NativeBrowserCallback GameObject should be created");
            Assert.AreEqual("NativeBrowserCallback", receiverGameObject.name);

            // Verify component is attached
            var component = receiverGameObject.GetComponent(receiverType);
            Assert.IsNotNull(component, "NativeBrowserCallbackReceiver component should be attached");
        }

        [UnityTest]
        public IEnumerator Initialize_CalledTwice_DoesNotCreateDuplicates()
        {
            // Act: Call Initialize twice
            NativeBrowser.Initialize();
            yield return null;
            
            NativeBrowser.Initialize();
            yield return null;

            // Assert: Only one GameObject should exist
            var allObjects = GameObject.FindObjectsOfType(receiverType);
            Assert.AreEqual(1, allObjects.Length, "Only one NativeBrowserCallbackReceiver should exist after double Initialize");

            // Cleanup
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnPageStarted_FiresEvent()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageStarted += (url) => { receivedUrl = url; };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnPageStarted method via reflection
            var method = receiverType.GetMethod("OnPageStarted", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"url\":\"https://test.example.com\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with correct URL
            Assert.IsNotNull(receivedUrl, "OnPageStarted event should have been invoked");
            Assert.AreEqual("https://test.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnPageFinished_FiresEvent()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageFinished += (url) => { receivedUrl = url; };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnPageFinished method via reflection
            var method = receiverType.GetMethod("OnPageFinished", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"url\":\"https://finished.example.com\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with correct URL
            Assert.IsNotNull(receivedUrl, "OnPageFinished event should have been invoked");
            Assert.AreEqual("https://finished.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnError_FiresEventWithTwoParameters()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedMessage = null;
            string receivedUrl = null;
            NativeBrowser.OnError += (msg, url) => 
            { 
                receivedMessage = msg; 
                receivedUrl = url; 
            };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnError method via reflection
            var method = receiverType.GetMethod("OnError", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"message\":\"Connection failed\",\"url\":\"https://error.example.com\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with both parameters
            Assert.IsNotNull(receivedMessage, "OnError event should provide error message");
            Assert.IsNotNull(receivedUrl, "OnError event should provide URL");
            Assert.AreEqual("Connection failed", receivedMessage);
            Assert.AreEqual("https://error.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnPostMessage_FiresEvent()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedMessage = null;
            NativeBrowser.OnPostMessage += (msg) => { receivedMessage = msg; };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnPostMessage method via reflection
            var method = receiverType.GetMethod("OnPostMessage", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"message\":\"Hello from web\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with message content
            Assert.IsNotNull(receivedMessage, "OnPostMessage event should have been invoked");
            Assert.AreEqual("Hello from web", receivedMessage);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnJsResult_FiresEventWithTwoParameters()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedRequestId = null;
            string receivedResult = null;
            NativeBrowser.OnJsResult += (reqId, result) => 
            { 
                receivedRequestId = reqId; 
                receivedResult = result; 
            };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnJsResult method via reflection
            var method = receiverType.GetMethod("OnJsResult", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"requestId\":\"req-123\",\"result\":\"42\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with both parameters
            Assert.IsNotNull(receivedRequestId, "OnJsResult event should provide request ID");
            Assert.IsNotNull(receivedResult, "OnJsResult event should provide result");
            Assert.AreEqual("req-123", receivedRequestId);
            Assert.AreEqual("42", receivedResult);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnDeepLink_FiresEvent()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnDeepLink += (url) => { receivedUrl = url; };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnDeepLink method via reflection
            var method = receiverType.GetMethod("OnDeepLink", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{\"url\":\"myapp://deeplink/path\"}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired with deep link URL
            Assert.IsNotNull(receivedUrl, "OnDeepLink event should have been invoked");
            Assert.AreEqual("myapp://deeplink/path", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnClosed_FiresEvent()
        {
            // Arrange: Initialize and subscribe to event
            NativeBrowser.Initialize();
            yield return null;

            bool eventFired = false;
            NativeBrowser.OnClosed += () => { eventFired = true; };

            // Get the receiver component
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act: Call OnClosed method via reflection
            var method = receiverType.GetMethod("OnClosed", BindingFlags.Public | BindingFlags.Instance);
            string testJson = "{}";
            method.Invoke(component, new object[] { testJson });

            yield return null;

            // Assert: Event should have been fired
            Assert.IsTrue(eventFired, "OnClosed event should have been invoked");
        }

        [Test]
        public void CallbackMethod_WithInvalidJson_LogsWarningButDoesNotThrow()
        {
            // Arrange: Initialize
            NativeBrowser.Initialize();
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            var component = receiverGameObject.GetComponent(receiverType);

            // Act & Assert: Invalid JSON should log warning but not throw exception
            var method = receiverType.GetMethod("OnPageStarted", BindingFlags.Public | BindingFlags.Instance);
            string invalidJson = "not valid json";
            
            Assert.DoesNotThrow(() => 
            {
                method.Invoke(component, new object[] { invalidJson });
            }, "Callback methods should handle invalid JSON gracefully");
        }
    }
}
