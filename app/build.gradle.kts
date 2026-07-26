plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.llamadasdatos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.llamadasdatos"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Cliente WebRTC (fork mantenido activamente, reemplaza al AAR oficial
    // de Google que ya no se publica en JCenter)
    implementation("io.getstream:stream-webrtc-android:1.3.9")

    // WebSocket para señalización
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Corutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
