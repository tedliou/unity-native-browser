using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Reflection;
using System.Runtime.InteropServices;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// Runtime tests for WindowsCallbackDispatcher.
    /// Tests singleton behavior, callback queueing, event dispatching, and thread safety.
    /// Uses reflection to access the internal class.
    /// </summary>
    public class WindowsCallbackDispatcherTest
    {
        private Type dispatcherType;
        private Type delegateType;
        private GameObject receiverGameObject;

        [SetUp]
        public void Setup()
        {
            var assembly = typeof(NativeBrowser).Assembly;
            dispatcherType = assembly.GetType("TedLiou.NativeBrowser.Internal.WindowsCallbackDispatcher");
            Assert.IsNotNull(dispatcherType, "WindowsCallbackDispatcher type should be accessible via reflection");

            delegateType = dispatcherType.GetNestedType("NativeCallbackDelegate",
                BindingFlags.Public | BindingFlags.NonPublic);
            Assert.IsNotNull(delegateType, "NativeCallbackDelegate should be a nested type");
        }

        [TearDown]
        public void TearDown()
        {
            var go = GameObject.Find("NativeBrowserCallback");
            if (go != null)
            {
                UnityEngine.Object.DestroyImmediate(go);
            }
            receiverGameObject = null;
        }

        // ─── Type Structure Tests ──────────────────────────────────────────────

        [Test]
        public void WindowsCallbackDispatcher_IsInternalMonoBehaviour()
        {
            Assert.IsFalse(dispatcherType.IsPublic, "WindowsCallbackDispatcher should be internal");
            Assert.IsTrue(typeof(MonoBehaviour).IsAssignableFrom(dispatcherType),
                "WindowsCallbackDispatcher should derive from MonoBehaviour");
        }

        [Test]
        public void NativeCallbackDelegate_HasCorrectSignature()
        {
            // The delegate should accept (IntPtr, IntPtr) matching Rust: extern "C" fn(*const c_char, *const c_char)
            Assert.IsTrue(typeof(Delegate).IsAssignableFrom(delegateType),
                "NativeCallbackDelegate should be a delegate type");

            var invokeMethod = delegateType.GetMethod("Invoke");
            Assert.IsNotNull(invokeMethod, "Delegate should have Invoke method");
            Assert.AreEqual(typeof(void), invokeMethod.ReturnType,
                "Callback delegate should return void");

            var parameters = invokeMethod.GetParameters();
            Assert.AreEqual(2, parameters.Length, "Callback delegate should take 2 parameters");
            Assert.AreEqual(typeof(IntPtr), parameters[0].ParameterType,
                "First parameter should be IntPtr (event name pointer)");
            Assert.AreEqual(typeof(IntPtr), parameters[1].ParameterType,
                "Second parameter should be IntPtr (json data pointer)");
        }

        [Test]
        public void NativeCallbackDelegate_HasUnmanagedFunctionPointerAttribute()
        {
            var attr = delegateType.GetCustomAttribute<UnmanagedFunctionPointerAttribute>();
            Assert.IsNotNull(attr, "NativeCallbackDelegate should have UnmanagedFunctionPointer attribute");
            Assert.AreEqual(CallingConvention.Cdecl, attr.CallingConvention,
                "Calling convention should be Cdecl to match Rust FFI");
        }

        // ─── Singleton Tests ───────────────────────────────────────────────────

        [UnityTest]
        public IEnumerator Instance_CreatesDispatcherOnExistingGameObject()
        {
            // Initialize to create the NativeBrowserCallback GameObject
            NativeBrowser.Initialize();
            yield return null;

            receiverGameObject = GameObject.Find("NativeBrowserCallback");
            Assert.IsNotNull(receiverGameObject, "NativeBrowserCallback GameObject should exist");

            // Access the Instance property via reflection
            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(instanceProp, "Instance property should exist");

            var instance = instanceProp.GetValue(null);
            Assert.IsNotNull(instance, "Instance should return a non-null dispatcher");

            // Verify it was attached to the same GameObject as the receiver
            var dispatcherComponent = receiverGameObject.GetComponent(dispatcherType);
            Assert.IsNotNull(dispatcherComponent,
                "WindowsCallbackDispatcher should be attached to the NativeBrowserCallback GameObject");
        }

        [UnityTest]
        public IEnumerator Instance_CalledTwice_ReturnsSameInstance()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);

            var instance1 = instanceProp.GetValue(null);
            var instance2 = instanceProp.GetValue(null);

            Assert.AreSame(instance1, instance2,
                "Multiple Instance calls should return the same singleton");
        }

        // ─── Static Callback Method Tests ──────────────────────────────────────

        [Test]
        public void OnNativeCallback_IsStaticMethod()
        {
            var method = dispatcherType.GetMethod("OnNativeCallback",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(method, "OnNativeCallback static method should exist");
            Assert.IsTrue(method.IsStatic, "OnNativeCallback should be static");

            var parameters = method.GetParameters();
            Assert.AreEqual(2, parameters.Length);
            Assert.AreEqual(typeof(IntPtr), parameters[0].ParameterType);
            Assert.AreEqual(typeof(IntPtr), parameters[1].ParameterType);
        }

        [Test]
        public void OnNativeCallback_HasMonoPInvokeCallbackAttribute()
        {
            var method = dispatcherType.GetMethod("OnNativeCallback",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(method);

            var attr = method.GetCustomAttribute<AOT.MonoPInvokeCallbackAttribute>();
            Assert.IsNotNull(attr, "OnNativeCallback should have MonoPInvokeCallback attribute for IL2CPP");
        }

        [Test]
        public void OnNativeCallback_HasPreserveAttribute()
        {
            var method = dispatcherType.GetMethod("OnNativeCallback",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(method);

            var attr = method.GetCustomAttribute<UnityEngine.Scripting.PreserveAttribute>();
            Assert.IsNotNull(attr, "OnNativeCallback should have Preserve attribute to prevent stripping");
        }

        // ─── Callback Queue & Dispatch Tests ──────────────────────────────────

        [UnityTest]
        public IEnumerator CallbackQueue_OnPageStarted_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            // Ensure dispatcher is created
            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageStarted += (url) => { receivedUrl = url; };

            // Simulate native callback by enqueuing directly to the ConcurrentQueue
            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(queueField, "callbackQueue field should exist");

            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            Assert.IsNotNull(queue, "callbackQueue should be a ConcurrentQueue<string>");

            queue.Enqueue("OnPageStarted\n{\"url\":\"https://test.windows.com\"}");

            // Wait for Update() to drain the queue
            yield return null;
            yield return null;

            Assert.IsNotNull(receivedUrl, "OnPageStarted should have been dispatched");
            Assert.AreEqual("https://test.windows.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnPageFinished_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnPageFinished += (url) => { receivedUrl = url; };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnPageFinished\n{\"url\":\"https://finished.windows.com\"}");

            yield return null;
            yield return null;

            Assert.IsNotNull(receivedUrl, "OnPageFinished should have been dispatched");
            Assert.AreEqual("https://finished.windows.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnError_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedMessage = null;
            string receivedUrl = null;
            NativeBrowser.OnError += (msg, url) =>
            {
                receivedMessage = msg;
                receivedUrl = url;
            };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnError\n{\"type\":\"LOAD_ERROR\",\"message\":\"Network error\",\"url\":\"https://err.com\",\"requestId\":\"\"}");

            yield return null;
            yield return null;

            Assert.AreEqual("Network error", receivedMessage);
            Assert.AreEqual("https://err.com", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnPostMessage_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedMessage = null;
            NativeBrowser.OnPostMessage += (msg) => { receivedMessage = msg; };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnPostMessage\n{\"message\":\"Hello from JS\"}");

            yield return null;
            yield return null;

            Assert.AreEqual("Hello from JS", receivedMessage);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnJsResult_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedRequestId = null;
            string receivedResult = null;
            NativeBrowser.OnJsResult += (reqId, result) =>
            {
                receivedRequestId = reqId;
                receivedResult = result;
            };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnJsResult\n{\"requestId\":\"win-req-1\",\"result\":\"42\"}");

            yield return null;
            yield return null;

            Assert.AreEqual("win-req-1", receivedRequestId);
            Assert.AreEqual("42", receivedResult);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnDeepLink_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            string receivedUrl = null;
            NativeBrowser.OnDeepLink += (url) => { receivedUrl = url; };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnDeepLink\n{\"url\":\"myapp://windows/deeplink\"}");

            yield return null;
            yield return null;

            Assert.AreEqual("myapp://windows/deeplink", receivedUrl);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_OnClosed_DispatchesCorrectly()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            bool closedFired = false;
            NativeBrowser.OnClosed += () => { closedFired = true; };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;
            queue.Enqueue("OnClosed\n{}");

            yield return null;
            yield return null;

            Assert.IsTrue(closedFired, "OnClosed event should have been dispatched");
        }

        // ─── Queue Behavior Tests ──────────────────────────────────────────────

        [UnityTest]
        public IEnumerator CallbackQueue_MultipleEvents_ProcessedInOrder()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            var receivedUrls = new System.Collections.Generic.List<string>();
            NativeBrowser.OnPageStarted += (url) => { receivedUrls.Add(url); };

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;

            queue.Enqueue("OnPageStarted\n{\"url\":\"https://first.com\"}");
            queue.Enqueue("OnPageStarted\n{\"url\":\"https://second.com\"}");
            queue.Enqueue("OnPageStarted\n{\"url\":\"https://third.com\"}");

            yield return null;
            yield return null;

            Assert.AreEqual(3, receivedUrls.Count, "All three events should be dispatched");
            Assert.AreEqual("https://first.com", receivedUrls[0]);
            Assert.AreEqual("https://second.com", receivedUrls[1]);
            Assert.AreEqual("https://third.com", receivedUrls[2]);
        }

        [UnityTest]
        public IEnumerator CallbackQueue_UnknownEvent_LogsWarning()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;

            LogAssert.Expect(LogType.Warning,
                "WindowsCallbackDispatcher: Unknown event 'OnUnknownEvent'");

            queue.Enqueue("OnUnknownEvent\n{\"data\":\"test\"}");

            yield return null;
            yield return null;
        }

        [UnityTest]
        public IEnumerator CallbackQueue_MalformedPayload_NoSeparator_SkipsGracefully()
        {
            NativeBrowser.Initialize();
            yield return null;

            var instanceProp = dispatcherType.GetProperty("Instance",
                BindingFlags.Public | BindingFlags.Static);
            instanceProp.GetValue(null);
            yield return null;

            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            var queue = queueField.GetValue(null) as ConcurrentQueue<string>;

            // Enqueue malformed payload (no newline separator)
            queue.Enqueue("malformed-no-separator");

            // Should not throw — just skip
            yield return null;
            yield return null;

            // If we got here without exception, test passes
            Assert.Pass("Malformed payload without separator was skipped gracefully");
        }

        // ─── Cleanup Tests ─────────────────────────────────────────────────────

        [Test]
        public void WindowsCallbackDispatcher_HasOnDestroyCleanup()
        {
            var method = dispatcherType.GetMethod("OnDestroy",
                BindingFlags.NonPublic | BindingFlags.Instance);
            Assert.IsNotNull(method, "OnDestroy method should exist for cleanup");
        }

        [Test]
        public void WindowsCallbackDispatcher_HasUpdateMethod()
        {
            var method = dispatcherType.GetMethod("Update",
                BindingFlags.NonPublic | BindingFlags.Instance);
            Assert.IsNotNull(method, "Update method should exist for draining the callback queue");
        }

        // ─── Thread Safety Tests ───────────────────────────────────────────────

        [Test]
        public void CallbackQueue_IsConcurrentQueue()
        {
            var queueField = dispatcherType.GetField("callbackQueue",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(queueField, "callbackQueue field should exist");

            Assert.AreEqual(typeof(ConcurrentQueue<string>), queueField.FieldType,
                "callbackQueue should be ConcurrentQueue<string> for thread safety");
        }
    }
}
