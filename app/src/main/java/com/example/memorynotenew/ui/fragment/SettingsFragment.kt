package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    private var signInPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        signInPref = findPreference<Preference>(SIGN_IN_PREF)
        val passwordPref = findPreference<Preference>(PW_PREF)

        signInPref?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SignInFragment())
                .addToBackStack(null)
                .commit()
            true
        }
        passwordPref?.setOnPreferenceClickListener {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.SETTINGS)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, passwordFragment)
                .addToBackStack(null)
                .commit()
            true
        }
    }

    override fun onResume() {
        super.onResume()

        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            if (currentUser.isEmailVerified) {
                signInPref?.title = currentUser.email
                signInPref?.isEnabled = false
            } else {
                signInPref?.title = getString(R.string.sign_in)
            }
        }
    }
}