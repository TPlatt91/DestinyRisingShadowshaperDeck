plugins { id("com.android.application") }
android {
    namespace = "com.tplatt.shadowshaper"
    compileSdk = 35
    defaultConfig { applicationId = "com.tplatt.shadowshaper"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

// Local JVM tests; no GitHub Actions or GitHub credits are used.
dependencies { testImplementation("junit:junit:4.13.2") }
