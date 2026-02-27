using System.IO;
using UnityEditor;
using UnityEngine;

namespace TedLiou.NativeBrowser.Editor
{
    /// <summary>
    /// Exports NativeBrowser as a traditional .unitypackage file.
    /// Can be invoked from the menu or from the command line in batch mode:
    ///   Unity -quit -batchmode -nographics -projectPath &lt;path&gt; \
    ///         -executeMethod TedLiou.NativeBrowser.Editor.PackageExporter.Export
    /// </summary>
    public static class PackageExporter
    {
        private const string MenuPath = "Tools/NativeBrowser/Export .unitypackage";
        private const string PackageRoot = "Assets/Plugins/NativeBrowser";
        private const string DefaultOutputDir = "Build";

        [MenuItem(MenuPath)]
        public static void ExportFromMenu()
        {
            string version = ReadPackageVersion();
            string fileName = $"NativeBrowser-{version}.unitypackage";
            string outputPath = Path.Combine(DefaultOutputDir, fileName);

            Directory.CreateDirectory(DefaultOutputDir);
            ExportToPath(outputPath);

            EditorUtility.RevealInFinder(outputPath);
            Debug.Log($"[NativeBrowser] Exported {outputPath}");
        }

        /// <summary>
        /// Entry point for batch mode export.  Writes to Build/ by default.
        /// Override with -outputPath command line argument.
        /// </summary>
        public static void Export()
        {
            string version = ReadPackageVersion();
            string outputPath = GetCommandLineArg("-outputPath");

            if (string.IsNullOrEmpty(outputPath))
            {
                string fileName = $"NativeBrowser-{version}.unitypackage";
                outputPath = Path.Combine(DefaultOutputDir, fileName);
            }

            string dir = Path.GetDirectoryName(outputPath);
            if (!string.IsNullOrEmpty(dir))
            {
                Directory.CreateDirectory(dir);
            }

            ExportToPath(outputPath);
            Debug.Log($"[NativeBrowser] Exported {outputPath}");
        }

        private static void ExportToPath(string outputPath)
        {
            AssetDatabase.ExportPackage(
                PackageRoot,
                outputPath,
                ExportPackageOptions.Recurse
            );
        }

        private static string ReadPackageVersion()
        {
            string packageJsonPath = Path.Combine(Application.dataPath, "Plugins", "NativeBrowser", "package.json");
            if (!File.Exists(packageJsonPath))
            {
                Debug.LogWarning("[NativeBrowser] package.json not found, using 0.0.0 as version");
                return "0.0.0";
            }

            string json = File.ReadAllText(packageJsonPath);

            // Simple parse — avoid pulling in a JSON library just for this
            int idx = json.IndexOf("\"version\"");
            if (idx < 0)
            {
                return "0.0.0";
            }

            int colon = json.IndexOf(':', idx);
            int quote1 = json.IndexOf('"', colon + 1);
            int quote2 = json.IndexOf('"', quote1 + 1);
            if (quote1 < 0 || quote2 < 0)
            {
                return "0.0.0";
            }

            return json.Substring(quote1 + 1, quote2 - quote1 - 1);
        }

        private static string GetCommandLineArg(string name)
        {
            string[] args = System.Environment.GetCommandLineArgs();
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (args[i] == name)
                {
                    return args[i + 1];
                }
            }

            return null;
        }
    }
}
