import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Export Room schemas so future migrations can be verified against every past version.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// google-services.json holds this project's Firebase config and is deliberately not committed. Without it the app still builds and
// runs, just with cloud sync switched off.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Release signing comes from keystore.properties (git-ignored, see AGENTS.md). Without it the release build is simply left unsigned.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// The app version is the single line in the VERSION file at the repo root: YBPV MAJOR.MINOR.PATCH (see AGENTS.md).
// versionCode is derived from it so it always goes up when the version does: 1.2.3 -> 10203.
val ybpVersion = rootProject.file("VERSION").readText().trim()
val ybpParts = ybpVersion.split(".").map { it.toIntOrNull() }
require(ybpParts.size == 3 && ybpParts.all { it != null && it in 0..99 }) { "VERSION must look like 1.2.3 (each part 0-99), found '$ybpVersion'" }

android {
    namespace = "com.example.biblepaceproject"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.joseycode.yourbiblepace"
        minSdk = 24
        targetSdk = 37
        versionCode = ybpParts[0]!! * 10000 + ybpParts[1]!! * 100 + ybpParts[2]!!
        versionName = ybpVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.id)
    implementation(libs.kotlinx.coroutines.play)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}