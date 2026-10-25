plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nmims.songstudio"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.nmims.songstudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui-android:1.10.4")
    implementation("androidx.compose.foundation:foundation-android:1.10.4")
    implementation("androidx.compose.material3:material3-android:1.4.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
