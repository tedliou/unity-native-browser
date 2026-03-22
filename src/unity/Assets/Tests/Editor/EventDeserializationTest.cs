using NUnit.Framework;
using UnityEngine;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for JSON deserialization of all browser event classes.
    /// Verifies that JsonUtility.FromJson correctly maps JSON fields to C# properties.
    /// </summary>
    [TestFixture]
    public class EventDeserializationTest
    {
        [Test]
        public void PageStartedEvent_DeserializesUrl()
        {
            var evt = JsonUtility.FromJson<PageStartedEvent>("{\"url\":\"https://test.com\"}");

            Assert.IsNotNull(evt);
            Assert.AreEqual("https://test.com", evt.url);
        }

        [Test]
        public void PageFinishedEvent_DeserializesUrl()
        {
            var evt = JsonUtility.FromJson<PageFinishedEvent>("{\"url\":\"https://test.com\"}");

            Assert.IsNotNull(evt);
            Assert.AreEqual("https://test.com", evt.url);
        }

        [Test]
        public void BrowserErrorEvent_DeserializesAllFields()
        {
            var json = "{\"type\":\"PageLoadException\",\"message\":\"err\",\"url\":\"https://test.com\",\"requestId\":\"\"}";
            var evt = JsonUtility.FromJson<BrowserErrorEvent>(json);

            Assert.IsNotNull(evt);
            Assert.AreEqual("PageLoadException", evt.type);
            Assert.AreEqual("err", evt.message);
            Assert.AreEqual("https://test.com", evt.url);
        }

        [Test]
        public void PostMessageEvent_DeserializesMessage()
        {
            var evt = JsonUtility.FromJson<PostMessageEvent>("{\"message\":\"hello\"}");

            Assert.IsNotNull(evt);
            Assert.AreEqual("hello", evt.message);
        }

        [Test]
        public void JsResultEvent_DeserializesRequestIdAndResult()
        {
            var evt = JsonUtility.FromJson<JsResultEvent>("{\"requestId\":\"req-1\",\"result\":\"42\"}");

            Assert.IsNotNull(evt);
            Assert.AreEqual("req-1", evt.requestId);
            Assert.AreEqual("42", evt.result);
        }

        [Test]
        public void DeepLinkEvent_DeserializesUrl()
        {
            var evt = JsonUtility.FromJson<DeepLinkEvent>("{\"url\":\"myapp://deep\"}");

            Assert.IsNotNull(evt);
            Assert.AreEqual("myapp://deep", evt.url);
        }

        [Test]
        public void PageStartedEvent_EmptyJson_ReturnsDefaultValues()
        {
            // JsonUtility returns an object with default (null/empty) fields for missing keys
            var evt = JsonUtility.FromJson<PageStartedEvent>("{}");

            Assert.IsNotNull(evt);
            Assert.IsNull(evt.url);
        }

        [Test]
        public void BrowserErrorEvent_MissingOptionalFields_DoesNotThrow()
        {
            // requestId is optional — missing it should not throw
            var json = "{\"type\":\"NetworkError\",\"message\":\"timeout\",\"url\":\"https://example.com\"}";
            BrowserErrorEvent evt = null;
            Assert.DoesNotThrow(() => evt = JsonUtility.FromJson<BrowserErrorEvent>(json));
            Assert.IsNotNull(evt);
            Assert.AreEqual("NetworkError", evt.type);
        }
    }
}
