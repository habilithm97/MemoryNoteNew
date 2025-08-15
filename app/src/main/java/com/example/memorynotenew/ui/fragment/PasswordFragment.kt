package com.example.memorynotenew.ui.fragment

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.common.PasswordMode
import com.example.memorynotenew.common.PasswordString
import com.example.memorynotenew.databinding.FragmentPasswordBinding
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.utils.PasswordManager
import com.example.memorynotenew.utils.ToastUtil
import com.example.memorynotenew.utils.VibrateUtil
import com.example.memorynotenew.viewmodel.MemoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordFragment : Fragment() {
    private lateinit var passwordPurpose: PasswordPurpose
    private lateinit var passwordMode: PasswordMode

    private val memo: Memo by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(Constants.MEMO, Memo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(Constants.MEMO)!!
        }
    }

    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)
    private val dots: List<View> by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }
    private val safeContext get() = requireContext() // Attach된 시점의 안전한 Context를 반환

    private var storedPassword: String? = null // 저장된 비밀번호
    private var password = StringBuilder()
    private var confirmPassword: StringBuilder? = null // 확인용 비밀번호
    private var isLocked = false // 입력 잠금 상태

    private val memoViewModel: MemoViewModel by viewModels()

    companion object {
        private const val PURPOSE = "password_purpose"

        fun newInstance(purpose: PasswordPurpose, memo: Memo? = null) : PasswordFragment {
            return PasswordFragment().apply {
                // enum 값을 문자열로 변환하여 arguments에 저장
                arguments = Bundle().apply {
                    putString(PURPOSE, purpose.name)

                    if (memo != null) {
                        putParcelable(Constants.MEMO, memo)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purpose = arguments?.getString(PURPOSE)
            ?: throw IllegalArgumentException("PasswordFragment is required")
        passwordPurpose = PasswordPurpose.valueOf(purpose) // 문자열을 enum 값으로 변환
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storedPassword = PasswordManager.getPassword(safeContext)

        passwordMode = if (storedPassword.isNullOrEmpty()) {
            PasswordMode.NEW // 새 비밀번호 입력 모드
        } else {
            PasswordMode.ENTER // 기존 비밀번호 입력 모드
        }
        setupTitle()
        setupKeypad()
        setupBtnCancel()
    }

    private fun setupTitle() {
        val title = when (passwordMode) {
            PasswordMode.NEW -> getString(PasswordString.NEW.resId) // "새 비밀번호 입력"
            PasswordMode.ENTER -> getString(PasswordString.ENTER.resId) // "기존 비밀번호 입력"
        }
        binding.textView.text = title
    }

    private fun setupKeypad() {
        with(binding) {
            val buttons = listOf(btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0)
            for (btn in buttons) {
                btn.setOnClickListener {
                    onPwKeyPressed(btn.text.toString())
                }
            }
        }
    }

    private fun onPwKeyPressed(number: String) {
        // 4자리 이상이거나 입력 처리 중이면 추가 입력 제한
        if (password.length >= 4 || isLocked) return

        password.append(number)
        updateDots()

        // 4자리 입력 완료
        if (password.length == 4) {
            isLocked = true // 입력 잠금

            // 화면 소멸 시 코루틴 자동 취소 (메모리 누수 방지)
            lifecycleScope.launch {
                delay(500)
                when (passwordPurpose) {
                    PasswordPurpose.SETTINGS -> { // 설정용
                        when (passwordMode) {
                            PasswordMode.NEW -> newPassword() // 새 비밀번호 저장
                            PasswordMode.ENTER -> updatePassword() // 비밀번호 변경
                        }
                    }
                    PasswordPurpose.LOCK -> lockMemo() // 메모 잠금 및 잠금 해제
                    PasswordPurpose.OPEN -> openMemo() // 메모 열기
                }
            }
        }
    }

    private fun updateDots() {
        for (i in dots.indices) {
            // 현재 인덱스가 입력된 비밀번호 길이보다 작으면 선택 상태로 표시
            dots[i].isSelected = i < password.length
        }
    }

    private fun newPassword() {
        // 첫 번째 입력
        if (confirmPassword == null) {
            confirmPassword = StringBuilder(password)
            password.clear()
            binding.textView.text = getString(PasswordString.CONFIRM.resId) // "비밀번호 확인"
        } else { // 두 번째 입력~
            if (password.toString() == confirmPassword.toString()) { // 첫 번째 입력과 일치
                PasswordManager.savePassword(safeContext, password.toString()) // 비밀번호 저장

                val message = if (storedPassword.isNullOrEmpty()) {
                    R.string.password_saved // "비밀번호 저장 완료!"
                } else {
                    R.string.password_changed // "비밀번호 변경 완료!"
                }
                ToastUtil.showToast(safeContext, getString(message))
                confirmPassword = null
                requireActivity().supportFragmentManager.popBackStack()
                return
            } else { // 첫 번째 입력과 불일치
                reEnterPassword()
            }
        }
        clearPassword()
    }

    private fun updatePassword() {
        // 저장된 비밀번호와 일치
        if (password.toString() == storedPassword) {
            passwordMode = PasswordMode.NEW // 새 비밀번호 입력 모드로 변경
            binding.textView.text = getString(PasswordString.NEW.resId) // "새 비밀번호 입력"
        } else { // 저장된 비밀번호와 불일치
            reEnterPassword()
        }
        clearPassword()
    }

    private fun lockMemo() {
        // 저장된 비밀번호와 일치
        if (password.toString() == storedPassword) {
            // 메모 잠금 상태 변경
            memoViewModel.updateMemo(memo.copy(isLocked = !memo.isLocked))
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            reEnterPassword()
        }
        clearPassword()
    }

    private fun openMemo() {
        // 저장된 비밀번호와 일치
        if (password.toString() == storedPassword) {
            // PasswordFragment 제거
            requireActivity().supportFragmentManager.popBackStack()

            val memoFragment = MemoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Constants.MEMO, memo)
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, memoFragment)
                .addToBackStack(null)
                .commit()
        } else {
            reEnterPassword()
        }
        clearPassword()
    }

    // 비밀번호 재입력
    private fun reEnterPassword() {
        binding.textView.text = getString(PasswordString.RE_ENTER.resId) // "비밀번호 재입력"
        VibrateUtil.vibrate(safeContext)
    }

    // 비밀번호 초기화
    private fun clearPassword() {
        password.clear()
        updateDots()
        isLocked = false
    }

    private fun setupBtnCancel() {
        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}