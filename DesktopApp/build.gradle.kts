import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
}

group = "me.eliej"
version = "1.0-SNAPSHOT"

kotlin {

    targets {
        jvm()
    }

    sourceSets {
        val okioVersion = "3.0.0"

        val commonMain by getting {
            dependencies {
                implementation(project(":Alesia"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation(compose.desktop.currentOs)
            }
        }

        val jvmMain by getting {
            dependencies {

                implementation("com.squareup.okio:okio:$okioVersion")
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
