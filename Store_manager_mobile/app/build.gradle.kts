plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.store_manager"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.store_manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Dia chi nay danh cho Android Emulator truy cap may tinh dang chay Next.js.
        // Neu dung dien thoai that, thay bang IP LAN cua may tinh, vi du 192.168.1.10.
        buildConfigField("String", "DIA_CHI_API", "\"http://10.0.2.2:3000/\"")

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

    // Cho phep doc cac gia tri buildConfig nhu DIA_CHI_API trong Kotlin.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
