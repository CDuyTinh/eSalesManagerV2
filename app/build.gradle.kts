// Imported explicitly: inside a Gradle build script `java` resolves to the
// JavaPluginExtension, so `java.util.Properties` does not compile.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.tinhcd.myesalesmanager"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "com.tinhcd.myesalesmanager"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Keys come from local.properties (gitignored), and point at the same
        // Supabase project MyeSalesSFA uses: both apps share one database.
        val props = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { stream -> load(stream) }
        }
        buildConfigField("String", "SUPABASE_URL", "\"${props.getProperty("supabase.url") ?: ""}\"")
        buildConfigField(
            "String", "SUPABASE_PUBLISHABLE_KEY",
            "\"${props.getProperty("supabase.publishableKey") ?: ""}\"",
        )
        manifestPlaceholders["mapsApiKey"] = props.getProperty("maps.apiKey") ?: ""
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // The sign-in screen shows the build number, and data/ reads the keys.
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Supabase SDK for auth only — sign-in, session persistence and token refresh.
    // Every data call goes through Retrofit to the Edge Functions.
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Đi tuyến cùng nhân viên: a GPS fix at check-in, and shelf photos whose
    // rotation lives in EXIF.
    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.exifinterface)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
