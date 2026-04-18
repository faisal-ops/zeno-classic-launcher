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

android {
    namespace = "com.zeno.classiclauncher.nlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zeno.classiclauncher.nlauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.2.4"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as? String
            keyPassword = keystoreProperties["keyPassword"] as? String
            storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }
            storePassword = keystoreProperties["storePassword"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

