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
    /// Tests for the NativeBrowserCallbackReceiver MonoBehaviour singleton.
    /// Validates public API surface, virtual method overrides, JSON parsing, and event pipeline.
    /// </summary>
    public class NativeBrowserCallbackReceiverTest
    {
        private GameObject receiverGameObject;
        private NativeBrowserCallbackReceiver receiver;

        [SetUp]
        public void Setup()
        {
            // Clean any leftover instance from previous test
            var existing = GameObject.Find("NativeBrowserCallback");
            if (existing != null)
                UnityEngine.Object.DestroyImmediate(existing);
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
            receiver = null;
        }

        private NativeBrowserCallbackReceiver InitializeAndGetReceiver()
        {
            NativeBrowser.Initialize();
            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            receiver = receiverGameObject.GetComponent<NativeBrowserCallbackReceiver>();
            return receiver;
        }

        // =====================================================================
        // Public API Surface Tests
        // =====================================================================

        [Test]
        public void NativeBrowserCallbackReceiver_IsPublicClass()
        {
            // Verify the class is public and accessible without reflection
            Type type = typeof(NativeBrowserCallbackReceiver);
            Assert.IsTrue(type.IsPublic, "NativeBrowserCallbackReceiver should be a public class");
        }

        [Test]
        public void NativeBrowserCallbackReceiver_InheritsFromMonoBehaviour()
        {
            Assert.IsTrue(typeof(MonoBehaviour).IsAssignableFrom(typeof(NativeBrowserCallbackReceiver)),
                "NativeBrowserCallbackReceiver should inherit from MonoBehaviour");
        }

        [Test]
        public void CallbackMethods_ArePublicVirtual()
        {
            // All 7 callback methods must be public and virtual for subclass overrides
            string[] methodNames = {
                "OnPageStarted", "OnPageFinished", "OnError",
                "OnPostMessage", "OnJsResult", "OnDeepLink", "OnClosed"
            };

            Type type = typeof(NativeBrowserCallbackReceiver);

            foreach (string name in methodNames)
            {
                MethodInfo method = type.GetMethod(name, BindingFlags.Public | BindingFlags.Instance,
                    null, new[] { typeof(string) }, null);
                Assert.IsNotNull(method, $"{name} should exist as a public instance method");
                Assert.IsTrue(method.IsVirtual, $"{name} should be virtual");
                Assert.IsFalse(method.IsAbstract, $"{name} should not be abstract");
                Assert.AreEqual(typeof(void), method.ReturnType, $"{name} should return void");

                // Verify single string parameter
                ParameterInfo[] parameters = method.GetParameters();
                Assert.AreEqual(1, parameters.Length, $"{name} should have exactly 1 parameter");
                Assert.AreEqual(typeof(string), parameters[0].ParameterType, $"{name} parameter should be string");
                Assert.AreEqual("json", parameters[0].Name, $"{name} parameter should be named 'json'");
            }
        }

        [Test]
        public void CallbackMethods_HavePreserveAttribute()
        {
            // All callback methods must have [Preserve] to survive IL2CPP stripping
            string[] methodNames = {
                "OnPageStarted", "OnPageFinished", "OnError",
                "OnPostMessage", "OnJsResult", "OnDeepLink", "OnClosed"
            };

            Type type = typeof(NativeBrowserCallbackReceiver);

            foreach (string name in methodNames)
            {
                MethodInfo method = type.GetMethod(name, BindingFlags.Public | BindingFlags.Instance,
                    null, new[] { typeof(string) }, null);
                Assert.IsNotNull(method, $"{name} should exist");

                var preserveAttr = method.GetCustomAttribute<UnityEngine.Scripting.PreserveAttribute>();
                Assert.IsNotNull(preserveAttr, $"{name} should have [Preserve] attribute");
            }
        }

        [Test]
        public void InstanceProperty_IsPublicStatic()
        {
            PropertyInfo prop = typeof(NativeBrowserCallbackReceiver)
                .GetProperty("Instance", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(prop, "Instance property should be public and static");
            Assert.AreEqual(typeof(NativeBrowserCallbackReceiver), prop.PropertyType,
                "Instance property should return NativeBrowserCallbackReceiver");
        }

        // =====================================================================
        // Singleton and Initialization Tests
        // =====================================================================

        [UnityTest]
        public IEnumerator Initialize_CreatesGameObjectWithCorrectName()
        {
            NativeBrowser.Initialize();
            yield return null;

            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            Assert.IsNotNull(receiverGameObject, "NativeBrowserCallback GameObject should be created");
            Assert.AreEqual("NativeBrowserCallback", receiverGameObject.name);

            // Verify component is attached — direct type access, no reflection
            receiver = receiverGameObject.GetComponent<NativeBrowserCallbackReceiver>();
            Assert.IsNotNull(receiver, "NativeBrowserCallbackReceiver component should be attached");
        }

        [UnityTest]
        public IEnumerator Initialize_CalledTwice_DoesNotCreateDuplicates()
        {
            NativeBrowser.Initialize();
            yield return null;

            NativeBrowser.Initialize();
            yield return null;

            var allObjects = GameObject.FindObjectsByType<NativeBrowserCallbackReceiver>(FindObjectsSortMode.None);
            Assert.AreEqual(1, allObjects.Length, "Only one NativeBrowserCallbackReceiver should exist after double Initialize");
        }

        // =====================================================================
        // Callback → Event Pipeline Tests (direct calls, no reflection)
        // =====================================================================

        [UnityTest]
        public IEnumerator CallbackMethod_OnPageStarted_FiresEvent()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageStarted += (url) => { receivedUrl = url; };

            recv.OnPageStarted("{\"url\":\"https://test.example.com\"}");
            yield return null;

            Assert.IsNotNull(receivedUrl, "OnPageStarted event should have been invoked");
            Assert.AreEqual("https://test.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnPageFinished_FiresEvent()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageFinished += (url) => { receivedUrl = url; };

            recv.OnPageFinished("{\"url\":\"https://finished.example.com\"}");
            yield return null;

            Assert.IsNotNull(receivedUrl, "OnPageFinished event should have been invoked");
            Assert.AreEqual("https://finished.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnError_FiresEventWithTwoParameters()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedMessage = null;
            string receivedUrl = null;
            NativeBrowser.OnError += (msg, url) =>
            {
                receivedMessage = msg;
                receivedUrl = url;
            };

            recv.OnError("{\"message\":\"Connection failed\",\"url\":\"https://error.example.com\"}");
            yield return null;

            Assert.IsNotNull(receivedMessage, "OnError event should provide error message");
            Assert.IsNotNull(receivedUrl, "OnError event should provide URL");
            Assert.AreEqual("Connection failed", receivedMessage);
            Assert.AreEqual("https://error.example.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnPostMessage_FiresEvent()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedMessage = null;
            NativeBrowser.OnPostMessage += (msg) => { receivedMessage = msg; };

            recv.OnPostMessage("{\"message\":\"Hello from web\"}");
            yield return null;

            Assert.IsNotNull(receivedMessage, "OnPostMessage event should have been invoked");
            Assert.AreEqual("Hello from web", receivedMessage);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnJsResult_FiresEventWithTwoParameters()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedRequestId = null;
            string receivedResult = null;
            NativeBrowser.OnJsResult += (reqId, result) =>
            {
                receivedRequestId = reqId;
                receivedResult = result;
            };

            recv.OnJsResult("{\"requestId\":\"req-123\",\"result\":\"42\"}");
            yield return null;

            Assert.IsNotNull(receivedRequestId, "OnJsResult event should provide request ID");
            Assert.IsNotNull(receivedResult, "OnJsResult event should provide result");
            Assert.AreEqual("req-123", receivedRequestId);
            Assert.AreEqual("42", receivedResult);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnDeepLink_FiresEvent()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnDeepLink += (url) => { receivedUrl = url; };

            recv.OnDeepLink("{\"url\":\"myapp://deeplink/path\"}");
            yield return null;

            Assert.IsNotNull(receivedUrl, "OnDeepLink event should have been invoked");
            Assert.AreEqual("myapp://deeplink/path", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackMethod_OnClosed_FiresEvent()
        {
            var recv = InitializeAndGetReceiver();
            yield return null;

            bool eventFired = false;
            NativeBrowser.OnClosed += () => { eventFired = true; };

            recv.OnClosed("{}");
            yield return null;

            Assert.IsTrue(eventFired, "OnClosed event should have been invoked");
        }

        // =====================================================================
        // Error Handling Tests
        // =====================================================================

        [Test]
        public void CallbackMethod_WithInvalidJson_LogsWarningButDoesNotThrow()
        {
            NativeBrowser.Initialize();
            var recv = GameObject.Find("NativeBrowserCallback")
                .GetComponent<NativeBrowserCallbackReceiver>();

            // Invalid JSON should log warning but not throw exception
            Assert.DoesNotThrow(() =>
            {
                recv.OnPageStarted("not valid json");
            }, "Callback methods should handle invalid JSON gracefully");
        }

        [Test]
        public void CallbackMethod_WithEmptyJson_DoesNotThrow()
        {
            NativeBrowser.Initialize();
            var recv = GameObject.Find("NativeBrowserCallback")
                .GetComponent<NativeBrowserCallbackReceiver>();

            Assert.DoesNotThrow(() =>
            {
                recv.OnPageStarted("");
            }, "Callback methods should handle empty strings gracefully");
        }

        [Test]
        public void CallbackMethod_WithNullFields_DoesNotThrow()
        {
            NativeBrowser.Initialize();
            var recv = GameObject.Find("NativeBrowserCallback")
                .GetComponent<NativeBrowserCallbackReceiver>();

            // JSON with missing fields should not crash
            Assert.DoesNotThrow(() =>
            {
                recv.OnError("{}");
            }, "OnError should handle JSON with missing fields gracefully");
        }

        // =====================================================================
        // Subclass Override Tests
        // =====================================================================

        /// <summary>
        /// Test subclass that tracks which callbacks were invoked and whether base was called.
        /// </summary>
        private class TestCallbackSubclass : NativeBrowserCallbackReceiver
        {
            public string LastMethod { get; private set; }
            public string LastJson { get; private set; }
            public bool BaseCalled { get; private set; }

            public override void OnPageStarted(string json)
            {
                LastMethod = nameof(OnPageStarted);
                LastJson = json;
                base.OnPageStarted(json);
                BaseCalled = true;
            }

            public override void OnPageFinished(string json)
            {
                LastMethod = nameof(OnPageFinished);
                LastJson = json;
                base.OnPageFinished(json);
                BaseCalled = true;
            }

            public override void OnError(string json)
            {
                LastMethod = nameof(OnError);
                LastJson = json;
                base.OnError(json);
                BaseCalled = true;
            }

            public override void OnPostMessage(string json)
            {
                LastMethod = nameof(OnPostMessage);
                LastJson = json;
                base.OnPostMessage(json);
                BaseCalled = true;
            }

            public override void OnJsResult(string json)
            {
                LastMethod = nameof(OnJsResult);
                LastJson = json;
                base.OnJsResult(json);
                BaseCalled = true;
            }

            public override void OnDeepLink(string json)
            {
                LastMethod = nameof(OnDeepLink);
                LastJson = json;
                base.OnDeepLink(json);
                BaseCalled = true;
            }

            public override void OnClosed(string json)
            {
                LastMethod = nameof(OnClosed);
                LastJson = json;
                base.OnClosed(json);
                BaseCalled = true;
            }

            public void Reset()
            {
                LastMethod = null;
                LastJson = null;
                BaseCalled = false;
            }
        }

        [Test]
        public void Subclass_CanOverrideAllCallbackMethods()
        {
            // Verify a subclass can be created and its overrides compile and run
            var go = new GameObject("TestSubclass");
            var sub = go.AddComponent<TestCallbackSubclass>();

            try
            {
                Assert.IsNotNull(sub, "TestCallbackSubclass should be instantiatable as a component");
                Assert.IsInstanceOf<NativeBrowserCallbackReceiver>(sub,
                    "Subclass should be an instance of NativeBrowserCallbackReceiver");
            }
            finally
            {
                UnityEngine.Object.DestroyImmediate(go);
            }
        }

        [UnityTest]
        public IEnumerator Subclass_OnPageStarted_OverrideReceivesJsonAndBaseFiresEvent()
        {
            NativeBrowser.Initialize();
            yield return null;

            // Create subclass instance
            var go = new GameObject("TestSubclass");
            var sub = go.AddComponent<TestCallbackSubclass>();

            string receivedUrl = null;
            NativeBrowser.OnPageStarted += (url) => { receivedUrl = url; };

            string testJson = "{\"url\":\"https://subclass.example.com\"}";
            sub.OnPageStarted(testJson);
            yield return null;

            // Verify subclass received raw JSON
            Assert.AreEqual("OnPageStarted", sub.LastMethod);
            Assert.AreEqual(testJson, sub.LastJson);
            Assert.IsTrue(sub.BaseCalled, "Base method should have been called");

            // Verify base method fired the event
            Assert.AreEqual("https://subclass.example.com", receivedUrl,
                "Base.OnPageStarted should parse JSON and fire event");

            UnityEngine.Object.DestroyImmediate(go);
        }

        [UnityTest]
        public IEnumerator Subclass_OnError_OverrideReceivesJsonAndBaseFiresEvent()
        {
            NativeBrowser.Initialize();
            yield return null;

            var go = new GameObject("TestSubclass");
            var sub = go.AddComponent<TestCallbackSubclass>();

            string receivedMessage = null;
            string receivedUrl = null;
            NativeBrowser.OnError += (msg, url) =>
            {
                receivedMessage = msg;
                receivedUrl = url;
            };

            string testJson = "{\"message\":\"Test error\",\"url\":\"https://error.test\"}";
            sub.OnError(testJson);
            yield return null;

            Assert.AreEqual("OnError", sub.LastMethod);
            Assert.AreEqual(testJson, sub.LastJson);
            Assert.IsTrue(sub.BaseCalled);
            Assert.AreEqual("Test error", receivedMessage);
            Assert.AreEqual("https://error.test", receivedUrl);

            UnityEngine.Object.DestroyImmediate(go);
        }

        [UnityTest]
        public IEnumerator Subclass_OnClosed_OverrideReceivesJsonAndBaseFiresEvent()
        {
            NativeBrowser.Initialize();
            yield return null;

            var go = new GameObject("TestSubclass");
            var sub = go.AddComponent<TestCallbackSubclass>();

            bool eventFired = false;
            NativeBrowser.OnClosed += () => { eventFired = true; };

            sub.OnClosed("{}");
            yield return null;

            Assert.AreEqual("OnClosed", sub.LastMethod);
            Assert.IsTrue(sub.BaseCalled);
            Assert.IsTrue(eventFired, "Base.OnClosed should fire the OnClosed event");

            UnityEngine.Object.DestroyImmediate(go);
        }
    }
}
