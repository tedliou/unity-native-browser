using System;
using System.Reflection;
using NUnit.Framework;
using TedLiou.NativeBrowser;
using TedLiou.NativeBrowser.Internal;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for the WindowsBridge implementation.
    /// Validates P/Invoke declarations, class structure, and IPlatformBridge conformance
    /// without requiring the actual native DLL to be loaded.
    /// </summary>
    [TestFixture]
    public class WindowsBridgeApiTest
    {
        private Type bridgeType;

        [SetUp]
        public void Setup()
        {
            var assembly = typeof(NativeBrowser).Assembly;
            bridgeType = assembly.GetType("TedLiou.NativeBrowser.Internal.WindowsBridge");
            Assert.IsNotNull(bridgeType, "WindowsBridge type should exist in the assembly");
        }

        // ─── Type Structure Tests ──────────────────────────────────────────────

        [Test]
        public void WindowsBridge_IsInternalClass()
        {
            Assert.IsFalse(bridgeType.IsPublic, "WindowsBridge should be internal, not public");
            Assert.IsTrue(bridgeType.IsClass, "WindowsBridge should be a class");
            Assert.IsFalse(bridgeType.IsAbstract, "WindowsBridge should not be abstract");
        }

        [Test]
        public void WindowsBridge_ImplementsIPlatformBridge()
        {
            var interfaceType = typeof(NativeBrowser).Assembly
                .GetType("TedLiou.NativeBrowser.Internal.IPlatformBridge");
            Assert.IsNotNull(interfaceType, "IPlatformBridge should exist");
            Assert.IsTrue(interfaceType.IsAssignableFrom(bridgeType),
                "WindowsBridge should implement IPlatformBridge");
        }

        // ─── IPlatformBridge Method Tests ──────────────────────────────────────

        [Test]
        public void WindowsBridge_HasInitializeMethod()
        {
            var method = bridgeType.GetMethod("Initialize",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Initialize method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WindowsBridge_HasOpenMethod()
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
        public void WindowsBridge_HasCloseMethod()
        {
            var method = bridgeType.GetMethod("Close",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Close method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WindowsBridge_HasRefreshMethod()
        {
            var method = bridgeType.GetMethod("Refresh",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "Refresh method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WindowsBridge_HasIsOpenMethod()
        {
            var method = bridgeType.GetMethod("IsOpen",
                BindingFlags.Public | BindingFlags.Instance);
            Assert.IsNotNull(method, "IsOpen method should exist");
            Assert.AreEqual(typeof(bool), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WindowsBridge_HasExecuteJavaScriptMethod()
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
        public void WindowsBridge_HasInjectJavaScriptMethod()
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
        public void WindowsBridge_HasSendPostMessageMethod()
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

        [Test]
        public void WindowsBridge_HasPInvokeDeclarations()
        {
            // Verify all expected P/Invoke methods exist as private static extern
            var bindingFlags = BindingFlags.NonPublic | BindingFlags.Static;

            string[] expectedPInvokeMethods = new[]
            {
                "NbInitialize",
                "NbOpen",
                "NbClose",
                "NbRefresh",
                "NbIsOpen",
                "NbExecuteJs",
                "NbInjectJs",
                "NbSendPostMessage",
                "NbDestroy"
            };

            foreach (var methodName in expectedPInvokeMethods)
            {
                var method = bridgeType.GetMethod(methodName, bindingFlags);
                Assert.IsNotNull(method, $"P/Invoke method '{methodName}' should exist");
                Assert.IsTrue((method.Attributes & MethodAttributes.PinvokeImpl) != 0
                    || method.GetMethodBody() == null,
                    $"'{methodName}' should be an extern method (P/Invoke)");
            }
        }

        [Test]
        public void WindowsBridge_DllImportAttribute_TargetsCorrectDll()
        {
            var bindingFlags = BindingFlags.NonPublic | BindingFlags.Static;
            var method = bridgeType.GetMethod("NbClose", bindingFlags);
            Assert.IsNotNull(method, "NbClose should exist");

            var dllImportAttr = method.GetCustomAttribute<System.Runtime.InteropServices.DllImportAttribute>();
            Assert.IsNotNull(dllImportAttr, "NbClose should have DllImport attribute");
            Assert.AreEqual("NativeBrowserWebView", dllImportAttr.Value,
                "DLL name should be 'NativeBrowserWebView'");
            Assert.AreEqual(System.Runtime.InteropServices.CallingConvention.Cdecl,
                dllImportAttr.CallingConvention,
                "Calling convention should be Cdecl");
        }

        // ─── Shutdown / Lifecycle Tests ────────────────────────────────────────

        [Test]
        public void WindowsBridge_HasShutdownMethod()
        {
            var method = bridgeType.GetMethod("Shutdown",
                BindingFlags.NonPublic | BindingFlags.Static | BindingFlags.Public | BindingFlags.Instance);
            // Shutdown is internal static
            if (method == null)
            {
                method = bridgeType.GetMethod("Shutdown",
                    BindingFlags.Static | BindingFlags.NonPublic | BindingFlags.Public);
            }
            Assert.IsNotNull(method, "Shutdown method should exist");
            Assert.IsTrue(method.IsStatic, "Shutdown should be static");
        }

        [Test]
        public void WindowsBridge_HasStaticConstructor()
        {
            // Static constructor registers Application.quitting += Shutdown
            var staticCtor = bridgeType.TypeInitializer;
            Assert.IsNotNull(staticCtor, "WindowsBridge should have a static constructor for shutdown hook registration");
        }

        // ─── State Fields Tests ────────────────────────────────────────────────

        [Test]
        public void WindowsBridge_HasCallbackDelegateField()
        {
            var field = bridgeType.GetField("callbackDelegate",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(field, "callbackDelegate field should exist to prevent GC of the pinned delegate");
            Assert.IsTrue(field.IsStatic, "callbackDelegate should be static");
        }

        [Test]
        public void WindowsBridge_HasInitializedField()
        {
            var field = bridgeType.GetField("initialized",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(field, "initialized field should exist");
            Assert.AreEqual(typeof(bool), field.FieldType);
        }

        // ─── GetUnityWindowHandle Test ─────────────────────────────────────────

        [Test]
        public void WindowsBridge_HasGetUnityWindowHandleMethod()
        {
            var method = bridgeType.GetMethod("GetUnityWindowHandle",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(method, "GetUnityWindowHandle helper should exist");
            Assert.AreEqual(typeof(IntPtr), method.ReturnType,
                "GetUnityWindowHandle should return IntPtr");
        }

        [Test]
        public void WindowsBridge_HasFindWindowPInvoke()
        {
            var method = bridgeType.GetMethod("FindWindow",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(method, "FindWindow P/Invoke should exist for HWND lookup");

            var dllImportAttr = method.GetCustomAttribute<System.Runtime.InteropServices.DllImportAttribute>();
            Assert.IsNotNull(dllImportAttr, "FindWindow should have DllImport attribute");
            Assert.AreEqual("user32.dll", dllImportAttr.Value,
                "FindWindow should import from user32.dll");
        }

        // ─── DLL Name Constant Test ────────────────────────────────────────────

        [Test]
        public void WindowsBridge_DllNameConstant_IsNativeBrowserWebView()
        {
            var field = bridgeType.GetField("DllName",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(field, "DllName constant should exist");
            Assert.IsTrue(field.IsLiteral, "DllName should be a const");
            Assert.AreEqual("NativeBrowserWebView", field.GetRawConstantValue(),
                "DllName should be 'NativeBrowserWebView'");
        }
    }
}
