import java.time.Duration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // alias(libs.plugins.ksp) // Unused after migrating durable data to Cloud Firestore
    alias(libs.plugins.google.services)
}

// Automatically provide a template configuration if google-services.json is absent locally
val googleServicesJson = file("google-services.json")
val googleServicesExample = file("google-services.json.example")
if (!googleServicesJson.exists() && googleServicesExample.exists()) {
    googleServicesExample.copyTo(googleServicesJson, overwrite = false)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.zaythar.zxtmkr"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "2.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test> {
    failFast = true
    timeout.set(Duration.ofMinutes(2))
    systemProperty(
        "skipFirebaseTests",
        System.getProperty("skipFirebaseTests") ?: System.getenv("SKIP_FIREBASE_TESTS") ?: "false"
    )
    testLogging {
        events("passed", "failed", "skipped")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room dependencies commented out after migrating durable records to Cloud Firestore
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.multiplatform.markdown.renderer)
    implementation(libs.multiplatform.markdown.renderer.m3)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
