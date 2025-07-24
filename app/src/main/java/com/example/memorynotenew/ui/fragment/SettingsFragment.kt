package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val passwordPref = findPreference<Preference>(Constants.PW_SETTINGS)
        passwordPref?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, PasswordFragment2())
                .addToBackStack(null)
                .commit()
            true
        }
    }
}