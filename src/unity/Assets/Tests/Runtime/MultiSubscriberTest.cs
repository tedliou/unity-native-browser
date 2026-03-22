using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// PlayMode tests verifying that multiple NativeBrowserCallbackReceiver instances
    /// all receive static events when a callback is triggered on any one of them.
    /// Since NativeBrowser events are static, all subscribers receive every event
    /// regardless of which receiver instance invokes the callback method.
    /// </summary>
    public class MultiSubscriberTest
    {
        private GameObject go1;
        private GameObject go2;
        private GameObject go3;

        [SetUp]
        public void SetUp()
        {
            // Clean up any leftover singleton from previous tests
            var existing = GameObject.Find("NativeBrowserCallback");
            if (existing != null)
                UnityEngine.Object.DestroyImmediate(existing);
        }

        [TearDown]
        public void TearDown()
        {
            if (go1 != null) UnityEngine.Object.DestroyImmediate(go1);
            if (go2 != null) UnityEngine.Object.DestroyImmediate(go2);
            if (go3 != null) UnityEngine.Object.DestroyImmediate(go3);
            go1 = go2 = go3 = null;

            var singleton = GameObject.Find("NativeBrowserCallback");
            if (singleton != null)
                UnityEngine.Object.DestroyImmediate(singleton);
        }

        [UnityTest]
        public IEnumerator MultipleReceivers_AllReceiveOnPageStarted_WhenOneInvokesCallback()
        {
            // Arrange: create 3 separate GameObjects each with a receiver component
            go1 = new GameObject("Receiver1");
            go2 = new GameObject("Receiver2");
            go3 = new GameObject("Receiver3");

            var recv1 = go1.AddComponent<NativeBrowserCallbackReceiver>();
            var recv2 = go2.AddComponent<NativeBrowserCallbackReceiver>();
            var recv3 = go3.AddComponent<NativeBrowserCallbackReceiver>();

            yield return null;

            // Subscribe to the static event from each "subscriber"
            int subscriber1Count = 0;
            int subscriber2Count = 0;
            int subscriber3Count = 0;

            Action<string> handler1 = (url) => subscriber1Count++;
            Action<string> handler2 = (url) => subscriber2Count++;
            Action<string> handler3 = (url) => subscriber3Count++;

            NativeBrowser.OnPageStarted += handler1;
            NativeBrowser.OnPageStarted += handler2;
            NativeBrowser.OnPageStarted += handler3;

            try
            {
                // Act: trigger callback on only one receiver
                recv1.OnPageStarted("{\"url\":\"https://multi-subscriber.example.com\"}");
                yield return null;

                // Assert: all 3 subscribers received the event (static event broadcasts to all)
                Assert.AreEqual(1, subscriber1Count, "Subscriber 1 should have received the event");
                Assert.AreEqual(1, subscriber2Count, "Subscriber 2 should have received the event");
                Assert.AreEqual(1, subscriber3Count, "Subscriber 3 should have received the event");
            }
            finally
            {
                NativeBrowser.OnPageStarted -= handler1;
                NativeBrowser.OnPageStarted -= handler2;
                NativeBrowser.OnPageStarted -= handler3;
            }
        }

        [UnityTest]
        public IEnumerator MultipleReceivers_EventFiredOnce_PerCallbackInvocation()
        {
            // Arrange: 3 receivers, 1 subscriber counting total fires
            go1 = new GameObject("Receiver1");
            go2 = new GameObject("Receiver2");
            go3 = new GameObject("Receiver3");

            var recv1 = go1.AddComponent<NativeBrowserCallbackReceiver>();
            var recv2 = go2.AddComponent<NativeBrowserCallbackReceiver>();
            var recv3 = go3.AddComponent<NativeBrowserCallbackReceiver>();

            yield return null;

            int totalFires = 0;
            Action<string> handler = (url) => totalFires++;
            NativeBrowser.OnPageFinished += handler;

            try
            {
                // Act: each receiver fires the callback once
                recv1.OnPageFinished("{\"url\":\"https://page1.example.com\"}");
                recv2.OnPageFinished("{\"url\":\"https://page2.example.com\"}");
                recv3.OnPageFinished("{\"url\":\"https://page3.example.com\"}");
                yield return null;

                // Assert: event fired once per callback invocation (3 total)
                Assert.AreEqual(3, totalFires,
                    "Static event should fire once per callback invocation across all receivers");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= handler;
            }
        }

        [UnityTest]
        public IEnumerator MultipleReceivers_AllReceiveCorrectUrl()
        {
            // Arrange
            go1 = new GameObject("Receiver1");
            go2 = new GameObject("Receiver2");
            go3 = new GameObject("Receiver3");

            var recv1 = go1.AddComponent<NativeBrowserCallbackReceiver>();
            recv2 = go2; // unused receiver
            recv3 = go3; // unused receiver
            go2.AddComponent<NativeBrowserCallbackReceiver>();
            go3.AddComponent<NativeBrowserCallbackReceiver>();

            yield return null;

            string url1 = null;
            string url2 = null;
            string url3 = null;

            Action<string> handler1 = (url) => url1 = url;
            Action<string> handler2 = (url) => url2 = url;
            Action<string> handler3 = (url) => url3 = url;

            NativeBrowser.OnDeepLink += handler1;
            NativeBrowser.OnDeepLink += handler2;
            NativeBrowser.OnDeepLink += handler3;

            try
            {
                recv1.OnDeepLink("{\"url\":\"myapp://test/path\"}");
                yield return null;

                Assert.AreEqual("myapp://test/path", url1, "Subscriber 1 should receive correct URL");
                Assert.AreEqual("myapp://test/path", url2, "Subscriber 2 should receive correct URL");
                Assert.AreEqual("myapp://test/path", url3, "Subscriber 3 should receive correct URL");
            }
            finally
            {
                NativeBrowser.OnDeepLink -= handler1;
                NativeBrowser.OnDeepLink -= handler2;
                NativeBrowser.OnDeepLink -= handler3;
            }
        }
    }
}
