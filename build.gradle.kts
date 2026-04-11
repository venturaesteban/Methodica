// Top-level build file: solo registra plugins. La configuración real va en app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false
}

buildscript {
    configurations.classpath {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}
