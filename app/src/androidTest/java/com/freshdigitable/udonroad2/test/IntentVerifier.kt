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

package com.freshdigitable.udonroad2.test

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.BundleMatchers.hasEntry
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras
import org.hamcrest.CoreMatchers.allOf

fun intendingWithExtras(
    bundle: Bundle,
    responseBlock: (Intent) -> Instrumentation.ActivityResult = {
        Instrumentation.ActivityResult(0, null)
    }
) {
    val bundles = bundle.keySet().map { hasEntry(it, bundle.get(it)) }
    intending(hasExtras(allOf(bundles)))
        .respondWithFunction(responseBlock)
}

fun intendedWithExtras(bundle: Bundle) {
    val bundles = bundle.keySet().map { hasEntry(it, bundle.get(it)) }
    intended(hasExtras(allOf(bundles)))
}
