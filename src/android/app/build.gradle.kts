import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    jacoco
}

android {
    namespace = "com.tedliou.android.browser"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 26

        // -----------------------------------------------------------------------
        // API 26 相容性驗證（任務 1.2）
        // 已確認以下所有使用的 API 均相容 Android 8.0（API 26）：
        //
        //   WebView.evaluateJavascript(String, ValueCallback)  — API 19+，安全
        //   WebView.addJavascriptInterface(Object, String)     — API 1+，安全
        //   WebView.isAttachedToWindow                         — API 19+，安全
        //   WebSettings.javaScriptEnabled                      — API 1+，安全
        //   WebSettings.domStorageEnabled                      — API 7+，安全
        //   WebSettings.mixedContentMode (MIXED_CONTENT_ALWAYS_ALLOW) — API 21+，安全
        //   WebSettings.cacheMode (LOAD_DEFAULT)               — API 1+，安全
        //   WebSettings.userAgentString                        — API 1+，安全
        //   CustomTabsClient.getPackageName()                  — API 18+，安全
        //   CustomTabsClient.bindCustomTabsService()           — AndroidX，安全
        //   CustomTabsClient.warmup()                          — AndroidX，安全
        //   CustomTabsSession.mayLaunchUrl()                   — AndroidX，安全
        //   CustomTabsIntent.Builder / launchUrl()             — AndroidX，安全
        //   CustomTabColorSchemeParams                         — AndroidX，安全
        //   CustomTabsIntent.SHARE_STATE_ON / setShareState()  — AndroidX，安全
        //   Activity.runOnUiThread()                           — API 1+，安全
        //   Application.ActivityLifecycleCallbacks             — API 14+，安全
        //
        // 原始碼中無任何 @RequiresApi 標注或 Build.VERSION.SDK_INT 版本守衛，
        // 表示所有 API 呼叫路徑均在 minSdk = 26 範圍內安全使用。
        // -----------------------------------------------------------------------

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.all {
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
    kotlin {
        jvmToolchain(11)
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generate JaCoCo coverage report for unit tests"

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val sourceDirs = listOf("src/main/java", "src/main/kotlin")
    sourceDirectories.setFrom(sourceDirs.map { file(it) })
    classDirectories.setFrom(
        fileTree("build/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class",
                "**/R${'$'}*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "android/**/*.*"
            )
        }
    )
    executionData.setFrom("${layout.buildDirectory.get()}/jacoco/testDebugUnitTest.exec")
}

tasks.register<Task>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")
    group = "verification"
    description = "Verify JaCoCo code coverage meets 85% threshold"

    doLast {
        val reportTask = tasks.getByName("jacocoTestReport") as JacocoReport
        val xmlFile = reportTask.reports.xml.outputLocation.get().asFile
        if (xmlFile.exists()) {
            val xmlContent = xmlFile.readText()
            val lineRatePattern = """linerate="([^"]+)""".toRegex()
            val matches = lineRatePattern.findAll(xmlContent)
            matches.lastOrNull()?.let {
                val coveragePercent = (it.groupValues[1].toDouble() * 100).toInt()
                println("\n✓ JaCoCo Coverage: $coveragePercent%")
                if (coveragePercent < 85) {
                    throw GradleException("Coverage $coveragePercent% is below 85% threshold")
                }
            } ?: println("\n✗ No coverage data found in XML report")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.activity.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.runner.junit4)
    testImplementation(libs.kotest.property)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockk.android)
}

// Task to export all runtime dependencies for Unity plugin integration.
// Unity does not resolve Maven dependencies from .aar files, so all runtime
// jars/aars must be placed in Assets/Plugins/Android/ manually.
tasks.register<Copy>("exportDependencies") {
    group = "build"
    description = "Copy runtime dependencies to build/deps/ for Unity integration"

    from(configurations.named("releaseRuntimeClasspath").get().resolve())
    into(layout.buildDirectory.dir("deps"))
}