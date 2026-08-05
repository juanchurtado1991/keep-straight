import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":shared-ui"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.onnxruntime)
    implementation(libs.webcam.capture)
    // Explicit so packaging always ships AVFoundation / Nokhwa / DirectShow / V4L2 natives.
    implementation(libs.webcam.capture.native)
    implementation(libs.ghost.serialization)
    implementation(libs.ghost.ktor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
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
                entitlementsFile.set(project.file("macos/entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("macos/runtime-entitlements.plist"))
                signing {
                    sign.set(
                        providers.gradleProperty("compose.desktop.mac.sign")
                            .map(String::toBoolean)
                            .orElse(false),
                    )
                    identity.set(
                        providers.gradleProperty("compose.desktop.mac.signing.identity").orElse(""),
                    )
                    providers.gradleProperty("compose.desktop.mac.signing.keychain").orNull?.let { path ->
                        keychain.set(file(path).absolutePath)
                    }
                }
                notarization {
                    appleID.set(providers.gradleProperty("compose.desktop.mac.notarization.appleID"))
                    password.set(providers.gradleProperty("compose.desktop.mac.notarization.password"))
                    teamID.set(providers.gradleProperty("compose.desktop.mac.notarization.teamID"))
                }
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSCameraUsageDescription</key>
                        <string>KeepStraight uses the camera for live posture detection. Frames are not saved.</string>
                        <key>NSLocalNetworkUsageDescription</key>
                        <string>KeepStraight finds your phone on the local network to sync alerts and install companion apps.</string>
                        <key>NSBonjourServices</key>
                        <array>
                            <string>_adb-tls-pairing._tcp</string>
                            <string>_adb-tls-connect._tcp</string>
                        </array>
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

val validateDesktopBundle = tasks.register("validateDesktopBundle") {
    group = "keepstraight"
    description = "Fails the build if bundled APKs or the MoveNet model are missing."
    dependsOn(syncCompanionApks)
    doLast {
        if (skipApkSync) return@doLast
        val apkDir = stagedApkRoot.get().dir("apks").asFile
        when (companionApksMode) {
            "phone" -> check(apkDir.resolve("keepstraight-phone.apk").isFile) {
                "Missing staged phone APK. Run :desktopApp:syncCompanionApks or build release APKs."
            }
            "wear" -> check(apkDir.resolve("keepstraight-wear.apk").isFile) {
                "Missing staged wear APK. Run :desktopApp:syncCompanionApks or build release APKs."
            }
            else -> {
                check(apkDir.resolve("keepstraight-phone.apk").isFile) {
                    "Missing staged phone APK. Run :desktopApp:syncCompanionApks."
                }
                check(apkDir.resolve("keepstraight-wear.apk").isFile) {
                    "Missing staged wear APK. Run :desktopApp:syncCompanionApks."
                }
            }
        }
        val model = layout.projectDirectory.file("src/main/resources/models/movenet_lightning.onnx").asFile
        check(model.isFile) {
            "Missing MoveNet model at ${model.path}. Run desktopApp/scripts/download-movenet.sh before packaging."
        }
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
    when (companionApksMode) {
        "phone" -> filesMatching("**/keepstraight-wear.apk") { exclude() }
        "wear" -> filesMatching("**/keepstraight-phone.apk") { exclude() }
    }
}

listOf("packageDeb", "packageMsi", "packageExe", "packageDmg", "createDistributable").forEach { taskName ->
    tasks.matching { it.name.equals(taskName, ignoreCase = true) }.configureEach {
        dependsOn(validateDesktopBundle)
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
