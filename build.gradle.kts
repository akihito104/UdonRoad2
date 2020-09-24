// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://kotlin.bintray.com/kotlin-eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath(BuildLibs.ANDROID_GRADLE_TOOL)
        classpath(BuildLibs.KOTLIN_GRADLE_PLUGIN)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath(BuildLibs.ANDROIDX_NAVIGATION_SAFEARGS_PLUGIN)
    }
}

plugins {
    id("scabbard.gradle") version Versions.SCABBARD
    id("com.releaseshub.gradle.plugin") version Versions.RELEASES_HUB
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_PLUGIN
}

scabbard {
    enabled = true
    fullBindingGraphValidation = false
    outputFormat = "svg"
}

releasesHub {
    dependenciesBasePath = "buildSrc/src/main/java/"
    dependenciesClassNames = listOf("Libs.kt", "BuildLibs.kt")
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://kotlin.bintray.com/kotlin-eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    configurations.all {
        resolutionStrategy {
            force(Libs.KOTLINX_COROUTINES)
            force(Libs.KOTLINX_COROUTINES_ANDROID)
            force(Libs.KOTLIN_STDLIB_JDK7)
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
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
