package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.BACKUP_PREF
import com.example.memorynotenew.common.Constants.PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.Constants.SIGN_OUT_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth
    private var signInPref: Preference? = null
    private var backupPref: Preference? = null
    private var signOutPref: Preference? = null
    private val memoViewModel: MemoViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        auth = FirebaseAuth.getInstance()
        signInPref = findPreference(SIGN_IN_PREF)
        val passwordPref = findPreference<Preference>(PW_PREF)
        backupPref = findPreference(BACKUP_PREF)
        signOutPref = findPreference(SIGN_OUT_PREF)

        // 로그인 Preference
        signInPref?.setOnPreferenceClickListener {
            replaceFragment(SignInFragment())
            true
        }
        // 비밀번호 설정 Preference
        passwordPref?.setOnPreferenceClickListener {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.SETTING)
            replaceFragment(passwordFragment)
            true
        }
        // 백업 Preference
        backupPref?.setOnPreferenceClickListener {
            showBackupDialog()
            true
        }
        // 로그아웃 Preference
        signOutPref?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.backup))
            .setMessage(getString(R.string.dialog_backup))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                memoViewModel.startBackup()
            }
            .show()
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.dialog_sign_out))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                auth.signOut()
                updateUI()
                memoViewModel.onUserChanged()
            }
            .show()
    }

    private fun updateUI() {
        val user = auth.currentUser // 현재 Firebase에 로그인된 사용자 (로그아웃 상태 -> null)
        val isVerified = user?.isEmailVerified ?: false // 이메일 인증 여부 (null -> false)
        val isSignedIn = user != null // 로그인 여부

        // 로그인 Preference
        signInPref?.apply { // 인증o -> 이메일, 인증x -> "로그인"
            title = if (isVerified) user?.email else getString(R.string.sign_in)
            isEnabled = !isVerified // 인증x -> 활성화
        }
        // 백업 Preference
        backupPref?.isEnabled = isSignedIn && isVerified
        // 로그아웃 Preference
        signOutPref?.isVisible = isSignedIn && isVerified
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()

        updateUI()
    }
}