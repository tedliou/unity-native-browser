using System;
using System.Reflection;
using NUnit.Framework;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for NativeBrowser API surface validation.
    /// Tests public API methods, events, and structure without Android JNI calls.
    /// </summary>
    [TestFixture]
    public class NativeBrowserApiTest
    {
        [Test]
        public void NativeBrowser_HasExpectedStaticMethods()
        {
            // Verify NativeBrowser is a static class with expected public methods
            Type type = typeof(NativeBrowser);
            
            // Verify core methods exist
            Assert.IsNotNull(type.GetMethod("Initialize", BindingFlags.Public | BindingFlags.Static));
            Assert.IsNotNull(type.GetMethod("Open", BindingFlags.Public | BindingFlags.Static));
            Assert.IsNotNull(type.GetMethod("Close", BindingFlags.Public | BindingFlags.Static));
            Assert.IsNotNull(type.GetMethod("Refresh", BindingFlags.Public | BindingFlags.Static));
            Assert.IsNotNull(type.GetMethod("ExecuteJavaScript", BindingFlags.Public | BindingFlags.Static));
            Assert.IsNotNull(type.GetMethod("InjectJavaScript", BindingFlags.Public | BindingFlags.Static));
        }

        [Test]
        public void NativeBrowser_HasIsOpenProperty()
        {
            // Verify IsOpen property exists and is boolean
            Type type = typeof(NativeBrowser);
            PropertyInfo isOpenProp = type.GetProperty("IsOpen", BindingFlags.Public | BindingFlags.Static);
            
            Assert.IsNotNull(isOpenProp, "IsOpen property should exist");
            Assert.AreEqual(typeof(bool), isOpenProp.PropertyType, "IsOpen should return bool");
            Assert.IsNotNull(isOpenProp.GetMethod, "IsOpen should have a getter");
        }

        [Test]
        public void NativeBrowser_HasExpectedEvents()
        {
            // Verify all callback events exist with correct signatures
            Type type = typeof(NativeBrowser);
            
            // OnPageStarted: Action<string>
            EventInfo onPageStarted = type.GetEvent("OnPageStarted", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onPageStarted);
            Assert.AreEqual(typeof(Action<string>), onPageStarted.EventHandlerType);
            
            // OnPageFinished: Action<string>
            EventInfo onPageFinished = type.GetEvent("OnPageFinished", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onPageFinished);
            Assert.AreEqual(typeof(Action<string>), onPageFinished.EventHandlerType);
            
            // OnError: Action<string, string>
            EventInfo onError = type.GetEvent("OnError", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onError);
            Assert.AreEqual(typeof(Action<string, string>), onError.EventHandlerType);
            
            // OnPostMessage: Action<string>
            EventInfo onPostMessage = type.GetEvent("OnPostMessage", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onPostMessage);
            Assert.AreEqual(typeof(Action<string>), onPostMessage.EventHandlerType);
            
            // OnJsResult: Action<string, string>
            EventInfo onJsResult = type.GetEvent("OnJsResult", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onJsResult);
            Assert.AreEqual(typeof(Action<string, string>), onJsResult.EventHandlerType);
            
            // OnDeepLink: Action<string>
            EventInfo onDeepLink = type.GetEvent("OnDeepLink", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onDeepLink);
            Assert.AreEqual(typeof(Action<string>), onDeepLink.EventHandlerType);
            
            // OnClosed: Action
            EventInfo onClosed = type.GetEvent("OnClosed", BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(onClosed);
            Assert.AreEqual(typeof(Action), onClosed.EventHandlerType);
        }

        [Test]
        public void BrowserType_HasExpectedValues()
        {
            // Verify BrowserType enum has all expected values
            Assert.IsTrue(Enum.IsDefined(typeof(BrowserType), "WebView"));
            Assert.IsTrue(Enum.IsDefined(typeof(BrowserType), "CustomTab"));
            Assert.IsTrue(Enum.IsDefined(typeof(BrowserType), "SystemBrowser"));
            
            // Verify enum values
            Assert.AreEqual(BrowserType.WebView, Enum.Parse(typeof(BrowserType), "WebView"));
            Assert.AreEqual(BrowserType.CustomTab, Enum.Parse(typeof(BrowserType), "CustomTab"));
            Assert.AreEqual(BrowserType.SystemBrowser, Enum.Parse(typeof(BrowserType), "SystemBrowser"));
        }

        [Test]
        public void Alignment_HasExpectedValues()
        {
            // Verify Alignment enum has all expected values
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "CENTER"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "LEFT"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "RIGHT"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "TOP"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "BOTTOM"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "TOP_LEFT"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "TOP_RIGHT"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "BOTTOM_LEFT"));
            Assert.IsTrue(Enum.IsDefined(typeof(Alignment), "BOTTOM_RIGHT"));
        }

        [Test]
        public void OpenMethod_HasCorrectSignature()
        {
            // Verify Open method signature: void Open(BrowserType type, BrowserConfig config)
            Type type = typeof(NativeBrowser);
            MethodInfo openMethod = type.GetMethod("Open", BindingFlags.Public | BindingFlags.Static);
            
            Assert.IsNotNull(openMethod);
            Assert.AreEqual(typeof(void), openMethod.ReturnType);
            
            ParameterInfo[] parameters = openMethod.GetParameters();
            Assert.AreEqual(2, parameters.Length);
            Assert.AreEqual(typeof(BrowserType), parameters[0].ParameterType);
            Assert.AreEqual(typeof(BrowserConfig), parameters[1].ParameterType);
        }

        [Test]
        public void ExecuteJavaScriptMethod_HasCorrectSignature()
        {
            // Verify ExecuteJavaScript method signature: void ExecuteJavaScript(string script, string requestId = null)
            Type type = typeof(NativeBrowser);
            MethodInfo executeMethod = type.GetMethod("ExecuteJavaScript", BindingFlags.Public | BindingFlags.Static);
            
            Assert.IsNotNull(executeMethod);
            Assert.AreEqual(typeof(void), executeMethod.ReturnType);
            
            ParameterInfo[] parameters = executeMethod.GetParameters();
            Assert.AreEqual(2, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("script", parameters[0].Name);
            Assert.AreEqual(typeof(string), parameters[1].ParameterType);
            Assert.AreEqual("requestId", parameters[1].Name);
            Assert.IsTrue(parameters[1].IsOptional, "requestId should be optional");
        }

        [Test]
        public void InjectJavaScriptMethod_HasCorrectSignature()
        {
            // Verify InjectJavaScript method signature: void InjectJavaScript(string script)
            Type type = typeof(NativeBrowser);
            MethodInfo injectMethod = type.GetMethod("InjectJavaScript", BindingFlags.Public | BindingFlags.Static);
            
            Assert.IsNotNull(injectMethod);
            Assert.AreEqual(typeof(void), injectMethod.ReturnType);
            
            ParameterInfo[] parameters = injectMethod.GetParameters();
            Assert.AreEqual(1, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("script", parameters[0].Name);
        }

        [Test]
        public void BrowserEventClasses_AreSerializable()
        {
            // Verify all event classes have [Serializable] attribute for JSON serialization
            Assert.IsTrue(typeof(PageStartedEvent).IsSerializable || 
                          typeof(PageStartedEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "PageStartedEvent should be serializable");
            
            Assert.IsTrue(typeof(PageFinishedEvent).IsSerializable || 
                          typeof(PageFinishedEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "PageFinishedEvent should be serializable");
            
            Assert.IsTrue(typeof(BrowserErrorEvent).IsSerializable || 
                          typeof(BrowserErrorEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "BrowserErrorEvent should be serializable");
            
            Assert.IsTrue(typeof(PostMessageEvent).IsSerializable || 
                          typeof(PostMessageEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "PostMessageEvent should be serializable");
            
            Assert.IsTrue(typeof(JsResultEvent).IsSerializable || 
                          typeof(JsResultEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "JsResultEvent should be serializable");
            
            Assert.IsTrue(typeof(DeepLinkEvent).IsSerializable || 
                          typeof(DeepLinkEvent).GetCustomAttribute<SerializableAttribute>() != null,
                          "DeepLinkEvent should be serializable");
        }

        [Test]
        public void BrowserEventClasses_HaveExpectedProperties()
        {
            // PageStartedEvent should have 'url' property
            PropertyInfo urlProp1 = typeof(PageStartedEvent).GetField("url");
            Assert.IsNotNull(urlProp1);
            
            // PageFinishedEvent should have 'url' property
            PropertyInfo urlProp2 = typeof(PageFinishedEvent).GetField("url");
            Assert.IsNotNull(urlProp2);
            
            // BrowserErrorEvent should have type, message, url, requestId
            Assert.IsNotNull(typeof(BrowserErrorEvent).GetField("type"));
            Assert.IsNotNull(typeof(BrowserErrorEvent).GetField("message"));
            Assert.IsNotNull(typeof(BrowserErrorEvent).GetField("url"));
            Assert.IsNotNull(typeof(BrowserErrorEvent).GetField("requestId"));
            
            // PostMessageEvent should have 'message' property
            Assert.IsNotNull(typeof(PostMessageEvent).GetField("message"));
            
            // JsResultEvent should have requestId and result
            Assert.IsNotNull(typeof(JsResultEvent).GetField("requestId"));
            Assert.IsNotNull(typeof(JsResultEvent).GetField("result"));
            
            // DeepLinkEvent should have 'url' property
            Assert.IsNotNull(typeof(DeepLinkEvent).GetField("url"));
        }
    }
}
