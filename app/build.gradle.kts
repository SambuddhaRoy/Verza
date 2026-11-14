import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.chaquopy)
}

// Release signing comes from keystore.properties at the root, which is never committed. Without it
// the release build is unsigned and won't install over the published app.
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }

android {
    namespace = "com.verza"
    compileSdk = 35

    defaultConfig {
        // Same id and key as the old app, so this installs over it and finds its library.
        applicationId = "com.verza"
        minSdk = 26
        targetSdk = 35
        versionCode = 103
        versionName = "2.2.0"
        // Chaquopy ships Python per ABI. 3.11 is the newest with a 32-bit build; 3.12+ would drop
        // armeabi-v7a phones, which ran Verza before since it had no native code.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
    }

    signingConfigs {
        if (keystoreFile.exists()) create("release") {
            storeFile = rootProject.file(keystore.getProperty("storeFile"))
            storePassword = keystore.getProperty("storePassword")
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "Verza-v${variant.versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipeExtractor calls Java APIs Android only has from 13 (URLDecoder.decode with a Charset).
        // Without this every stream lookup throws below 13 and nothing plays.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

ksp { arg("room.generateKotlin", "true") }

// The Nicotine+ core (src/main/python) for Soulseek downloads, data/downloads/Soulseek.kt
// A Python 3.11 here (`buildPython=` in local.properties) compiles the .py files at build time; without one
// they ship as source and each phone compiles them the first time Soulseek starts.
val localPython = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
    .getProperty("buildPython")
chaquopy { defaultConfig { version = "3.11"; localPython?.let { buildPython(it) } } }

dependencies {
    coreLibraryDesugaring(libs.desugar)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.datastore)
    implementation(libs.palette)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.guava)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.newpipe)

    testImplementation(libs.junit)
}
