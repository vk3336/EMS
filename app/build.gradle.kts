plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ems"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ems"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {


    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    // JUnit for local unit tests
    testImplementation ("junit:junit:4.13.2")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.firebase:firebase-firestore:24.4.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    implementation("androidx.core:core-ktx:1.12.0") // Compatible version
    implementation("androidx.core:core:1.12.0")     // Compatible version

    // Firebase dependencies
    implementation("com.google.firebase:firebase-database:20.3.0")

    implementation("com.google.firebase:firebase-auth:22.3.1")

    // Location and UI libraries
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Lifecycle and Activity
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation(libs.firebase.firestore.ktx)

    implementation("androidx.cardview:cardview:1.0.0")

    implementation ("androidx.gridlayout:gridlayout:1.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
      //kapt("com.github.bumptech.glide:compiler:4.16.0")



}

