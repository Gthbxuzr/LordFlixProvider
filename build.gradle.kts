import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories { google(); mavenCentral(); maven("https://jitpack.io") }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects { repositories { google(); mavenCentral(); maven("https://jitpack.io") } }

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    configure<BaseExtension> {
        namespace = "com.example.lordflix"
        compileSdkVersion(34)
        defaultConfig { minSdk = 21; targetSdk = 34 }
        compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
        }
    }

    dependencies {
        val implementation by configurations
        implementation("com.github.recloudstream.cloudstream:library:-SNAPSHOT")
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    }
}
