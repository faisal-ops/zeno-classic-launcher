import com.android.build.api.variant.impl.VariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun signingProperty(name: String, envName: String): String? =
    (keystoreProperties[name] as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.zeno.classiclauncher.nlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeno.classiclauncher.nlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "1.6.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = signingProperty("keyAlias", "ZENO_RELEASE_KEY_ALIAS")
            keyPassword = signingProperty("keyPassword", "ZENO_RELEASE_KEY_PASSWORD")
            storeFile = signingProperty("storeFile", "ZENO_RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = signingProperty("storePassword", "ZENO_RELEASE_STORE_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Zeno Classic - Debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

// APK output name: zeno-classic-launcher-vX.Y.Z.apk for release builds.
androidComponents {
    onVariants(selector().all()) { variant ->
        val buildType = variant.buildType
        variant.outputs.forEach { output ->
            val impl = output as? VariantOutputImpl ?: return@forEach
            impl.outputFileName.set(
                when (buildType) {
                    "release" -> "zeno-classic-launcher-v${android.defaultConfig.versionName}.apk"
                    "debug" -> "zeno-classic-launcher-debug-v${android.defaultConfig.versionName}.apk"
                    else -> "zeno-classic-launcher-$buildType.apk"
                },
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
