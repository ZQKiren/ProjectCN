plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 28
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.lottie)
    //firebase
    implementation(libs.firebase.auth)
    //database
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    //Google Sign-In
    implementation(libs.google.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.identity)
    //Facebook Sign-In
    implementation(libs.facebook.login)
    //implementation(libs.facebook.android.sdk)
    //Product view
    implementation(libs.picasso)
    //Google Maps
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    implementation(libs.viewpager2)
    //Glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}