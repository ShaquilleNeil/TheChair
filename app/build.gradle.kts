
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

// Load MAPS_API_KEY from local.properties correctly
val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
val MAPS_API_KEY: String = localProps.getProperty("MAPS_API_KEY") ?: ""

android {
    namespace = "com.example.thechair"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.thechair"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject key into manifest
        manifestPlaceholders["MAPS_API_KEY"] = MAPS_API_KEY

        // Inject key into BuildConfig.java
        buildConfigField("String", "MAPS_API_KEY", "\"$MAPS_API_KEY\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation(libs.coordinatorlayout)
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation(libs.recyclerview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
