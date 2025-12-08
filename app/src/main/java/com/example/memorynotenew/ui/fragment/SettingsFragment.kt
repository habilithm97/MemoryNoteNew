package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.BACKUP_PREF
import com.example.memorynotenew.common.Constants.LOAD_PREF
import com.example.memorynotenew.common.Constants.LOCK_PW_PREF
import com.example.memorynotenew.common.Constants.SIGN_IN_PREF
import com.example.memorynotenew.common.Constants.SIGN_OUT_PREF
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            showBackupDialog()
            true
        }
        // 불러오기 Preference
        loadPref = findPreference(LOAD_PREF)
        loadPref?.setOnPreferenceClickListener {
            handleLoadRequest()
            true
        }
        // 로그아웃 Preference
        signOutPref = findPreference(SIGN_OUT_PREF)
        signOutPref?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 원래 PreferenceFragmentCompat 레이아웃 가져오기
        val root = super.onCreateView(inflater, container, savedInstanceState) as LinearLayout

        // 하단 버튼 생성
        val btnDeleteAccont = Button(requireContext()).apply {
            text = getString(R.string.delete_account)
            setTextColor(Color.RED)
            setBackgroundResource(android.R.color.transparent)
            setOnClickListener { replaceFragment(DeleteAccountFragment()) }
        }
        // 버튼을 Preference 아래에 추가
        root.addView(btnDeleteAccont)

        btnDeleteAccont.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START
            topMargin = 16
            bottomMargin = 16
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(memoViewModel) {
            backupResult.observe(viewLifecycleOwner) {
                handleResult(it.isSuccess, R.string.backup_success, R.string.backup_failed)
            }
            loadResult.observe(viewLifecycleOwner) {
                handleResult(it.isSuccess, R.string.load_success, R.string.load_failed)
            }
            // LiveData를 활성화하여 memoViewModel.memos가 최신값을 가지도록 함
            getAllMemos.observe(viewLifecycleOwner) {}
        }
    }

    private fun handleResult(isSuccess: Boolean, successMsg: Int, failMsg: Int) {
        val msg = if (isSuccess) successMsg else failMsg
        requireContext().showToast(getString(msg))
        requireActivity().finish()
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.backup))
            .setMessage(getString(R.string.backup_dialog))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                handleBackupRequest()
            }
            .show()
    }

    private fun handleBackupRequest() {
        // 현재 메모 리스트에서 하나라도 잠겼으면 true 반환
        val hasLockedMemo = memoViewModel.memos.any { it.isLocked }

        if (hasLockedMemo) {
            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.BACKUP)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, passwordFragment)
                .commit()
        } else {
            memoViewModel.backupMemos()
        }
    }

    private fun handleLoadRequest() {
        // 프래그먼트에서 안전하게 코루틴 실행
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val serverMemos = memoViewModel.firebaseRepository.load()

                // UI 업데이트는 메인 스레드에서 실행
                withContext(Dispatchers.Main) {
                    if (serverMemos.isEmpty()) {
                        requireContext().showToast(getString(R.string.backup_empty))
                    } else {
                        showLoadDialog()
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "An error occurred while checking memos.")
            }
        }
    }

    private fun showLoadDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.load))
            .setMessage(getString(R.string.load_dialog))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()

                memoViewModel.loadMemos()
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