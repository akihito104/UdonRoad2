// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath(BuildLibs.ANDROID_GRADLE_TOOL)
        classpath(BuildLibs.KOTLIN_GRADLE_PLUGIN)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath(BuildLibs.ANDROIDX_NAVIGATION_SAFEARGS_PLUGIN)
        classpath(BuildLibs.RELEASES_HUB)
    }
}

plugins {
    id("scabbard.gradle") version Versions.SCABBARD
    id("com.dipien.releaseshub.gradle.plugin") version Versions.RELEASES_HUB
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_PLUGIN
    id("com.cookpad.android.plugin.license-tools") version Versions.LICENSE_TOOL
}

scabbard {
    enabled = true
    fullBindingGraphValidation = false
    outputFormat = "svg"
}

releasesHub {
    autoDetectDependenciesPaths = true
    dependenciesPaths = listOf("Libs.kt", "BuildLibs.kt").map { "buildSrc/src/main/java/$it" }
}

allprojects {
    repositories {
        google()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    val compilerArgs = listOf(
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlin.RequiresOptIn",
    )
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + compilerArgs
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent

    // Optionally configure plugin
    ktlint {
        version.set(Versions.KTLINT)
        debug.set(false)
        android.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
    }

    if (path.startsWith(":test-")) {
        tasks.whenTaskAdded {
            if (name.contains("UnitTest", ignoreCase = true)) {
                onlyIf { false }
            }
        }
    }
    if (path != ":app") {
        tasks.whenTaskAdded {
            if (name.contains("AndroidTest", ignoreCase = true)) {
                onlyIf { false }
            }
        }
    }
}

configure(listOf(project(":app"))) {
    apply(plugin = "com.cookpad.android.plugin.license-tools")

    licenseTools {
        ignoredProjects = setOf(":test-common", ":test-common-jvm")
    }

    tasks.whenTaskAdded {
        if (name.contains("assemble")) {
            dependsOn("checkLicenses")
        }
        if (name.matches("""generate.*Assets""".toRegex())) {
            dependsOn("generateLicensePage")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
