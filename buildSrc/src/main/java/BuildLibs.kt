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
    const val android_gradle_tools =
        "com.android.tools.build:gradle:${Versions.android_gradle_tools}"
    const val kotlin_gradle_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.ktlint_plugin}"
    const val androidx_navigation_safeargs_plugin =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.nav}"
}
