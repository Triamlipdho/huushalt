import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Set by the release workflow from the Git tag (v1.2.3 -> "1.2.3" / 10203).
// Local and CI builds fall back to a development version.
val appVersionName = providers.environmentVariable("HUUSHALT_VERSION_NAME").orNull ?: "0.0.0-dev"
val appVersionCode = providers.environmentVariable("HUUSHALT_VERSION_CODE").orNull?.toInt() ?: 1

// Release signing comes from environment variables, never from files in the repository.
val keystoreFile = providers.environmentVariable("HUUSHALT_KEYSTORE_FILE").orNull
val keystorePassword = providers.environmentVariable("HUUSHALT_KEYSTORE_PASSWORD").orNull
val keyAliasName = providers.environmentVariable("HUUSHALT_KEY_ALIAS").orNull
// PKCS12 keystores (the keytool default) use the keystore password for the key as well,
// so the key password is optional.
val keyPasswordValue = providers.environmentVariable("HUUSHALT_KEY_PASSWORD").orNull?.takeIf { it.isNotBlank() } ?: keystorePassword
val hasReleaseSigning = listOf(keystoreFile, keystorePassword, keyAliasName, keyPasswordValue).all { !it.isNullOrBlank() }

android {
    namespace = "xyz.haselbach.huushalt"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.haselbach.huushalt"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric UI tests need the app's resources.
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric downloads Android runtime jars during the tests; use the Maven Central
            // mirror for those too (see settings.gradle.kts).
            it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
        }
    }

    sourceSets {
        // Exported Room schemas for the migration tests. Robolectric reads the app's assets, so they
        // go into the debug build only; release APKs don't contain them.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    dependenciesInfo {
        // The dependency metadata block is only meant for Google Play; keep the APK clean.
        includeInApk = false
        includeInBundle = false
    }
}

room {
    // Exported schemas are committed, so future migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
