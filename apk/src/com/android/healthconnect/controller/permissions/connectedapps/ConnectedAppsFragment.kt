/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.ALLOWED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.DENIED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.INACTIVE
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for connected apps screen. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class ConnectedAppsFragment : Hilt_ConnectedAppsFragment() {

    companion object {
        const val ALLOWED_APPS_CATEGORY = "allowed_apps"
        private const val NOT_ALLOWED_APPS = "not_allowed_apps"
        private const val CANT_SEE_ALL_YOUR_APPS = "cant_see_apps"
        private const val REMOVE_ALL_APPS = "remove_all_apps"
        private const val INACTIVE_APPS = "inactive_apps"
    }

    private val viewModel: ConnectedAppsViewModel by viewModels()

    private val mAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(ALLOWED_APPS_CATEGORY)
    }

    private val mNotAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(NOT_ALLOWED_APPS)
    }

    private val mInactiveAppsPreference: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(INACTIVE_APPS)
    }

    private val mCantSeeAllYourAppsPreference: Preference? by lazy {
        preferenceScreen.findPreference(CANT_SEE_ALL_YOUR_APPS)
    }

    private val mRemoveAllApps: Preference? by lazy {
        preferenceScreen.findPreference(REMOVE_ALL_APPS)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connected_apps_screen, rootKey)
        mCantSeeAllYourAppsPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connectedApps_to_cantSeeAllYourApps)
            true
        }
        mRemoveAllApps?.setOnPreferenceClickListener {
            openRemoveAllAppsAccessDialog()
            true
        }
    }

    private fun openRemoveAllAppsAccessDialog() {
        AlertDialogBuilder(this)
            .setTitle(R.string.permissions_disconnect_all_dialog_title)
            .setMessage(R.string.permissions_disconnect_all_dialog_message)
            .setIcon(R.attr.disconnectAllIcon)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.permissions_disconnect_all_dialog_disconnect) { _, _ ->
                viewModel.disconnectAllApps()
            }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.connected_apps_title)
        viewModel.loadConnectedApps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.connectedApps.observe(viewLifecycleOwner) { connectedApps ->
            val connectedAppsGroup = connectedApps.groupBy { it.status }
            updateAllowedApps(connectedAppsGroup[ALLOWED].orEmpty())
            updateDeniedApps(connectedAppsGroup[DENIED].orEmpty())
            updateInactiveApps(connectedAppsGroup[INACTIVE].orEmpty())
        }
    }

    private fun updateInactiveApps(appsList: List<ConnectedAppMetadata>) {
        mInactiveAppsPreference?.removeAll()
        if (appsList.isEmpty()) {
            preferenceScreen.removePreference(mInactiveAppsPreference)
        } else {
            appsList.forEach { app ->
                mInactiveAppsPreference?.addPreference(getAppPreference(app))
            }
        }
    }

    private fun updateAllowedApps(appsList: List<ConnectedAppMetadata>) {
        mAllowedAppsCategory?.removeAll()
        if (appsList.isEmpty()) {
            mAllowedAppsCategory?.addPreference(getNoAppsPreference(R.string.no_apps_allowed))
        } else {
            appsList.forEach { app ->
                mAllowedAppsCategory?.addPreference(
                    getAppPreference(app) { navigateToAppInfoScreen(app) })
            }
        }
    }

    private fun updateDeniedApps(appsList: List<ConnectedAppMetadata>) {
        mNotAllowedAppsCategory?.removeAll()

        if (appsList.isEmpty()) {
            mNotAllowedAppsCategory?.addPreference(getNoAppsPreference(R.string.no_apps_denied))
        } else {
            appsList.forEach { app ->
                mNotAllowedAppsCategory?.addPreference(
                    getAppPreference(app) { navigateToAppInfoScreen(app) })
            }
        }
    }

    private fun navigateToAppInfoScreen(app: ConnectedAppMetadata) {
        findNavController()
            .navigate(
                R.id.action_connectedApps_to_connectedApp, getBundle(app.appMetadata.packageName))
    }

    private fun getNoAppsPreference(@StringRes res: Int): Preference {
        return Preference(context).also {
            it.setTitle(res)
            it.isSelectable = false
        }
    }

    private fun getAppPreference(
        app: ConnectedAppMetadata,
        onClick: (() -> Unit)? = null
    ): Preference {
        return Preference(requireContext()).also {
            it.title = app.appMetadata.appName
            it.icon = app.appMetadata.icon
            it.setOnPreferenceClickListener {
                onClick?.invoke()
                true
            }
        }
    }

    private fun getBundle(packageName: String): Bundle {
        val bundle = Bundle()
        bundle.putString("packageName", packageName)
        return bundle
    }
}
