using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests verifying that NativeBrowser log messages contain the expected "NativeBrowser:" prefix.
    ///
    /// NOTE: Unlike the Android BrowserLogger which uses the "[NativeBrowser]" tag format,
    /// the C# layer uses Unity's Debug.LogWarning with a "NativeBrowser:" prefix.
    /// There is no separate BrowserLogger class in C# — logging is inline in EditorBridge.
    /// These tests use LogAssert to verify warning messages are emitted with the correct prefix.
    /// </summary>
    [TestFixture]
    public class BrowserLogTest
    {
        [Test]
        public void Open_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.Open(BrowserType.WebView, new BrowserConfig("https://example.com"));
        }

        [Test]
        public void Close_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.Close();
        }

        [Test]
        public void Refresh_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.Refresh();
        }

        [Test]
        public void ExecuteJavaScript_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.ExecuteJavaScript("document.title");
        }

        [Test]
        public void InjectJavaScript_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.InjectJavaScript("console.log('test');");
        }

        [Test]
        public void SendPostMessage_OnNonAndroidPlatform_LogsWarningWithNativeBrowserPrefix()
        {
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.SendPostMessage("hello");
        }

        [Test]
        public void Initialize_OnNonAndroidPlatform_DoesNotLogWarnings()
        {
            // Initialize() is a no-op in EditorBridge — it should produce no warnings
            LogAssert.NoUnexpectedReceived();
            NativeBrowser.Initialize();
        }

        [Test]
        public void Open_WithNullConfig_LogsWarningWithNativeBrowserPrefix()
        {
            // NativeBrowser.Open() guards against null config with its own warning
            LogAssert.Expect(LogType.Warning, new System.Text.RegularExpressions.Regex("^NativeBrowser:"));
            NativeBrowser.Open(BrowserType.WebView, null);
        }
    }
}
