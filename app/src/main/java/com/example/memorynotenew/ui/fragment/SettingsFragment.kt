package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.PasswordPurpose

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val loginPref = findPreference<Preference>(SIGN_IN_PREF)
        loginPref?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SignInFragment())
                .addToBackStack(null)
                .commit()
            true
        }
        val passwordPref = findPreference<Preference>(PW_PREF)
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