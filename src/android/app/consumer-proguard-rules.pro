# =============================================================================
# NativeBrowser ProGuard Rules (Consumer)
# =============================================================================
# These rules are bundled into the .aar and consumed by the final app's R8/ProGuard.
# Unity calls all classes via JNI reflection (AndroidJavaClass/AndroidJavaObject),
# so R8 cannot see the references and will strip them unless we explicitly keep them.
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
