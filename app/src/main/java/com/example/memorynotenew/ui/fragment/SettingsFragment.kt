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
import com.example.memorynotenew.common.LockPasswordPurpose
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    private var signInPref: Preference? = null
    private var backupPref: Preference? = null
    private var loadPref: Preference? = null
    private var signOutPref: Preference? = null
    private lateinit var btnDeleteAccount: Button
    private val auth by lazy { FirebaseAuth.getInstance() }

    // 프래그먼트 생명주기에 맞춰 생성/관리되는 ViewModel
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        initPreferences()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 기존 PreferenceFragmentCompat 레이아웃 가져오기
        val root = super.onCreateView(inflater, container, savedInstanceState) as LinearLayout
        btnDeleteAccount = createDeleteAccountButton()
        root.addView(btnDeleteAccount)
        return root
    }

    // 뷰가 생성될 때 한 번만 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    // 화면이 보여질 때마다 UI 갱신
    override fun onResume() {
        super.onResume()

        updateUI()
    }

    private fun initPreferences() {
        // 로그인 Preference
        signInPref = findPreference<Preference?>(SIGN_IN_PREF)?.apply {
            setOnPreferenceClickListener {
                replaceFragment(SignInFragment())
                true
            }
        }
        // 잠금 비밀번호 설정 Preference
        findPreference<Preference>(LOCK_PW_PREF)?.setOnPreferenceClickListener {
            val lockPasswordFragment = LockPasswordFragment.newInstance(LockPasswordPurpose.SETTING)
            replaceFragment(lockPasswordFragment)
            true
        }
        // 백업 Preference
        backupPref = findPreference<Preference?>(BACKUP_PREF)?.apply {
            setOnPreferenceClickListener {
                it.isEnabled = false // 중복 클릭 방지
                showBackupDialog()
                true
            }
        }
        // 불러오기 Preference
        loadPref = findPreference<Preference?>(LOAD_PREF)?.apply {
            setOnPreferenceClickListener {
                it.isEnabled = false
                loadRequest()
                true
            }
        }
        // 로그아웃 Preference
        signOutPref = findPreference<Preference?>(SIGN_OUT_PREF)?.apply {
            setOnPreferenceClickListener {
                showSignOutDialog()
                true
            }
        }
    }

    private fun createDeleteAccountButton(): Button =
        Button(requireContext()).apply {
            text = getString(R.string.delete_account)
            setTextColor(Color.RED)
            setBackgroundResource(android.R.color.transparent)
            setOnClickListener {
                replaceFragment(DeleteAccountFragment())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                topMargin = 16
                bottomMargin = 16
            }
        }

    private fun observeViewModel() {
        with(mainViewModel) {
            backupResult.observe(viewLifecycleOwner) { // "메모 백업에 성공/실패했습니다."
                handleResult(it.isSuccess, R.string.backup_success, R.string.backup_failed)
            }
            loadResult.observe(viewLifecycleOwner) { // "메모 불러오기에 성공/실패했습니다."
                handleResult(it.isSuccess, R.string.load_success, R.string.load_failed)
            }
            getAllMemos.observe(viewLifecycleOwner) {} // 최신값
        }
    }

    private fun updateUI() {
        val user = auth.currentUser
        val isVerified = user?.isEmailVerified == true // 이메일 인증 여부
        val isSignedIn = user != null && isVerified // 이메일 인증까지 고려한 로그인 상태

        // 로그인 Preference
        signInPref?.apply {
            title = when {
                user == null -> getString(R.string.sign_in) // 사용자 x -> "로그인" 표시
                isVerified -> user.email // 인증 o -> 이메일 표시
                else -> getString(R.string.sign_in) // 인증 x -> "로그인" 표시
            }
            isEnabled = !isSignedIn // 로그인 x -> 활성화
        }
        // 백업 Preference
        backupPref?.isEnabled = isSignedIn // 로그인 o -> 활성화

        // 불러오기 Preference
        loadPref?.isEnabled = isSignedIn // 로그인 o -> 활성화

        // 로그아웃 Preference
        signOutPref?.isVisible = isSignedIn // 로그인 o -> 가시화

        // 로그인 여부에 따른 회원탈퇴 버튼 가시성 토글
        btnDeleteAccount.visibility = if (isSignedIn) View.VISIBLE else View.GONE
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.backup))
            .setMessage(getString(R.string.backup_dialog))
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                backupPref?.isEnabled = true
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                backupRequest()
                backupPref?.isEnabled = true
            }
            .setOnCancelListener {
                backupPref?.isEnabled = true
            }
            .show()
    }

    private fun backupRequest() {
        // 현재 메모 리스트에서 하나라도 잠겼으면 true 반환
        val hasLockedMemo = mainViewModel.memos.any { it.isLocked }

        if (hasLockedMemo) {
            val lockPasswordFragment = LockPasswordFragment.newInstance(LockPasswordPurpose.BACKUP)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, lockPasswordFragment)
                .commit()
        } else {
            mainViewModel.backupMemos()
        }
    }

    private fun loadRequest() {
        // 프래그먼트에서 안전하게 코루틴 실행
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val serverMemos = mainViewModel.firestoreRepository.load()

                if (serverMemos.isEmpty()) {
                    // "백업된 메모가 없습니다."
                    requireContext().showToast(getString(R.string.backup_empty))
                } else {
                    showLoadDialog()
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "An error occurred while checking memos.", e)
            }
        }
    }

    private fun showLoadDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.load))
            .setMessage(getString(R.string.load_dialog))
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                loadPref?.isEnabled = true
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                mainViewModel.loadMemos()
                loadPref?.isEnabled = true
            }
            .setOnCancelListener {
                loadPref?.isEnabled = true
            }
            .show()
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.dialog_sign_out))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                auth.signOut()
                updateUI()
            }
            .show()
    }

    private fun handleResult(isSuccess: Boolean, successMsg: Int, failMsg: Int) {
        val msg = if (isSuccess) successMsg else failMsg
        requireContext().showToast(getString(msg))
        requireActivity().finish()
    }
}