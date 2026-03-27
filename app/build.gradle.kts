
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dyu.ereader"
    compileSdk = 36

    defaultConfig {
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use(::load)
            }
        }
        val dropboxAppKey = providers.gradleProperty("DROPBOX_APP_KEY")
            .orElse(providers.environmentVariable("DROPBOX_APP_KEY"))
            .orElse(localProperties.getProperty("DROPBOX_APP_KEY") ?: "")
            .orElse("")
        val githubReleaseOwner = providers.gradleProperty("GITHUB_RELEASE_OWNER")
            .orElse(providers.environmentVariable("GITHUB_RELEASE_OWNER"))
            .orElse(localProperties.getProperty("GITHUB_RELEASE_OWNER") ?: "JustApstl")
            .orElse("JustApstl")
        val githubReleaseRepo = providers.gradleProperty("GITHUB_RELEASE_REPO")
            .orElse(providers.environmentVariable("GITHUB_RELEASE_REPO"))
            .orElse(localProperties.getProperty("GITHUB_RELEASE_REPO") ?: "EReader")
            .orElse("EReader")
        applicationId = "com.dyu.ereader"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "2.2.3"
        buildConfigField("String", "MOBI_CONVERTER_URL", "\"\"")
        buildConfigField("String", "DROPBOX_APP_KEY", "\"${dropboxAppKey.get()}\"")
        buildConfigField("String", "GITHUB_RELEASE_OWNER", "\"${githubReleaseOwner.get()}\"")
        buildConfigField("String", "GITHUB_RELEASE_REPO", "\"${githubReleaseRepo.get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

dependencies {

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)
    
    implementation(libs.androidx.webkit)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Text-to-Speech
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.base)

    // PDF Generation
    implementation(libs.itext7.core)

    // Cloud Storage (Google Drive & Dropbox)
    implementation(libs.play.services.drive)
    implementation(libs.dropbox.core.sdk)
    implementation(libs.play.services.auth)

    // OPDS & XML Parsing
    implementation(libs.gson)
    implementation(libs.xmlpull)

    // Analytics (Privacy-respecting)
    implementation(libs.play.services.analytics)

    // Retrofit for OPDS API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
