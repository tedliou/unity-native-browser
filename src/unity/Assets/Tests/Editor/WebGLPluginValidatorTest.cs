using System;
using System.Reflection;
using NUnit.Framework;
using UnityEditor;
using TedLiou.NativeBrowser.Editor;

namespace TedLiou.NativeBrowser.Tests
{
    /// <summary>
    /// EditMode tests for the WebGLPluginValidator editor script.
    /// Validates class structure, MenuItem attribute, and method signatures.
    /// </summary>
    [TestFixture]
    public class WebGLPluginValidatorTest
    {
        private Type validatorType;

        [SetUp]
        public void Setup()
        {
            // WebGLPluginValidator is in the Editor assembly
            var editorAssembly = typeof(WebGLPluginValidator).Assembly;
            validatorType = editorAssembly.GetType("TedLiou.NativeBrowser.Editor.WebGLPluginValidator");
            Assert.IsNotNull(validatorType, "WebGLPluginValidator type should exist");
        }

        // ─── Type Structure Tests ──────────────────────────────────────────────

        [Test]
        public void WebGLPluginValidator_IsStaticClass()
        {
            Assert.IsTrue(validatorType.IsAbstract && validatorType.IsSealed,
                "WebGLPluginValidator should be a static class");
        }

        [Test]
        public void WebGLPluginValidator_IsPublic()
        {
            Assert.IsTrue(validatorType.IsPublic,
                "WebGLPluginValidator should be public");
        }

        [Test]
        public void WebGLPluginValidator_HasInitializeOnLoadAttribute()
        {
            var attr = validatorType.GetCustomAttribute<InitializeOnLoadAttribute>();
            Assert.IsNotNull(attr,
                "WebGLPluginValidator should have [InitializeOnLoad] attribute for automatic validation");
        }

        // ─── Method Tests ──────────────────────────────────────────────────────

        [Test]
        public void WebGLPluginValidator_HasValidateFromMenuMethod()
        {
            var method = validatorType.GetMethod("ValidateFromMenu",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(method, "ValidateFromMenu method should exist");
            Assert.AreEqual(typeof(void), method.ReturnType);
            Assert.AreEqual(0, method.GetParameters().Length);
        }

        [Test]
        public void WebGLPluginValidator_ValidateFromMenu_HasMenuItemAttribute()
        {
            var method = validatorType.GetMethod("ValidateFromMenu",
                BindingFlags.Public | BindingFlags.Static);
            Assert.IsNotNull(method);

            var menuAttr = method.GetCustomAttribute<MenuItem>();
            Assert.IsNotNull(menuAttr,
                "ValidateFromMenu should have [MenuItem] attribute for Unity menu integration");
        }

        [Test]
        public void WebGLPluginValidator_MenuPath_ContainsNativeBrowser()
        {
            // Verify the menu path constant includes the expected prefix
            var menuPathField = validatorType.GetField("MenuPath",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(menuPathField, "MenuPath constant should exist");
            Assert.IsTrue(menuPathField.IsLiteral, "MenuPath should be a const");

            var menuPath = menuPathField.GetRawConstantValue() as string;
            Assert.IsNotNull(menuPath);
            Assert.That(menuPath, Does.Contain("NativeBrowser"),
                "Menu path should contain 'NativeBrowser'");
            Assert.That(menuPath, Does.Contain("WebGL"),
                "Menu path should contain 'WebGL'");
        }

        [Test]
        public void WebGLPluginValidator_HasJslibRelativePathConstant()
        {
            var field = validatorType.GetField("JslibRelativePath",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(field, "JslibRelativePath constant should exist");
            Assert.IsTrue(field.IsLiteral, "JslibRelativePath should be a const");

            var path = field.GetRawConstantValue() as string;
            Assert.IsNotNull(path);
            Assert.That(path, Does.Contain("NativeBrowser.jslib"),
                "JslibRelativePath should reference the NativeBrowser.jslib file");
            Assert.That(path, Does.Contain("WebGL"),
                "JslibRelativePath should reference the WebGL directory");
        }

        [Test]
        public void WebGLPluginValidator_HasStaticConstructor()
        {
            // Static constructor should exist for [InitializeOnLoad] hook
            var staticCtor = validatorType.TypeInitializer;
            Assert.IsNotNull(staticCtor,
                "WebGLPluginValidator should have a static constructor for InitializeOnLoad hook");
        }

        [Test]
        public void WebGLPluginValidator_HasGetPackagePathMethod()
        {
            var method = validatorType.GetMethod("GetPackagePath",
                BindingFlags.NonPublic | BindingFlags.Static);
            Assert.IsNotNull(method, "GetPackagePath helper method should exist");
            Assert.AreEqual(typeof(string), method.ReturnType,
                "GetPackagePath should return string");
        }

        // ─── Functional Tests ──────────────────────────────────────────────────

        [Test]
        public void WebGLPluginValidator_ValidateFromMenu_DoesNotThrow()
        {
            // ValidateFromMenu should run without throwing even when build target is not WebGL
            var method = validatorType.GetMethod("ValidateFromMenu",
                BindingFlags.Public | BindingFlags.Static);

            Assert.DoesNotThrow(() =>
            {
                method.Invoke(null, null);
            }, "ValidateFromMenu should not throw regardless of current build target");
        }

        [Test]
        public void WebGLPluginValidator_GetPackagePath_ReturnsNonNull()
        {
            // In the dev environment, the package should be findable
            var method = validatorType.GetMethod("GetPackagePath",
                BindingFlags.NonPublic | BindingFlags.Static);

            var result = method.Invoke(null, null) as string;
            // In a development workspace, the package path should be resolved
            // (either via Packages/ or via Assets/Plugins/NativeBrowser/)
            Assert.IsNotNull(result,
                "GetPackagePath should resolve the NativeBrowser package path in development environment");
        }
    }
}
