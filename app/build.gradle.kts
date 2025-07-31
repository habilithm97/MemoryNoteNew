plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.memorynotenew"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.memorynotenew"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room
    implementation(libs.androidx.room.runtime) // Room 기본 (Entity, DAO, Database)
    // KSP : 코틀린에 최적화된 컴파일 타임 코드 생성 도구 (KAPT보다 더 빠르고 가벼움)
    ksp(libs.androidx.room.compiler) // DAO 구현체 및 코드 자동 생성
    implementation(libs.androidx.room.ktx) // 코틀린 확장 (코루틴, Flow)

    implementation(libs.androidx.lifecycle.runtime.ktx) // Lifecycle
    implementation(libs.androidx.lifecycle.livedata.ktx) // LiveData

    // Fragment API에 코틀린 확장 기능(KTX) 추가
    implementation(libs.androidx.fragment.ktx)

    // Preference
    implementation(libs.androidx.preference.ktx)

    // EncryptedSharedPreferences, MasterKey
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.security.crypto.ktx)
}