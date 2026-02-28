using System.Runtime.CompilerServices;

// Allow editor build scripts and tests to access internal types for integration testing.
[assembly: InternalsVisibleTo("TedLiou.Build")]
[assembly: InternalsVisibleTo("TedLiou.NativeBrowser.Tests.Editor")]
[assembly: InternalsVisibleTo("TedLiou.NativeBrowser.Tests.Runtime")]
