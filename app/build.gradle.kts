import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Con Kotlin 2.0+ este plugin gestiona el Compose Compiler; no se necesita composeOptions
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val localModelManifestUrl = (findProperty("METHODICA_LOCAL_MODEL_MANIFEST_URL") as String?)?.trim().orEmpty()
val hasReleaseKeystore = if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    true
} else {
    false
}

android {
    namespace  = "com.methodica.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.methodica.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "0.1.0"
        buildConfigField(
            "String",
            "LOCAL_MODEL_MANIFEST_URL",
            "\"${localModelManifestUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        ignoreAssetsPatterns += "*.tflite"
        ignoreAssetsPatterns += "*.litertlm"
        ignoreAssetsPatterns += "*.task"
        noCompress += listOf("tflite", "litertlm", "task")
    }

    lint {
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "OldTargetApi",
            "ObsoleteSdkInt"
        )
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    // --- Compose BOM: fija versiones coherentes de todas las librerías Compose ---
    implementation(platform(libs.androidx.compose.bom))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // UI / Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navegación
    implementation(libs.androidx.navigation.compose)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room (fuente única de verdad)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (preferencias de usuario)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager (recordatorios: preparado, sin lógica funcional en Fase 0)
    implementation(libs.androidx.work.runtime.ktx)

    // PDF parsing (resumen textual para IA)
    implementation(libs.pdfbox.android)
    implementation(libs.google.play.services.mlkit.text.recognition)
    implementation(libs.google.play.services.tasks)
    implementation(libs.mediapipe.tasks.text)
    implementation(libs.mediapipe.tasks.genai)

    // Hilt DI
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // --- Test ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
