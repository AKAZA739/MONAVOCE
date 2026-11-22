import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val releaseStore = keystoreProps.getProperty("storeFile")?.takeIf { it.isNotBlank() }?.let(rootProject::file)
val hasReleaseKey = releaseStore?.isFile == true &&
    listOf("storePassword", "keyAlias", "keyPassword").all { !keystoreProps.getProperty(it).isNullOrBlank() }
val verifyUnsignedRelease = providers.gradleProperty("verifyUnsignedRelease").map { it.toBooleanStrict() }.orElse(false)

android {
    namespace = "com.myexpenses.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myexpenses.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        if (hasReleaseKey && !verifyUnsignedRelease.get()) {
            create("release") {
                storeFile = releaseStore
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            ndk { abiFilters += "x86_64" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    val signingAvailable = hasReleaseKey
    val unsignedVerification = verifyUnsignedRelease.get()
    doLast {
        check(signingAvailable || unsignedVerification) {
            "Release signing is missing. Configure keystore.properties or pass -PverifyUnsignedRelease=true for a local/CI build check."
        }
    }
}

tasks.configureEach {
    if (name in setOf("assembleRelease", "bundleRelease", "packageRelease", "packageReleaseBundle")) {
        dependsOn(checkReleaseSigning)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(files("libs/sherpa-onnx-1.10.45.aar"))

    testImplementation(libs.junit)
    testImplementation(libs.json.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
}
