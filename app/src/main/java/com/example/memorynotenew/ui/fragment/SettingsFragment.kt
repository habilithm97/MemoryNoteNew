package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.PASSWORD_PREF
import com.example.memorynotenew.common.PasswordPurpose

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val passwordPref = findPreference<Preference>(PASSWORD_PREF)
        passwordPref?.setOnPreferenceClickListener {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.SETTINGS)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, passwordFragment)
                .addToBackStack(null)
                .commit()
            true
        }
    }
}