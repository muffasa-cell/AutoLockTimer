android {
    namespace = "com.antorik.autolocktimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antorik.autolocktimer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
