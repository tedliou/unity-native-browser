using System;
using System.Collections.Generic;
using NUnit.Framework;
using TedLiou.NativeBrowser;
using UnityEngine;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for BrowserConfig validation.
    /// Tests default values, property setters, JSON serialization without Android JNI.
    /// </summary>
    [TestFixture]
    public class BrowserConfigTest
    {
        [Test]
        public void BrowserConfig_HasCorrectDefaultValues()
        {
            // Verify default values when constructing with just URL
            var config = new BrowserConfig("https://example.com");
            
            Assert.AreEqual("https://example.com", config.url);
            Assert.AreEqual(1.0f, config.width, "Default width should be 1.0");
            Assert.AreEqual(1.0f, config.height, "Default height should be 1.0");
            Assert.AreEqual(Alignment.CENTER, config.alignment, "Default alignment should be CENTER");
            Assert.AreEqual(false, config.closeOnTapOutside, "Default closeOnTapOutside should be false");
            Assert.AreEqual(true, config.closeOnDeepLink, "Default closeOnDeepLink should be true");
            Assert.AreEqual(true, config.enableJavaScript, "Default enableJavaScript should be true");
            Assert.AreEqual("", config.userAgent, "Default userAgent should be empty string");
            Assert.IsNotNull(config.deepLinkPatterns, "deepLinkPatterns should not be null");
            Assert.AreEqual(0, config.deepLinkPatterns.Count, "deepLinkPatterns should be empty by default");
        }

        [Test]
        public void BrowserConfig_CanSetAllProperties()
        {
            // Verify all properties can be set correctly
            var config = new BrowserConfig("https://test.com")
            {
                width = 0.75f,
                height = 0.5f,
                alignment = Alignment.TOP_LEFT,
                closeOnTapOutside = true,
                closeOnDeepLink = false,
                enableJavaScript = false,
                userAgent = "CustomAgent/1.0"
            };
            config.deepLinkPatterns.Add("myapp://");
            
            Assert.AreEqual("https://test.com", config.url);
            Assert.AreEqual(0.75f, config.width);
            Assert.AreEqual(0.5f, config.height);
            Assert.AreEqual(Alignment.TOP_LEFT, config.alignment);
            Assert.AreEqual(true, config.closeOnTapOutside);
            Assert.AreEqual(false, config.closeOnDeepLink);
            Assert.AreEqual(false, config.enableJavaScript);
            Assert.AreEqual("CustomAgent/1.0", config.userAgent);
            Assert.AreEqual(1, config.deepLinkPatterns.Count);
            Assert.AreEqual("myapp://", config.deepLinkPatterns[0]);
        }

        [Test]
        public void BrowserConfig_WidthHeightValueRanges()
        {
            // Test valid ranges for width and height (0.0-1.0)
            var config = new BrowserConfig("https://example.com");
            
            // Test minimum values
            config.width = 0.0f;
            config.height = 0.0f;
            Assert.AreEqual(0.0f, config.width);
            Assert.AreEqual(0.0f, config.height);
            
            // Test maximum values
            config.width = 1.0f;
            config.height = 1.0f;
            Assert.AreEqual(1.0f, config.width);
            Assert.AreEqual(1.0f, config.height);
            
            // Test mid-range values
            config.width = 0.5f;
            config.height = 0.8f;
            Assert.AreEqual(0.5f, config.width);
            Assert.AreEqual(0.8f, config.height);
            
            // Note: Values outside 0-1 range are allowed by the API (no enforcement in C# layer)
            // Android side may clamp or handle these differently
            config.width = 1.5f;
            config.height = -0.1f;
            Assert.AreEqual(1.5f, config.width, "API allows values > 1.0");
            Assert.AreEqual(-0.1f, config.height, "API allows negative values");
        }

        [Test]
        public void BrowserConfig_ToJsonOutputFormat()
        {
            // Verify ToJson() produces valid JSON with expected structure
            var config = new BrowserConfig("https://example.com")
            {
                width = 0.8f,
                height = 0.6f,
                alignment = Alignment.BOTTOM_RIGHT,
                closeOnTapOutside = true,
                enableJavaScript = true,
                userAgent = "TestAgent"
            };
            config.deepLinkPatterns.Add("https://deep.link/*");
            
            string json = config.ToJson();
            
            // Verify JSON is not null or empty
            Assert.IsNotNull(json);
            Assert.IsNotEmpty(json);
            
            // Verify JSON contains expected fields (basic validation)
            Assert.That(json, Does.Contain("\"url\""));
            Assert.That(json, Does.Contain("https://example.com"));
            Assert.That(json, Does.Contain("\"width\""));
            Assert.That(json, Does.Contain("0.8"));
            Assert.That(json, Does.Contain("\"height\""));
            Assert.That(json, Does.Contain("0.6"));
            Assert.That(json, Does.Contain("\"alignment\""));
            Assert.That(json, Does.Contain("BOTTOM_RIGHT"));
            Assert.That(json, Does.Contain("\"closeOnTapOutside\""));
            Assert.That(json, Does.Contain("\"enableJavaScript\""));
            Assert.That(json, Does.Contain("\"userAgent\""));
            Assert.That(json, Does.Contain("TestAgent"));
            Assert.That(json, Does.Contain("\"deepLinkPatterns\""));
            Assert.That(json, Does.Contain("https://deep.link/*"));
        }

        [Test]
        public void BrowserConfig_ToJsonWithDefaultValues()
        {
            // Verify JSON serialization with default values
            var config = new BrowserConfig("https://default.com");
            string json = config.ToJson();
            
            Assert.IsNotNull(json);
            Assert.That(json, Does.Contain("\"url\":\"https://default.com\""));
            Assert.That(json, Does.Contain("\"width\":1"));
            Assert.That(json, Does.Contain("\"height\":1"));
            Assert.That(json, Does.Contain("\"alignment\":\"CENTER\""));
            Assert.That(json, Does.Contain("\"closeOnTapOutside\":false"));
            Assert.That(json, Does.Contain("\"closeOnDeepLink\":true"));
            Assert.That(json, Does.Contain("\"enableJavaScript\":true"));
            Assert.That(json, Does.Contain("\"userAgent\":\"\""));
            Assert.That(json, Does.Contain("\"deepLinkPatterns\":[]"));
        }

        [Test]
        public void BrowserConfig_AlignmentStringMapping()
        {
            // Test that all Alignment enum values map to expected string values in JSON
            var testCases = new Dictionary<Alignment, string>
            {
                { Alignment.CENTER, "CENTER" },
                { Alignment.LEFT, "LEFT" },
                { Alignment.RIGHT, "RIGHT" },
                { Alignment.TOP, "TOP" },
                { Alignment.BOTTOM, "BOTTOM" },
                { Alignment.TOP_LEFT, "TOP_LEFT" },
                { Alignment.TOP_RIGHT, "TOP_RIGHT" },
                { Alignment.BOTTOM_LEFT, "BOTTOM_LEFT" },
                { Alignment.BOTTOM_RIGHT, "BOTTOM_RIGHT" }
            };
            
            foreach (var testCase in testCases)
            {
                var config = new BrowserConfig("https://test.com")
                {
                    alignment = testCase.Key
                };
                
                string json = config.ToJson();
                Assert.That(json, Does.Contain($"\"alignment\":\"{testCase.Value}\""),
                    $"Alignment.{testCase.Key} should map to \"{testCase.Value}\" in JSON");
            }
        }

        [Test]
        public void BrowserConfig_DeepLinkPatternsHandling()
        {
            // Test deep link patterns list handling
            var config = new BrowserConfig("https://example.com");
            
            // Test empty list
            Assert.IsNotNull(config.deepLinkPatterns);
            Assert.AreEqual(0, config.deepLinkPatterns.Count);
            
            // Add single pattern
            config.deepLinkPatterns.Add("myapp://open");
            Assert.AreEqual(1, config.deepLinkPatterns.Count);
            Assert.AreEqual("myapp://open", config.deepLinkPatterns[0]);
            
            // Add multiple patterns
            config.deepLinkPatterns.Add("https://example.com/callback");
            config.deepLinkPatterns.Add("custom://action/*");
            Assert.AreEqual(3, config.deepLinkPatterns.Count);
            
            // Verify JSON contains all patterns
            string json = config.ToJson();
            Assert.That(json, Does.Contain("myapp://open"));
            Assert.That(json, Does.Contain("https://example.com/callback"));
            Assert.That(json, Does.Contain("custom://action/*"));
        }

        [Test]
        public void BrowserConfig_UrlHandling()
        {
            // Test various URL formats
            var testUrls = new[]
            {
                "https://example.com",
                "http://test.com/path?query=value",
                "https://example.com:8080/path#fragment",
                "file:///android_asset/index.html",
                "about:blank",
                ""
            };
            
            foreach (var testUrl in testUrls)
            {
                var config = new BrowserConfig(testUrl);
                Assert.AreEqual(testUrl, config.url, $"URL '{testUrl}' should be stored correctly");
                
                string json = config.ToJson();
                Assert.IsNotNull(json);
                Assert.That(json, Does.Contain("\"url\""));
            }
        }

        [Test]
        public void BrowserConfig_BlankUrl_IsAccepted()
        {
            // NOTE: Unlike the Android Kotlin BrowserConfig which throws IllegalArgumentException
            // for blank URLs, the C# BrowserConfig is a plain data class with no validation.
            // URL validation is handled by the Android layer when Open() is called.
            // This test documents that behavior difference explicitly.

            // Empty string URL — accepted by C# layer
            var configEmpty = new BrowserConfig("");
            Assert.AreEqual("", configEmpty.url, "Empty URL should be accepted by C# BrowserConfig");
            Assert.DoesNotThrow(() => configEmpty.ToJson(), "ToJson() should not throw for empty URL");

            // Whitespace-only URL — accepted by C# layer
            var configWhitespace = new BrowserConfig("   ");
            Assert.AreEqual("   ", configWhitespace.url, "Whitespace URL should be accepted by C# BrowserConfig");
            Assert.DoesNotThrow(() => configWhitespace.ToJson(), "ToJson() should not throw for whitespace URL");

            // Null URL — accepted by C# layer (Android layer will handle null)
            var configNull = new BrowserConfig(null);
            Assert.IsNull(configNull.url, "Null URL should be accepted by C# BrowserConfig");
        }

        [Test]
        public void BrowserConfig_UserAgentHandling()
        {
            // Test user agent string handling
            var config = new BrowserConfig("https://example.com");
            
            // Default empty user agent
            Assert.AreEqual("", config.userAgent);
            
            // Set custom user agent
            config.userAgent = "Mozilla/5.0 (Custom Agent)";
            Assert.AreEqual("Mozilla/5.0 (Custom Agent)", config.userAgent);
            
            // Verify in JSON
            string json = config.ToJson();
            Assert.That(json, Does.Contain("Mozilla/5.0 (Custom Agent)"));
            
            // Empty user agent in JSON
            config.userAgent = "";
            json = config.ToJson();
            Assert.That(json, Does.Contain("\"userAgent\":\"\""));
        }

        [Test]
        public void BrowserConfig_BooleanFlagsHandling()
        {
            // Test all boolean flags
            var config = new BrowserConfig("https://example.com");
            
            // Test closeOnTapOutside
            config.closeOnTapOutside = true;
            Assert.IsTrue(config.closeOnTapOutside);
            string json1 = config.ToJson();
            Assert.That(json1, Does.Contain("\"closeOnTapOutside\":true"));
            
            config.closeOnTapOutside = false;
            Assert.IsFalse(config.closeOnTapOutside);
            string json2 = config.ToJson();
            Assert.That(json2, Does.Contain("\"closeOnTapOutside\":false"));
            
            // Test closeOnDeepLink
            config.closeOnDeepLink = true;
            Assert.IsTrue(config.closeOnDeepLink);
            string json3 = config.ToJson();
            Assert.That(json3, Does.Contain("\"closeOnDeepLink\":true"));
            
            config.closeOnDeepLink = false;
            Assert.IsFalse(config.closeOnDeepLink);
            string json4 = config.ToJson();
            Assert.That(json4, Does.Contain("\"closeOnDeepLink\":false"));
            
            // Test enableJavaScript
            config.enableJavaScript = true;
            Assert.IsTrue(config.enableJavaScript);
            string json5 = config.ToJson();
            Assert.That(json5, Does.Contain("\"enableJavaScript\":true"));
            
            config.enableJavaScript = false;
            Assert.IsFalse(config.enableJavaScript);
            string json6 = config.ToJson();
            Assert.That(json6, Does.Contain("\"enableJavaScript\":false"));
        }
    }
}
