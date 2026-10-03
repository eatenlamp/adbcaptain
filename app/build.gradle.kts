// SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// ADB Captain. Ownership notice - do not remove.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

import java.util.Properties

fun loadProps(fileName: String): Properties? {
    val propsFile = rootProject.file(fileName)
    if (!propsFile.exists()) return null
    val props = Properties()
    propsFile.inputStream().use { props.load(it) }
    return props
}

val keystoreProps = loadProps("keystore.properties")            // F-Droid / GitHub Releases signing key
val rustoreKeystoreProps = loadProps("rustore-keystore.properties") // separate RuStore signing key (isolated identity)

android {
    namespace = "adb.captain"
    compileSdk = 37

    defaultConfig {
        applicationId = "adb.captain"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
        // A separate RuStore signing identity. Isolated from the F-Droid key:
        // if either store's key leaks, the other build's signature stays valid
        // and no F-Droid metadata re-signing is needed.
        if (rustoreKeystoreProps != null) {
            create("rustore") {
                storeFile = rootProject.file(rustoreKeystoreProps["storeFile"] as String)
                storePassword = rustoreKeystoreProps["storePassword"] as String
                keyAlias = rustoreKeystoreProps["keyAlias"] as String
                keyPassword = rustoreKeystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Keep R8 off: required for F-Droid reproducible builds.
            // Rules live in proguard-rules.pro for when minification is re-enabled.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // No signingConfig here on purpose: a build-type-level signingConfig
            // overrides the product flavor's one in AGP, which silently signed the
            // RuStore build with the F-Droid key and broke signer isolation.
            // Each store flavor below owns its own identity instead.
            vcsInfo {
                include = false
            }
        }
    }
    flavorDimensions += "store"
    productFlavors {
        create("fdroid") {
            dimension = "store"
            // Pure FOSS build for F-Droid and GitHub Releases. All features, no tracking.
            signingConfig = if (keystoreProps != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        create("rustore") {
            dimension = "store"
            // Same FOSS build for RuStore. No paid features, no analytics.
            // Unique package so it can be installed alongside the F-Droid build.
            applicationId = "adb.captain.rustore"
            // Store-specific source set (app/src/rustore) makes Russian the
            // default language and renames the app to "ADB Капитан".
            // This source set touches only the rustore variant, so the
            // fdroid variant's output stays byte-identical.
            // RuStore build is signed with its own key (signingConfig "rustore"),
            // isolated from the F-Droid/GitHub key that release builds use.
            signingConfig = if (rustoreKeystoreProps != null) {
                signingConfigs.getByName("rustore")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    // Required for F-Droid Reproducible Builds: do not embed the
    // dependency list / signing blob into the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Wireless ADB client (connect/pair to remote or local ADB daemons)
    implementation(libs.libadb.android)
    implementation(libs.conscrypt.android)
    implementation(libs.sun.security.android)

    // Shizuku API
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
