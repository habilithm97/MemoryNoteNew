package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.BACK_UP_PREF
import com.example.memorynotenew.common.Constants.PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.Constants.SIGN_OUT_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth
    private var signInPref: Preference? = null
    private var backupPref: Preference? = null
    private var signOutPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        auth = FirebaseAuth.getInstance()
        signInPref = findPreference(SIGN_IN_PREF)
        val passwordPref = findPreference<Preference>(PW_PREF)
        backupPref = findPreference(BACK_UP_PREF)
        signOutPref = findPreference(SIGN_OUT_PREF)

        // 로그인 Preference
        signInPref?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SignInFragment())
                .addToBackStack(null)
                .commit()
            true
        }
        // 비밀번호 설정 Preference
        passwordPref?.setOnPreferenceClickListener {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.SETTINGS)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, passwordFragment)
                .addToBackStack(null)
                .commit()
            true
        }
        // 로그아웃 Preference
        signOutPref?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_dialog))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                auth.signOut()
                onResume()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()

        val user = auth.currentUser
        val isVerified = user?.isEmailVerified == true

        signInPref?.apply {
            title = if (isVerified) {
                user?.email
            } else {
                getString(R.string.sign_in)
            }
            isEnabled = !isVerified
        }
        backupPref?.isEnabled = isVerified
        signOutPref?.isVisible = user != null
    }
}