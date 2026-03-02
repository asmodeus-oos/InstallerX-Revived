import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutLibraries)
}

android {

    namespace = "com.android.packageinstaller"
    compileSdk = 36

    val properties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        try {
            properties.load(keystorePropertiesFile.inputStream())
        } catch (e: Exception) {
            println("Warning: Could not load keystore.properties file: ${e.message}")
        }
    }

    val storeFile = properties.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE")
    val storePassword =
        properties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = properties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
    val keyPassword = properties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")

    val hasCustomSigning =
        storeFile != null && storePassword != null && keyAlias != null && keyPassword != null

    defaultConfig {
        applicationId = "com.android.packageinstaller"

        minSdk = 26
        targetSdk = 36

        versionCode = 46
        versionName = project.findProperty("VERSION_NAME") as String?
            ?: project.findProperty("BASE_VERSION") as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCustomSigning) {
            register("releaseCustom") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {

        getByName("debug") {
            signingConfig =
                if (hasCustomSigning) {
                    println("Applying 'releaseCustom' signing to debug build.")
                    signingConfigs.getByName("releaseCustom")
                } else {
                    println("No custom signing info. Using default debug keystore.")
                    signingConfigs.getByName("debug")
                }

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig =
                if (hasCustomSigning) {
                    println("Applying 'releaseCustom' signing to release build.")
                    signingConfigs.getByName("releaseCustom")
                } else {
                    println("No custom signing info. Using default debug keystore.")
                    signingConfigs.getByName("debug")
                }

            isShrinkResources = true
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += listOf("connectivity", "level")

    productFlavors {

        create("online") {
            dimension = "connectivity"
            buildConfigField("boolean", "INTERNET_ACCESS_ENABLED", "true")
            isDefault = true
        }

        create("offline") {
            dimension = "connectivity"
            buildConfigField("boolean", "INTERNET_ACCESS_ENABLED", "false")
        }

        create("Unstable") {
            dimension = "level"
            isDefault = true
        }

        create("Preview") {
            dimension = "level"
        }

        create("Stable") {
            dimension = "level"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(25)
}

androidComponents {
    onVariants { variant ->
        val level = variant.productFlavors
            .firstOrNull { it.first == "level" }
            ?.second
            ?.let {
                when (it) {
                    "Unstable" -> 0
                    "Preview" -> 1
                    "Stable" -> 2
                    else -> 0
                }
            } ?: 0

        variant.buildConfigFields?.put(
            "BUILD_LEVEL",
            com.android.build.api.variant.BuildConfigField(
                "int",
                level.toString(),
                null
            )
        )
    }
}

aboutLibraries {
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    compileOnly(project(":hidden-api"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androix.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.materialIcons)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.ktx.serializationJson)
    implementation(libs.hiddenapibypass)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.accompanist.drawablepainter)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.appiconloader)

    implementation(libs.iamr0s.dhizuku.api)
    implementation(libs.iamr0s.androidAppProcess)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    implementation(libs.timber)

    implementation(libs.miuix)
    implementation(libs.miuix.icons)
    implementation(libs.capsule)

    implementation(libs.haze)
    implementation(libs.haze.materials)

    implementation(libs.okhttp)

    implementation(libs.monetcompat)
    implementation(libs.androidx.palette)

    implementation(libs.focus.api)

    implementation(libs.materialKolor)
}