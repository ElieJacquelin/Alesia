import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
}

group = "me.eliej"
version = "1.0-SNAPSHOT"

kotlin {

    targets {
        jvm()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Alesia"))
                implementation(libs.kotlin.coroutines)
                implementation(compose.desktop.currentOs)
            }
        }

        val jvmMain by getting {
            dependencies {

                implementation(libs.okio)
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
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            packageVersion = "0.1.0"
            targetFormats(TargetFormat.Exe)
        }
    }
}
