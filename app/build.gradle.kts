import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

    dependenciesInfo {
        // The dependency metadata block is only meant for Google Play; keep the APK clean.
        includeInApk = false
        includeInBundle = false
    }
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
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
