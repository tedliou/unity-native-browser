using System;
using System.Reflection;
using System.Runtime.InteropServices;
using NUnit.Framework;
using TedLiou.NativeBrowser;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for the WebGLBridge implementation.
    /// Validates class structure, IPlatformBridge conformance, and P/Invoke declarations
    /// without requiring the actual WebGL runtime. Mirrors WindowsBridgeApiTest conventions.
    /// </summary>
    [TestFixture]
    public class WebGLBridgeApiTest
    {
        private Type bridgeType;

        [SetUp]
        public void Setup()
        {
            var assembly = typeof(NativeBrowser).Assembly;
            bridgeType = assembly.GetType("TedLiou.NativeBrowser.Internal.WebGLBridge");
            Assert.IsNotNull(bridgeType, "WebGLBridge type should exist in the assembly");
        }

        // ─── Type Structure Tests ──────────────────────────────────────────────

        [Test]
        public void WebGLBridge_IsInternalClass()
        {
            Assert.IsFalse(bridgeType.IsPublic, "WebGLBridge should be internal, not public");
            Assert.IsTrue(bridgeType.IsClass, "WebGLBridge should be a class");
            Assert.IsFalse(bridgeType.IsAbstract, "WebGLBridge should not be abstract");
        }

        [Test]
        public void WebGLBridge_ImplementsIPlatformBridge()
        {
            var interfaceType = typeof(NativeBrowser).Assembly
                .GetType("TedLiou.NativeBrowser.Internal.IPlatformBridge");
            Assert.IsNotNull(interfaceType, "IPlatformBridge should exist");
            Assert.IsTrue(interfaceType.IsAssignableFrom(bridgeType),
                "WebGLBridge should implement IPlatformBridge");
        }

        [Test]
        public void WebGLBridge_CanBeInstantiated()
        {
            // WebGLBridge has no constructor dependencies; it should be instantiable
            var instance = Activator.CreateInstance(bridgeType);
            Assert.IsNotNull(instance, "WebGLBridge should be instantiable via default constructor");
        }

        // ─── IPlatformBridge Method Tests ──────────────────────────────────────

        [Test]
        public void WebGLBridge_HasInitializeMethod()
        {
            var method = bridgeType.GetMethod("Initialize",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Initialize method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WebGLBridge_HasOpenMethod()
        {
            var method = bridgeType.GetMethod("Open",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Open method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);

            var parameters = method.GetParameters();
            Assert.AreEqual(2, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("type", parameters[0].Name);
            Assert.AreEqual(typeof(string), parameters[1].ParameterType);
            Assert.AreEqual("configJson", parameters[1].Name);
        }

        [Test]
        public void WebGLBridge_HasCloseMethod()
        {
            var method = bridgeType.GetMethod("Close",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Close method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WebGLBridge_HasRefreshMethod()
        {
            var method = bridgeType.GetMethod("Refresh",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Refresh method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WebGLBridge_HasIsOpenMethod()
        {
            var method = bridgeType.GetMethod("IsOpen",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "IsOpen method should exist");
            Assert.AreEqual(typeof(bool), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WebGLBridge_HasExecuteJavaScriptMethod()
        {
            var method = bridgeType.GetMethod("ExecuteJavaScript",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "ExecuteJavaScript method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);

            var parameters = method.GetParameters();
            Assert.AreEqual(2, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("script", parameters[0].Name);
            Assert.AreEqual(typeof(string), parameters[1].ParameterType);
            Assert.AreEqual("requestId", parameters[1].Name);
        }

        [Test]
        public void WebGLBridge_HasInjectJavaScriptMethod()
        {
            var method = bridgeType.GetMethod("InjectJavaScript",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "InjectJavaScript method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);

            var parameters = method.GetParameters();
            Assert.AreEqual(1, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("script", parameters[0].Name);
        }

        [Test]
        public void WebGLBridge_HasSendPostMessageMethod()
        {
            var method = bridgeType.GetMethod("SendPostMessage",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "SendPostMessage method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);

            var parameters = method.GetParameters();
            Assert.AreEqual(1, parameters.Length);
            Assert.AreEqual(typeof(string), parameters[0].ParameterType);
            Assert.AreEqual("message", parameters[0].Name);
        }

        // ─── P/Invoke Declaration Tests ────────────────────────────────────────
        // Note: WebGL P/Invoke methods are guarded by #if UNITY_WEBGL && !UNITY_EDITOR.
        // In Editor, these private static extern methods are compiled out.
        // We verify they exist only when compiled for WebGL by checking the source
        // structure via the public interface methods above.

        [Test]
        public void WebGLBridge_PInvokeMethods_AreConditionallyCompiled()
        {
            // In Editor mode, the private static extern P/Invoke methods should NOT exist
            // because they are inside #if UNITY_WEBGL && !UNITY_EDITOR blocks.
            var bindingFlags = BindingFlags.NonPublic | BindingFlags.Static;

            string[] expectedPInvokeMethods = new[]
            {
                "NB_WebGL_Initialize",
                "NB_WebGL_Open",
                "NB_WebGL_Close",
                "NB_WebGL_Refresh",
                "NB_WebGL_IsOpen",
                "NB_WebGL_ExecuteJavaScript",
                "NB_WebGL_InjectJavaScript",
                "NB_WebGL_SendPostMessage"
            };

            foreach (var methodName in expectedPInvokeMethods)
            {
                var method = bridgeType.GetMethod(methodName, bindingFlags);
                // In Editor, these should be compiled out (null)
                // This confirms the conditional compilation is working correctly
                Assert.IsNull(method,
                    $"P/Invoke method '{methodName}' should be compiled out in Editor " +
                    "(guarded by #if UNITY_WEBGL && !UNITY_EDITOR)");
            }
        }

        // ─── Editor Behavior Tests ─────────────────────────────────────────────
        // In Editor, all bridge methods are no-ops (empty bodies due to #if guards).
        // Verify they don't throw.

        [Test]
        public void WebGLBridge_Initialize_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("Initialize",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, null);
            }, "Initialize should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_Open_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("Open",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, new object[] { "WebView", "{\"url\":\"https://example.com\"}" });
            }, "Open should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_Close_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("Close",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, null);
            }, "Close should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_Refresh_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("Refresh",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, null);
            }, "Refresh should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_IsOpen_InEditor_ReturnsFalse()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("IsOpen",
                BindingFlags.Public | BindingFlags.Instance);

            var result = method.Invoke(instance, null);
            Assert.AreEqual(false, result,
                "IsOpen should return false in Editor (non-WebGL runtime)");
        }

        [Test]
        public void WebGLBridge_ExecuteJavaScript_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("ExecuteJavaScript",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, new object[] { "console.log('test');", "req-1" });
            }, "ExecuteJavaScript should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_InjectJavaScript_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("InjectJavaScript",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, new object[] { "window.testValue = 123;" });
            }, "InjectJavaScript should be a no-op in Editor without throwing");
        }

        [Test]
        public void WebGLBridge_SendPostMessage_InEditor_DoesNotThrow()
        {
            var instance = Activator.CreateInstance(bridgeType);
            var method = bridgeType.GetMethod("SendPostMessage",
                BindingFlags.Public | BindingFlags.Instance);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(instance, new object[] { "Hello from Unity" });
            }, "SendPostMessage should be a no-op in Editor without throwing");
        }

        // ─── Interface Completeness Test ───────────────────────────────────────

        [Test]
        public void WebGLBridge_ImplementsAllIPlatformBridgeMethods()
        {
            var interfaceType = typeof(NativeBrowser).Assembly
                .GetType("TedLiou.NativeBrowser.Internal.IPlatformBridge");
            Assert.IsNotNull(interfaceType, "IPlatformBridge should exist");

            var interfaceMap = bridgeType.GetInterfaceMap(interfaceType);

            // Verify all interface methods are mapped to concrete implementations
            for (int i = 0; i < interfaceMap.InterfaceMethods.Length; i++)
            {
                var interfaceMethod = interfaceMap.InterfaceMethods[i];
                var targetMethod = interfaceMap.TargetMethods[i];

                Assert.IsNotNull(targetMethod,
                    $"IPlatformBridge.{interfaceMethod.Name} should have a concrete implementation in WebGLBridge");
                Assert.IsFalse(targetMethod.IsAbstract,
                    $"WebGLBridge.{targetMethod.Name} should not be abstract");
            }
        }
    }
}
