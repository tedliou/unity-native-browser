using System.IO;
using UnityEditor;
using UnityEngine;

namespace TedLiou.NativeBrowser.Editor
{
    /// <summary>
    /// Validates that the consumer's Unity project is correctly configured for
    /// NativeBrowser's Android dependencies.  Runs automatically on editor load
    /// and can also be triggered from the menu.
    /// </summary>
    [InitializeOnLoad]
    public static class GradleTemplateValidator
    {
        private const string MenuPath = "Tools/NativeBrowser/Validate Android Setup";

        // Dependencies that NativeBrowserDeps.androidlib declares.
        // If the consumer also has a mainTemplate.gradle with the same deps,
        // they will get a duplicate-class build error.
        private static readonly string[] KnownDependencies = new[]
        {
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android",
            "androidx.browser:browser",
            "androidx.webkit:webkit",
            "androidx.activity:activity-ktx"
        };

        static GradleTemplateValidator()
        {
            // Delay validation to avoid spamming during domain reload
            EditorApplication.delayCall += ValidateSilent;
        }

        [MenuItem(MenuPath)]
        public static void ValidateFromMenu()
        {
            Validate(silent: false);
        }

        private static void ValidateSilent()
        {
            Validate(silent: true);
        }

        private static void Validate(bool silent)
        {
            bool hasIssues = false;

            hasIssues |= CheckDuplicateDependencies();
            hasIssues |= CheckOldAarLocation();

            if (!hasIssues && !silent)
            {
                Debug.Log("[NativeBrowser] Android setup validation passed. No issues found.");
            }
        }

        /// <summary>
        /// Checks whether the consumer's mainTemplate.gradle contains dependencies
        /// that are already declared by NativeBrowserDeps.androidlib, which would
        /// cause duplicate-class errors at build time.
        /// </summary>
        private static bool CheckDuplicateDependencies()
        {
            string templatePath = Path.Combine(Application.dataPath, "Plugins", "Android", "mainTemplate.gradle");
            if (!File.Exists(templatePath))
            {
                return false;
            }

            string content = File.ReadAllText(templatePath);
            bool found = false;

            foreach (string dep in KnownDependencies)
            {
                if (content.Contains(dep))
                {
                    Debug.LogWarning(
                        $"[NativeBrowser] Duplicate dependency detected in mainTemplate.gradle: '{dep}'. " +
                        "This dependency is already provided by NativeBrowserDeps.androidlib. " +
                        "Remove it from mainTemplate.gradle to avoid duplicate-class build errors."
                    );
                    found = true;
                }
            }

            return found;
        }

        /// <summary>
        /// Checks whether a stale NativeBrowser.aar exists at the old location
        /// (Assets/Plugins/Android/) which may conflict with the one bundled
        /// inside the UPM package.
        /// </summary>
        private static bool CheckOldAarLocation()
        {
            string oldAarPath = Path.Combine(Application.dataPath, "Plugins", "Android", "NativeBrowser.aar");
            if (!File.Exists(oldAarPath))
            {
                return false;
            }

            // Only warn if the new .aar also exists (inside UPM package)
            string newAarPath = Path.Combine(Application.dataPath, "Plugins", "NativeBrowser",
                "Runtime", "Plugins", "Android", "NativeBrowser.aar");
            if (File.Exists(newAarPath))
            {
                Debug.LogWarning(
                    "[NativeBrowser] Duplicate NativeBrowser.aar detected. " +
                    $"Remove the old copy at '{oldAarPath}' — the UPM package already includes it."
                );
                return true;
            }

            return false;
        }
    }
}
