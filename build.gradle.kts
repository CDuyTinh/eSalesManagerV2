// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Same pin as MyeSalesSFA: AGP 9.2.1 compiles with Kotlin 2.2.0, which cannot read
// class metadata newer than 2.3.0. Left alone, transitive dependencies drag
// kotlin-stdlib up to 2.4.0 and every file that touches `Unit` fails to compile.
allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlinStdlib.get()}")
        }
    }
}
