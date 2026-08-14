plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.faceaccess.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.faceaccess.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-mvp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Model MediaPipe (.task) phai giu nguyen khong nen, giong quy uoc cho .tflite/.onnx
    androidResources {
        noCompress += listOf("task")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // MediaPipe Tasks Vision (Face Landmarker)
    implementation(libs.mediapipe.tasks.vision)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Luu ho so hieu chinh va danh sach app ghim cuc bo
    implementation(libs.androidx.datastore.preferences)

    // SavedStateRegistryOwner thu cong cho ComposeView trong overlay Service
    implementation(libs.androidx.savedstate)

    // Guava ListenableFuture (CameraX can voi compileSdk >= 36)
    implementation(libs.guava)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
