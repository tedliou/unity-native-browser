# Keep JavaScript interface methods (WebView ↔ Unity communication)
-keepclassmembers class com.tedliou.android.browser.webview.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep bridge classes (Unity reflection via AndroidJavaClass)
-keep class com.tedliou.android.browser.bridge.** { *; }

# Keep configuration data class (may be serialized from Unity)
-keep class com.tedliou.android.browser.core.BrowserConfig { *; }

# Keep callback interface (Unity may implement via reflection)
-keep class com.tedliou.android.browser.core.BrowserCallback { *; }

# Keep all browser implementations (Unity creates via reflection)
-keep class com.tedliou.android.browser.webview.WebViewBrowser { *; }
-keep class com.tedliou.android.browser.customtab.CustomTabBrowser { *; }
-keep class com.tedliou.android.browser.system.SystemBrowser { *; }
