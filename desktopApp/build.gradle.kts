import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.onnxruntime)
    implementation(libs.webcam.capture)
    // Explicit so packaging always ships AVFoundation / Nokhwa / DirectShow / V4L2 natives.
    implementation(libs.webcam.capture.native)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.zxing.core)
    implementation(libs.jmdns)
}

compose.desktop {
    application {
        mainClass = "com.keepstraight.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "KeepStraight"
            packageVersion = providers.gradleProperty("keepstraight.packageVersion")
                .getOrElse("1.0.0")
            description = "KeepStraight desktop posture companion"
            copyright = "KeepStraight"
            macOS {
                bundleID = "com.keepstraight.desktop"
                iconFile.set(project.file("icons/icon.icns"))
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSCameraUsageDescription</key>
                        <string>KeepStraight uses the camera for live posture detection. Frames are not saved.</string>
                    """.trimIndent()
                }
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
        jvmArgs("-Xmx512m")
    }
}

// The desktop app sideloads the companion apps over wireless adb, so every `run` and every
// packaged distribution has to carry freshly built release APKs. Pass
// -Pkeepstraight.skipApkSync=true to iterate on desktop-only code without Android builds.
val skipApkSync = providers.gradleProperty("keepstraight.skipApkSync")
    .map(String::toBoolean)
    .getOrElse(false)

val stagedApkRoot: Provider<Directory> = layout.buildDirectory.dir("companionApks")

fun registerApkStaging(taskName: String, module: String, stagedName: String) =
    tasks.register<Copy>(taskName) {
        group = "keepstraight"
        description = "Stages the $module release APK as $stagedName for the sideload installer."
        if (!skipApkSync) {
            dependsOn("$module:assembleRelease")
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(project(module).layout.buildDirectory.dir("outputs/apk/release")) {
            include("*.apk")
            rename(".*\\.apk", stagedName)
        }
        into(stagedApkRoot.map { it.dir("apks") })
    }

val stagePhoneApk = registerApkStaging("stagePhoneApk", ":androidApp", "keepstraight-phone.apk")
val stageWearApk = registerApkStaging("stageWearApk", ":wearApp", "keepstraight-wear.apk")

val syncCompanionApks = tasks.register("syncCompanionApks") {
    group = "keepstraight"
    description = "Builds the phone and watch release APKs and stages them for the desktop app."
    dependsOn(stagePhoneApk, stageWearApk)
}

sourceSets.named("main") {
    resources.srcDir(stagedApkRoot)
}

tasks.named("processResources") {
    dependsOn(syncCompanionApks)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
