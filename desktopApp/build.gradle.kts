import org.gradle.internal.os.OperatingSystem
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
    implementation(compose.components.resources)
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
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)
    implementation(libs.voyager.koin)
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
                dockName = "KeepStraight"
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSCameraUsageDescription</key>
                        <string>KeepStraight uses the camera for live posture detection. Frames are not saved.</string>
                    """.trimIndent()
                }
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
                shortcut = true
                menu = true
                menuGroup = "KeepStraight"
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
                shortcut = true
                packageName = "keepstraight"
                appCategory = "Utility"
                menuGroup = "Utility"
                debMaintainer = "KeepStraight <support@keepstraight.app>"
            }
        }
        jvmArgs("-Xmx512m")
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.keepstraight.desktop.generated.resources"
}

// The desktop app sideloads companion apps over wireless adb. Pass
// -Pkeepstraight.skipApkSync=true to iterate on desktop-only code without Android builds.
// -Pkeepstraight.companionApks=phone|wear|both (default both) to trim installer size.
val skipApkSync = providers.gradleProperty("keepstraight.skipApkSync")
    .map(String::toBoolean)
    .getOrElse(false)

val companionApksMode = providers.gradleProperty("keepstraight.companionApks")
    .getOrElse("both")

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
    description = "Builds companion release APKs and stages them for the desktop app (see keepstraight.companionApks)."
    when (companionApksMode) {
        "phone" -> dependsOn(stagePhoneApk)
        "wear" -> dependsOn(stageWearApk)
        else -> dependsOn(stagePhoneApk, stageWearApk)
    }
}

sourceSets.named("main") {
    resources.srcDir(stagedApkRoot)
}

fun hostAdbPlatform(): String = when {
    OperatingSystem.current().isWindows -> "windows"
    OperatingSystem.current().isMacOsX -> "macos"
    else -> "linux"
}

/** Picks bundled adb for the packaging target (packageMsi → windows, etc.). Override with -PadbPlatform=. */
val adbPlatform = providers.gradleProperty("adbPlatform").orElse(
    providers.provider {
        val requested = gradle.startParameter.taskNames
            .joinToString(" ")
            .lowercase()
        when {
            "packagemsi" in requested || "packageexe" in requested -> "windows"
            "packagedmg" in requested -> "macos"
            "packagedeb" in requested || "packagerpm" in requested -> "linux"
            else -> hostAdbPlatform()
        }
    },
)

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncCompanionApks)
    inputs.property("adbPlatform", adbPlatform)
    inputs.property("companionApksMode", companionApksMode)
    filesMatching("adb/**") {
        val platform = adbPlatform.get()
        val folder = relativePath.segments.getOrNull(1)
        if (folder != null && folder != platform && folder != "NOTICE.txt") {
            exclude()
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
