import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.edu.utq.biometria.wear"
    // Wear Compose 1.5.x (y las versiones de Compose UI que trae transitivamente) exigen
    // compileSdk >= 35. targetSdk/minSdk se dejan igual -- compileSdk solo cambia contra que
    // APIs se compila, no el comportamiento en runtime.
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.edu.utq.biometria.wear"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }

        // API_BASE_URL / WS_BASE_URL viven en local.properties (no versionado) para no filtrar
        // hosts/URLs en el codigo fuente. Ver local.properties.sample para los defaults de dev
        // (10.0.2.2 = alias del emulador estandar de Android hacia el localhost del host).
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) load(FileInputStream(file))
        }
        val apiBaseUrl = localProps.getProperty("API_BASE_URL") ?: "http://10.0.2.2:8080"
        val wsBaseUrl = localProps.getProperty("WS_BASE_URL") ?: "ws://10.0.2.2:8080"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "WS_BASE_URL", "\"$wsBaseUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Con Kotlin 2.0+ el compilador de Compose lo maneja el plugin
    // org.jetbrains.kotlin.plugin.compose (ver "plugins" arriba) -- composeOptions.kotlinCompilerExtensionVersion
    // ya no aplica (era el mecanismo de Kotlin 1.9.x/K1).
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.material.icons.extended)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.input)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.security.crypto)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
