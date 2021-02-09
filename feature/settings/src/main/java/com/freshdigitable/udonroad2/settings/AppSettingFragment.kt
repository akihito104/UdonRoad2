/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AppSettingFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory

    private val viewModel: AppSettingViewModel by viewModels(
        factoryProducer = { viewModelProviderFactory }
    )

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLaunchLoginUserList()
        setupVersionInfo()
    }

    private fun setupLaunchLoginUserList() {
        val defaultEntry = resources.getStringArray(R.array.settings_loginUser_entries)
        val defaultValue = resources.getStringArray(R.array.settings_loginUser_values)
        val loginUserKey = getString(R.string.settings_key_loginUser)
        viewModel.registeredUserAccount.observe(viewLifecycleOwner) { accounts ->
            val preference: ListPreference = requireNotNull(findPreference(loginUserKey))
            preference.entries = defaultEntry + accounts.map { it.second }
            preference.entryValues = defaultValue + accounts.map { it.first.value.toString() }
        }
    }

    private fun setupVersionInfo() {
        val packageInfo = with(requireActivity()) {
            requireNotNull(packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA))
        }
        val versionKey = getString(R.string.settings_key_version)
        val versionPref: Preference = requireNotNull(findPreference(versionKey))
        versionPref.summary = packageInfo.versionName
    }
}
