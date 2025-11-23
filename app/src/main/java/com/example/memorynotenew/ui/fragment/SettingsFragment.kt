package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.BACKUP_PREF
import com.example.memorynotenew.common.Constants.LOAD_PREF
import com.example.memorynotenew.common.Constants.LOCK_PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.Constants.SIGN_OUT_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth
    private var signInPref: Preference? = null
    private var backupPref: Preference? = null
    private var loadPref: Preference? = null
    private var signOutPref: Preference? = null
    // 프래그먼트 생명주기에 맞춰 생성/관리되는 ViewModel
    private val memoViewModel: MemoViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        auth = FirebaseAuth.getInstance()

        // 로그인 Preference
        signInPref = findPreference(SIGN_IN_PREF)
        signInPref?.setOnPreferenceClickListener {
            replaceFragment(SignInFragment())
            true
        }
        // 잠금 비밀번호 설정 Preference
        val lockPwPref = findPreference<Preference>(LOCK_PW_PREF)
        lockPwPref?.setOnPreferenceClickListener {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.SETTING)
            replaceFragment(passwordFragment)
            true
        }
        // 백업 Preference
        backupPref = findPreference(BACKUP_PREF)
        backupPref?.setOnPreferenceClickListener {
            memoViewModel.backup()
            true
        }
        // 불러오기 Preference
        loadPref = findPreference(LOAD_PREF)
        loadPref?.setOnPreferenceClickListener {
            true
        }
        // 로그아웃 Preference
        signOutPref = findPreference(SIGN_OUT_PREF)
        signOutPref?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.dialog_sign_out))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                auth.signOut() // 로그아웃
                updateUI()
            }
            .show()
    }

    private fun updateUI() {
        val user = auth.currentUser
        val isVerified = user?.isEmailVerified ?: false // 이메일 인증 여부
        val isSignedIn = user != null // 로그인 상태 여부

        // 로그인 Preference
        signInPref?.apply {
            /* 이메일 인증 -> 이메일 표시
            로그인 x, 이메일 인증 x -> "로그인" 표시 */
            title = user?.let { if (isVerified) it.email else getString(R.string.sign_in) }
                ?: getString(R.string.sign_in)
            isEnabled = !isSignedIn // 로그인 x -> 활성화
        }
        // 백업 Preference
        backupPref?.isEnabled = isSignedIn // 로그인 o -> 활성화

        // 불러오기 Preference
        loadPref?.isEnabled = isSignedIn // 로그인 o -> 활성화

        // 로그아웃 Preference
        signOutPref?.isVisible = isSignedIn // 로그인 o -> 가시화
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