plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    // Add the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics")
    // Update Kotlin to match Firebase's requirements (2.1.0+)
    alias(libs.plugins.kotlin.android) // version inherited from project-level build.gradle.kts
}

secrets {
    // Change the properties file from the default "local.properties" in your root project
    // to another properties file in your root project.
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be checked in version
    // control.
    defaultPropertiesFileName = "local.defaults.properties"

    // Configure which keys should be ignored by the plugin by providing regular expressions.
    // "sdk.dir" is ignored by default.
    ignoreList.add("keyToIgnore") // Ignore the key "keyToIgnore"
    ignoreList.add("sdk.*")       // Ignore all keys matching the regexp "sdk.*"
}

android {
    namespace = "com.example.commuterpartner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.commuterpartner"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Enables code-related app optimization.
            isMinifyEnabled = true
            // Enables resource shrinking.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    implementation(libs.play.services.measurement.api)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Maps SDK for Android
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    // Maps SDK for Android Utility Library
    implementation("com.google.maps.android:android-maps-utils:1.1.0")
    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // For viewModelScope() function, simplifying launching coroutines from the ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    // For by activityViewModels()
    implementation ("androidx.activity:activity-ktx:1.8.0")
    implementation ("androidx.fragment:fragment-ktx:1.8.6")
    // For Activity Result API (accessing ringtones in Settings)
    implementation ("androidx.activity:activity-ktx:1.7.0")
    implementation ("androidx.fragment:fragment-ktx:1.5.7")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    // When using the BoM, don't specify versions in Firebase dependencies
    // Add the dependency for the Analytics library
    implementation("com.google.firebase:firebase-analytics")
    // Add the dependencies for any other desired Firebase products
    implementation("com.google.firebase:firebase-analytics-ktx")
    // Add the dependency for the Crashlytics library
    implementation("com.google.firebase:firebase-crashlytics")
}