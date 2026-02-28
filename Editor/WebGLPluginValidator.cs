using System.IO;
using UnityEditor;
using UnityEngine;

namespace TedLiou.NativeBrowser.Editor
{
    /// <summary>
    /// Validates that the NativeBrowser WebGL plugin (.jslib) is present
    /// when the active build target is WebGL. Runs automatically on editor
    /// load and can be triggered from the menu.
    /// </summary>
    [InitializeOnLoad]
    public static class WebGLPluginValidator
    {
        private const string MenuPath = "Tools/NativeBrowser/Validate WebGL Setup";

        // Relative path from the package root to the .jslib
        private const string JslibRelativePath = "Runtime/Plugins/WebGL/NativeBrowser.jslib";

        static WebGLPluginValidator()
        {
            EditorApplication.delayCall += ValidateSilent;
        }

        [MenuItem(MenuPath)]
        public static void ValidateFromMenu()
        {
            Validate(silent: false);
        }

        private static void ValidateSilent()
        {
            // Only auto-validate when the active build target is WebGL
            if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.WebGL)
                return;

            Validate(silent: true);
        }

        private static void Validate(bool silent)
        {
            bool hasIssues = false;

            hasIssues |= CheckJslibPresent();

            if (!hasIssues && !silent)
            {
                Debug.Log("[NativeBrowser] WebGL setup validation passed. No issues found.");
            }
        }

        /// <summary>
        /// Checks whether the NativeBrowser.jslib plugin file exists.
        /// Without it, the WebGL build will compile but all NativeBrowser
        /// calls will fail at runtime with EntryPointNotFoundException.
        /// </summary>
        private static bool CheckJslibPresent()
        {
            // Try to find the .jslib via the package path
            string packagePath = GetPackagePath();
            if (string.IsNullOrEmpty(packagePath))
                return false;

            string jslibPath = Path.Combine(packagePath, JslibRelativePath);
            if (File.Exists(jslibPath))
                return false;

            Debug.LogError(
                "[NativeBrowser] WebGL plugin file not found: " + JslibRelativePath + ". " +
                "WebGL builds will fail at runtime. Ensure the NativeBrowser package is installed correctly."
            );
            return true;
        }

        /// <summary>
        /// Resolves the absolute filesystem path to the NativeBrowser package root.
        /// Works for both UPM (Packages/) and embedded (Assets/) installations.
        /// </summary>
        private static string GetPackagePath()
        {
            // UPM installation: the package is in the Library/PackageCache or Packages folder
            string upmAsset = "Packages/com.tedliou.nativebrowser";
            string fullPath = Path.GetFullPath(upmAsset);
            if (Directory.Exists(fullPath))
                return fullPath;

            // Embedded in Assets (development workflow)
            string embeddedPath = Path.Combine(Application.dataPath, "Plugins", "NativeBrowser");
            if (Directory.Exists(embeddedPath))
                return embeddedPath;

            return null;
        }
    }
}
