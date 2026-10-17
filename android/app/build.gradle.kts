plugins {
    id("com.android.application")
}
val gameUrl = providers.gradleProperty("gameUrl").orElse("").get().trimEnd('/')
require(gameUrl.isEmpty() || gameUrl.startsWith("https://") || gameUrl.startsWith("http://")) {
    "gameUrl must be an HTTP(S) URL"
}
android {
    namespace = "com.example.podkidnoypoker"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.podkidnoypoker"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.4"
        buildConfigField("String", "GAME_BASE_URL", "\"$gameUrl\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
        debug { isDebuggable = true }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
