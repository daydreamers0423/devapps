import java.util.Properties
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "in.daydreamers.devapps"
    compileSdk = 34
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "logurl", "http://192.168.130.226:5001/devapps-446507/us-central1/loganalytics")
        buildConfigField("String", "usageurl", "http://192.168.130.226:5001/devapps-446507/us-central1/logusage")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            multiDexEnabled = true


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