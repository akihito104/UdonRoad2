/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}
apply(from = rootProject.file("android_build.gradle"))

android {

    buildFeatures {
        dataBinding = true
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":feature:media"))
    implementation(project(":model"))
    implementation(Libs.KOTLIN_STDLIB_JDK)
    implementation(Libs.ANDROIDX_CORE_KTX)
    implementation(Libs.ANDROIDX_APPCOMPAT)
    implementation(Libs.ANDROIDX_CONSTRAINT_LAYOUT)
    implementation(Libs.MATERIAL_DESIGN)
    implementation(Libs.ANDROIDX_NAVIGATION_FRAGMENT_KTX)
    implementation(Libs.ANDROIDX_NAVIGATION_UI_KTX)

    testImplementation(Libs.JUNIT)

    androidTestImplementation(Libs.ANDROIDX_TEST_EXT_JUNIT)
    androidTestImplementation(Libs.ANDROIDX_TEST_ESPRESSO)
}
