plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "in.daydreamers.devapps"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    }

publishing {

    publications {
        register<MavenPublication>("release") {

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.functions)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.google.http.client.android)
    implementation(libs.google.http.client.gson) // Optional for JSON parsing

}