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

// 读取 keystore.properties（本地签名）
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.permissionx.animalguide"
    compileSdk = 35

    // Release 签名：优先读环境变量（CI），其次读 keystore.properties（本地）
    signingConfigs {
        create("release") {
            val storePath = System.getenv("KEYSTORE_FILE")
                ?: keystoreProperties["storeFile"] as? String
            if (storePath != null) storeFile = rootProject.file(storePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties["storePassword"] as? String ?: ""
            keyAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProperties["keyAlias"] as? String ?: ""
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties["keyPassword"] as? String ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.permissionx.animalguide"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
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
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    // 加密存储（用于 UserSessionManager 的 Token 加密）
    implementation("androidx.security:security-crypto:1.0.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.12")
}

android.testOptions.unitTests.all {
    it.maxHeapSize = "2048m"
}