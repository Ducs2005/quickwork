plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" // Make sure to use the latest version of the plugin
}

android {
    namespace = "com.example.quickwork"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.quickwork"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    //implementation(libs.ads.mobile.sdk)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3.lint)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Compose dependencies via BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.material.icons.extended)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.kotlinx.coroutines.play.services)
    implementation("androidx.compose.material3:material3:1.2.1") // or latest stable
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2") // optional
    implementation("io.coil-kt:coil-compose:2.0.0")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.5")

    implementation ("com.google.zxing:core:3.5.1")
    implementation ("com.google.zxing:javase:3.5.1") //) For Bitmap generation

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

// CameraX (Preview + CameraController)
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    //implementation ("org.threeten:threetenbp:1.6.8")
    implementation("androidx.compose.animation:animation:1.6.0")

    implementation ("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation ("com.google.accompanist:accompanist-pager:0.32.0")
    //implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation ("com.google.firebase:firebase-bom:33.1.0")
    //implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation ("org.osmdroid:osmdroid-android:6.1.18")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

}