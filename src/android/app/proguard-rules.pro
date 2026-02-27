# =============================================================================
# NativeBrowser ProGuard Rules
# =============================================================================
# Mirror of consumer-proguard-rules.pro for local builds.
# =============================================================================

# Keep the main entry point (Unity calls BrowserManager via AndroidJavaClass)
-keep class com.tedliou.android.browser.BrowserManager { *; }

# Keep bridge classes (Unity reflection via AndroidJavaClass)
-keep class com.tedliou.android.browser.bridge.** { *; }

# Keep core data classes (serialized/deserialized from Unity via JSON)
-keep class com.tedliou.android.browser.core.** { *; }

# Keep JavaScript interface methods (WebView ↔ Unity communication)
-keepclassmembers class com.tedliou.android.browser.webview.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep all browser implementations (created via reflection)
-keep class com.tedliou.android.browser.webview.WebViewBrowser { *; }
-keep class com.tedliou.android.browser.customtab.CustomTabBrowser { *; }
-keep class com.tedliou.android.browser.system.SystemBrowser { *; }

# Keep utility classes used by browser implementations
-keep class com.tedliou.android.browser.util.** { *; }
-keep class com.tedliou.android.browser.webview.** { *; }
-keep class com.tedliou.android.browser.customtab.** { *; }