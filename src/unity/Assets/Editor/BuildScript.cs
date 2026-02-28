using UnityEditor;
using UnityEngine;
using System.IO;

namespace TedLiou.Build
{
    /// <summary>
    /// Headless build script for Unity Android APK.
    /// Invoke via: Unity -batchmode -executeMethod TedLiou.Build.BuildScript.BuildAndroid
    /// </summary>
    public static class BuildScript
    {
        private const string OutputDir = "../../build";
        private const string ApkName  = "NativeBrowser.apk";

        /// <summary>
        /// Entry point for Unity headless Android build.
        /// </summary>
        public static void BuildAndroid()
        {
            Debug.Log("[BuildScript] Starting Android APK build...");

            // Ensure output directory exists
            if (!Directory.Exists(OutputDir))
                Directory.CreateDirectory(OutputDir);

            string apkPath = Path.Combine(OutputDir, ApkName);

            var options = new BuildPlayerOptions
            {
                scenes            = GetEnabledScenes(),
                locationPathName  = apkPath,
                target            = BuildTarget.Android,
                options           = BuildOptions.None
            };

            var report = BuildPipeline.BuildPlayer(options);

            if (report.summary.result == UnityEditor.Build.Reporting.BuildResult.Succeeded)
            {
                Debug.Log($"[BuildScript] Build succeeded: {apkPath} ({report.summary.totalSize / 1024 / 1024} MB)");
            }
            else
            {
                Debug.LogError($"[BuildScript] Build FAILED: {report.summary.result}");
                // Exit with error code so CI catches it
                EditorApplication.Exit(1);
            }
        }

        /// <summary>
        /// Entry point for Unity headless Windows EXE build.
        /// Invoke via: Unity -batchmode -executeMethod TedLiou.Build.BuildScript.BuildWindows
        /// </summary>
        public static void BuildWindows()
        {
            Debug.Log("[BuildScript] Starting Windows EXE build...");

            if (!Directory.Exists(OutputDir))
                Directory.CreateDirectory(OutputDir);

            string exePath = Path.Combine(OutputDir, "NativeBrowser.exe");

            var options = new BuildPlayerOptions
            {
                scenes           = GetEnabledScenes(),
                locationPathName = exePath,
                target           = BuildTarget.StandaloneWindows64,
                options          = BuildOptions.None
            };

            var report = BuildPipeline.BuildPlayer(options);

            if (report.summary.result == UnityEditor.Build.Reporting.BuildResult.Succeeded)
            {
                Debug.Log($"[BuildScript] Windows build succeeded: {exePath} ({report.summary.totalSize / 1024 / 1024} MB)");
            }
            else
            {
                Debug.LogError($"[BuildScript] Windows build FAILED: {report.summary.result}");
                EditorApplication.Exit(1);
            }
        }

        /// <summary>
        /// Entry point for Unity headless WebGL build.
        /// Invoke via: Unity -batchmode -executeMethod TedLiou.Build.BuildScript.BuildWebGL
        /// </summary>
        public static void BuildWebGL()
        {
            Debug.Log("[BuildScript] Starting WebGL build...");

            if (!Directory.Exists(OutputDir))
                Directory.CreateDirectory(OutputDir);

            string webglPath = Path.Combine(OutputDir, "WebGL");

            var options = new BuildPlayerOptions
            {
                scenes           = GetEnabledScenes(),
                locationPathName = webglPath,
                target           = BuildTarget.WebGL,
                options          = BuildOptions.None
            };

            var report = BuildPipeline.BuildPlayer(options);

            if (report.summary.result == UnityEditor.Build.Reporting.BuildResult.Succeeded)
            {
                Debug.Log($"[BuildScript] WebGL build succeeded: {webglPath}");
            }
            else
            {
                Debug.LogError($"[BuildScript] WebGL build FAILED: {report.summary.result}");
                EditorApplication.Exit(1);
            }
        }

        private static string[] GetEnabledScenes()
        {
            var scenes = new System.Collections.Generic.List<string>();
            foreach (var scene in EditorBuildSettings.scenes)
            {
                if (scene.enabled)
                    scenes.Add(scene.path);
            }

            if (scenes.Count == 0)
            {
                // Fallback: add SampleScene if nothing is configured
                scenes.Add("Assets/Scenes/SampleScene.unity");
            }

            Debug.Log($"[BuildScript] Building scenes: {string.Join(", ", scenes)}");
            return scenes.ToArray();
        }
    }
}
