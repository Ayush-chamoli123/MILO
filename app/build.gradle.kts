import java.net.HttpURLConnection
import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.milo_2"
    // Downgraded to 35 (Stable Android 15) to match your current tools
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.milo_2"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Hardcoded stable versions to override the broken "libs" versions
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Standard Compose libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)

    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ----------------------------------------------------------------------------
// Automatic FastAPI Backend Startup Integration
// ----------------------------------------------------------------------------
tasks.register("startFastApiBackend") {
    group = "milo"
    description = "Automatically starts the FastAPI backend server if not already running."

    doLast {
        val healthUrl = "http://127.0.0.1:8000/api/health"

        fun isBackendHealthy(): Boolean {
            return try {
                val conn = URL(healthUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 800
                conn.readTimeout = 800
                conn.requestMethod = "GET"
                conn.responseCode in 200..399
            } catch (_: Exception) {
                false
            }
        }

        if (isBackendHealthy()) {
            logger.lifecycle("[MILO] FastAPI backend is already running on http://127.0.0.1:8000 (accessible to emulator at http://10.0.2.2:8000).")
            return@doLast
        }

        logger.lifecycle("[MILO] FastAPI backend is not running. Automatically starting backend...")

        val backendDir = project.rootDir.resolve("backend")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        try {
            if (isWindows) {
                val script = backendDir.resolve("start_backend.bat")
                ProcessBuilder("cmd.exe", "/c", "start", "MILO FastAPI Backend", script.absolutePath)
                    .directory(backendDir)
                    .start()
            } else {
                ProcessBuilder("bash", "-c", "uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload &")
                    .directory(backendDir)
                    .start()
            }

            // Wait up to 12 seconds for the server to become healthy
            val maxWaitMs = 12000L
            val startTime = System.currentTimeMillis()
            var isReady = false

            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                Thread.sleep(600)
                if (isBackendHealthy()) {
                    isReady = true
                    break
                }
            }

            if (isReady) {
                logger.lifecycle("[MILO] FastAPI backend started successfully and is ready! (Android Emulator: http://10.0.2.2:8000)")
            } else {
                logger.warn("[MILO] Backend process launched, but health check is taking longer than expected. It may still be starting up.")
            }
        } catch (e: Exception) {
            logger.warn("[MILO] Failed to automatically start backend: ${e.message}")
        }
    }
}

afterEvaluate {
    tasks.findByName("preDebugBuild")?.dependsOn("startFastApiBackend")
    tasks.findByName("assembleDebug")?.dependsOn("startFastApiBackend")
    tasks.findByName("installDebug")?.dependsOn("startFastApiBackend")
}
