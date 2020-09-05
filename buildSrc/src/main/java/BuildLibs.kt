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

object BuildLibs {
    const val ANDROID_GRADLE_TOOL =
        "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_TOOL}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val KTLINT_PLUGIN = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.KTLINT_PLUGIN}"
    const val ANDROIDX_NAVIGATION_SAFEARGS_PLUGIN =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.ANDROIDX_NAVIGATION}"
    const val SCABBARD_PLUGIN =
        "gradle.plugin.dev.arunkumar:scabbard-gradle-plugin:${Versions.SCABBARD}"
    const val RELEASES_HUB = "com.releaseshub:releases-hub-gradle-plugin:${Versions.RELEASES_HUB}"
}
