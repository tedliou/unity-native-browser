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
        minSdk = 28

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
        fileTree("build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
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