package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.BACKUP_PREF
import com.example.memorynotenew.common.Constants.LOCK_PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.Constants.SIGN_OUT_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth
    private var signInPref: Preference? = null
    private var backupPref: Preference? = null
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
            showBackupDialog()
            true
        }
        // 로그아웃 Preference
        signOutPref = findPreference(SIGN_OUT_PREF)
        signOutPref?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewLifecycleOwner) {
            lifecycleScope.launch {
                // STARTED 상태에서만 collect 실행
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    // 실시간 백업 상태 Flow 관찰
                    memoViewModel.isBackupRunning.collect { running ->
                        backupPref?.apply {
                            title = if (running) {
                                getString(R.string.backup_running_title)
                            } else {
                                getString(R.string.backup)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBackupDialog() {
        val isRunning = memoViewModel.isBackupRunning.value

        val title = if (isRunning) getString(R.string.backup_stop) else getString(R.string.backup)
        val message = if (isRunning) getString(R.string.backup_stop_dialog) else getString(R.string.backup_dialog)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                // 클릭 시점 다시 읽어서 안전하게 토글
                val currentlyRunning = memoViewModel.isBackupRunning.value

                if (currentlyRunning) {
                    memoViewModel.stopBackup()
                } else {
                    memoViewModel.startBackup()
                }
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

                auth.signOut() // 로그아웃
                memoViewModel.onUserChanged() // 사용자 변경 알리기
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
            로그인x, 이메일 인증x -> "로그인" 표시 */
            title = user?.let { if (isVerified) it.email else getString(R.string.sign_in) }
                ?: getString(R.string.sign_in)
            isEnabled = !isSignedIn // 로그인x -> 클릭 가능
        }
        // 백업 Preference
        backupPref?.isEnabled = isSignedIn // 로그인x -> 클릭 가능
        // 로그아웃 Preference
        signOutPref?.isVisible = isSignedIn // 로그인x -> 클릭 가능
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