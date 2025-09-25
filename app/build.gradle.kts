// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.schmotz.calendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.schmotz.calendar"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }

    // Align Java & Kotlin to 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // If you ever drop minSdk below 26, enable desugaring here.
        // coreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose BOM (maps to Compose UI 1.7.x)
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // XML theme support (for your styles/themes.xml)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // ✅ Kizitonwose calendar — version matched to Compose 1.7.x
    implementation("com.kizitonwose.calendar:compose:2.6.0")

    // Images & metadata
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jsoup:jsoup:1.17.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // If you enable desugaring above:
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}
