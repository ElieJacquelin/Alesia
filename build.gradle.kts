import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "me.eliej"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }
//
//    nativeTarget.apply {
//        binaries {
//            executable {
//                entryPoint = "main"
//            }
//        }
//    }
    targets {
        jvm()
    }

    sourceSets {
        val okioVersion = "3.0.0"

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation(compose.desktop.currentOs)
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
                implementation("com.squareup.okio:okio-fakefilesystem:$okioVersion")
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
//        val jsMain by getting {
//            dependencies {
//                implementation("com.squareup.okio:okio-nodefilesystem:$okioVersion")
//            }
//        }
//        val nativeMain by getting
//        val nativeTest by getting
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
