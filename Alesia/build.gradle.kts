import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("com.android.library")
}

group = "me.eliej"
version = "1.0-SNAPSHOT"

kotlin {
    targets {
        androidTarget()
        jvm()
//        js(IR) {
//            browser()
//        }
//
        wasmJs {
            browser()
        }
    }

    sourceSets {
        val okioVersion = "3.0.0"

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
            }
         }
        commonTest {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.mockk:mockk-common:1.10.3-jdk8")
            }
        }

        val androidMain by getting {
            dependencies {

            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.mockk:mockk-common:1.12.2")
                implementation("io.mockk:mockk:1.12.2")
            }
        }

        val wasmJsMain by getting {
            dependsOn(commonMain)
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

android {
    compileSdk = 34
    namespace = "com.alesia.alesia"
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    kotlin {
//        jvmToolchain(8)
//    }
}
