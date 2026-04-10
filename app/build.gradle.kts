import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 读取 local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.permissionx.animalguide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.permissionx.animalguide"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.2"

        buildConfigField("String", "BAIDU_API_KEY", "\"${localProperties["BAIDU_API_KEY"]}\"")
        buildConfigField("String", "BAIDU_SECRET_KEY", "\"${localProperties["BAIDU_SECRET_KEY"]}\"")

        buildConfigField("String", "DOUBAO_API_KEY", "\"${localProperties["DOUBAO_API_KEY"]}\"")
        buildConfigField(
            "String",
            "DOUBAO_ENDPOINT_ID",
            "\"${localProperties["DOUBAO_ENDPOINT_ID"]}\""
        )

        buildConfigField("String", "CLOUDBASE_ENV_ID", "\"${localProperties["CLOUDBASE_ENV_ID"]}\"")
        buildConfigField(
            "String",
            "CLOUDBASE_ACCESS_TOKEN",
            "\"${localProperties["CLOUDBASE_ACCESS_TOKEN"]}\""
        )
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coil
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Navigation
    implementation(libs.navigation.compose)

    // Gson
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.android)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.compose)

    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.animation:animation:1.6.8")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Paging3（帖子列表分页）
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("com.github.yalantis:ucrop:2.2.10")
}