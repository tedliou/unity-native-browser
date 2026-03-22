using System;
using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// PlayMode tests for NativeBrowserCallbackReceiver MonoBehaviour lifecycle.
    ///
    /// NOTE: NativeBrowserCallbackReceiver does NOT implement OnEnable/OnDisable
    /// subscription management — it is a passive receiver that exposes virtual
    /// callback methods invoked directly by UnitySendMessage (Android) or the
    /// Windows dispatcher. The static events on NativeBrowser are always active
    /// regardless of the receiver's enabled state.
    ///
    /// These tests document and verify the current behavior:
    /// - Events fire when the receiver is enabled (normal case)
    /// - Events still fire when the receiver is disabled, because the static
    ///   event pipeline is not gated on the MonoBehaviour's enabled state
    /// - Re-enabling the receiver does not change event behavior
    /// </summary>
    public class CallbackReceiverLifecycleTest
    {
        private GameObject receiverGo;
        private NativeBrowserCallbackReceiver receiver;

        [SetUp]
        public void SetUp()
        {
            var existing = GameObject.Find("NativeBrowserCallback");
            if (existing != null)
                UnityEngine.Object.DestroyImmediate(existing);

            receiverGo = new GameObject("TestReceiver");
            receiver = receiverGo.AddComponent<NativeBrowserCallbackReceiver>();
        }

        [TearDown]
        public void TearDown()
        {
            if (receiverGo != null)
                UnityEngine.Object.DestroyImmediate(receiverGo);
            receiverGo = null;
            receiver = null;

            var singleton = GameObject.Find("NativeBrowserCallback");
            if (singleton != null)
                UnityEngine.Object.DestroyImmediate(singleton);
        }

        [UnityTest]
        public IEnumerator Receiver_WhenEnabled_CanReceiveEvents()
        {
            // Arrange: receiver is enabled by default
            Assert.IsTrue(receiverGo.activeInHierarchy, "Receiver should be enabled initially");
            yield return null;

            string receivedUrl = null;
            Action<string> handler = (url) => receivedUrl = url;
            NativeBrowser.OnPageStarted += handler;

            try
            {
                // Act
                receiver.OnPageStarted("{\"url\":\"https://enabled.example.com\"}");
                yield return null;

                // Assert
                Assert.AreEqual("https://enabled.example.com", receivedUrl,
                    "Event should fire when receiver is enabled");
            }
            finally
            {
                NativeBrowser.OnPageStarted -= handler;
            }
        }

        [UnityTest]
        public IEnumerator Receiver_WhenDisabled_StaticEventStillFires()
        {
            // Arrange: disable the receiver
            receiverGo.SetActive(false);
            Assert.IsFalse(receiverGo.activeInHierarchy, "Receiver should be disabled");
            yield return null;

            string receivedUrl = null;
            Action<string> handler = (url) => receivedUrl = url;
            NativeBrowser.OnPageStarted += handler;

            try
            {
                // Act: call the method directly (simulating UnitySendMessage which bypasses enabled check)
                receiver.OnPageStarted("{\"url\":\"https://disabled.example.com\"}");
                yield return null;

                // Assert: NativeBrowserCallbackReceiver does not gate on enabled state —
                // the static event fires regardless. This documents the current behavior.
                Assert.AreEqual("https://disabled.example.com", receivedUrl,
                    "Static event fires even when receiver GameObject is disabled, " +
                    "because NativeBrowserCallbackReceiver has no OnEnable/OnDisable subscription management");
            }
            finally
            {
                NativeBrowser.OnPageStarted -= handler;
            }
        }

        [UnityTest]
        public IEnumerator Receiver_ReEnabled_ContinuesToReceiveEvents()
        {
            // Arrange: disable then re-enable
            receiverGo.SetActive(false);
            yield return null;
            receiverGo.SetActive(true);
            Assert.IsTrue(receiverGo.activeInHierarchy, "Receiver should be re-enabled");
            yield return null;

            string receivedUrl = null;
            Action<string> handler = (url) => receivedUrl = url;
            NativeBrowser.OnPageFinished += handler;

            try
            {
                // Act
                receiver.OnPageFinished("{\"url\":\"https://reenabled.example.com\"}");
                yield return null;

                // Assert
                Assert.AreEqual("https://reenabled.example.com", receivedUrl,
                    "Event should fire after receiver is re-enabled");
            }
            finally
            {
                NativeBrowser.OnPageFinished -= handler;
            }
        }

        [UnityTest]
        public IEnumerator Receiver_Destroyed_DoesNotAffectStaticEvents()
        {
            // Arrange: subscribe before destroying
            string receivedUrl = null;
            Action<string> handler = (url) => receivedUrl = url;
            NativeBrowser.OnDeepLink += handler;

            try
            {
                // Act: destroy the receiver, then fire via a new receiver
                UnityEngine.Object.DestroyImmediate(receiverGo);
                receiverGo = null;
                receiver = null;
                yield return null;

                // Create a fresh receiver and fire
                var newGo = new GameObject("NewReceiver");
                var newRecv = newGo.AddComponent<NativeBrowserCallbackReceiver>();
                newRecv.OnDeepLink("{\"url\":\"myapp://after-destroy\"}");
                yield return null;

                Assert.AreEqual("myapp://after-destroy", receivedUrl,
                    "Static event should still fire from a new receiver after the original was destroyed");

                UnityEngine.Object.DestroyImmediate(newGo);
            }
            finally
            {
                NativeBrowser.OnDeepLink -= handler;
            }
        }
    }
}
