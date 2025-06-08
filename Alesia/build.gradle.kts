plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinStdlibCommon)
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.datetime)
            }
         }
        commonTest {
            dependencies {
                implementation(libs.kotlinStdlibCommon)
                implementation(libs.kotlinTestCommon)
                implementation(libs.kotlinTestAnnotationsCommon )
                implementation(libs.kotlinTest)
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
                implementation(libs.kotlinStdlibCommon)
                implementation(libs.kotlinTestCommon)
                implementation(libs.kotlinTestAnnotationsCommon )
                implementation(libs.kotlinTest)
                implementation(libs.kotlinTestJunit)
                implementation(libs.mockk)
                implementation(libs.mockk.common)
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
