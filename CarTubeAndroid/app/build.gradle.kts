plugins {
    id("com.android.application")
}
android {
    namespace = "dev.cartube.parked"
    compileSdk = 35
    useLibrary("android.car")
    defaultConfig {
        applicationId = "dev.cartube.parked"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
