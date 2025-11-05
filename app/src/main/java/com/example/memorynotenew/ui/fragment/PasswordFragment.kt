package com.example.memorynotenew.ui.fragment

import  android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.COUNT
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.MEMOS
import com.example.memorynotenew.common.Constants.PURPOSE
import com.example.memorynotenew.common.PasswordInput
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.common.PasswordString
import com.example.memorynotenew.databinding.FragmentPasswordBinding
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.utils.PasswordManager
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.utils.VibrateUtil
import com.example.memorynotenew.viewmodel.MemoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordFragment : Fragment() {
    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private var password = StringBuilder()
    private var isInputLocked = false
    private var confirmingPassword: StringBuilder? = null
    private var storedPassword: String? = null
    private var deleteCount: Int = 1

    private lateinit var passwordPurpose: PasswordPurpose
    private lateinit var passwordInput: PasswordInput
    private val memoViewModel: MemoViewModel by viewModels()
    
    private val dots: List<View> by lazy {
        with(binding) { listOf(dot1, dot2, dot3, dot4) }
    }

    private val memo: Memo? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(MEMO, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(MEMO)
        }
    }

    private val memos: List<Memo>? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelableArrayList(MEMOS, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelableArrayList(MEMOS)
        }
    }

    companion object {
        // 프래그먼트 재생성 시에도 안전하게 인자를 전달 및 복원
        fun newInstance(purpose: PasswordPurpose,
                        memo: Memo? = null,
                        deleteCount: Int = 1, // 기본값 1
                        memos: ArrayList<Memo>? = null
        ) : PasswordFragment {
            return PasswordFragment().apply {
                // enum 값을 문자열로 변환하여 arguments에 저장
                arguments = Bundle().apply {
                    putString(PURPOSE, purpose.name)

                    memo?.let { putParcelable(MEMO, it) }
                    memos?.let { putParcelableArrayList(MEMOS, it) }

                    // DELETE일 때만 deleteCount 전달
                    if (purpose == PasswordPurpose.DELETE) {
                        putInt(COUNT, deleteCount)
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

        // DELETE일 때만 deleteCount 가져오기
        deleteCount = if (passwordPurpose == PasswordPurpose.DELETE) {
            // 기본값 1, arguments가 null이면 1로 안전하게 처리
            arguments?.getInt(COUNT) ?: 1
        } else { // DELETE가 아니면 1
            1
        }
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

        storedPassword = PasswordManager.getPassword(binding.root.context)

        passwordInput = if (storedPassword.isNullOrEmpty()) {
            PasswordInput.NEW // 새 비밀번호 입력 모드
        } else {
            PasswordInput.ENTER // 기존 비밀번호 입력 모드
        }
        setupSubTitle()
        setupKeypad()
        setupBtnCancel()
    }

    private fun setupSubTitle() {
        val subTitle = when (passwordInput) {
            PasswordInput.NEW -> getString(PasswordString.NEW.resId) // 새 비밀번호 입력
            PasswordInput.ENTER -> getString(PasswordString.ENTER.resId) // 기존 비밀번호 입력
        }
        binding.textView.text = subTitle
    }

    private fun setupKeypad() {
        with(binding) {
            listOf(btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0)
                .forEach { btn ->
                    btn.setOnClickListener {
                        onPwKeyPressed(btn.text.toString())
                    }
                }
        }
    }

    private fun onPwKeyPressed(number: String) {
        // 4자리 이상이거나 입력 처리 중이면 추가 입력 제한
        if (password.length >= 4 || isInputLocked) return

        password.append(number)
        updateDots()

        // 4자리 입력 완료
        if (password.length == 4) {
            isInputLocked = true // 입력 잠금

            // 화면 소멸 시 코루틴 자동 취소 (메모리 누수 방지)
            lifecycleScope.launch {
                delay(500)
                when (passwordPurpose) {
                    PasswordPurpose.SETTING -> { // 설정
                        when (passwordInput) {
                            PasswordInput.NEW -> newPassword() // 새 비밀번호 저장
                            PasswordInput.ENTER -> updatePassword() // 비밀번호 변경
                        }
                    }
                    PasswordPurpose.LOCK -> toggleMemoLock() // 메모 잠금 및 잠금 해제
                    PasswordPurpose.OPEN -> openMemo() // 메모 열기
                    PasswordPurpose.DELETE -> deleteMemo() // 메모 삭제
                }
            }
        }
    }

    private fun updateDots() {
        // 현재 인덱스가 입력된 비밀번호 길이보다 작으면 선택 상태로 표시
        dots.forEachIndexed { index, dot ->
            dot.isSelected = index < password.length
        }
    }

    private fun newPassword() {
        with(binding) {
            // 첫 번째 입력
            if (confirmingPassword == null) {
                confirmingPassword = StringBuilder(password)
                password.clear()
                textView.text = getString(PasswordString.CONFIRM.resId) // 비밀번호 확인
            } else { // 두 번째 입력~
                if (password.toString() == confirmingPassword.toString()) { // 첫 번째 입력과 일치
                    PasswordManager.savePassword(root.context, password.toString()) // 비밀번호 저장

                    val message = if (storedPassword.isNullOrEmpty()) {
                        R.string.lock_password_saved // 비밀번호 저장 완료!
                    } else {
                        R.string.lock_password_changed // 비밀번호 변경 완료!
                    }
                    requireContext().showToast(getString(message))
                    confirmingPassword = null
                    requireActivity().supportFragmentManager.popBackStack()
                    return
                } else { // 첫 번째 입력과 불일치
                    reEnterPassword()
                }
            }
            clearPassword()
        }
    }

    private fun updatePassword() {
        if (password.toString() == storedPassword) { // 저장된 비밀번호와 일치
            passwordInput = PasswordInput.NEW
            binding.textView.text = getString(PasswordString.NEW.resId) // 새 비밀번호 입력
        } else { // 저장된 비밀번호와 불일치
            reEnterPassword()
        }
        clearPassword()
    }

    private fun toggleMemoLock() {
        if (password.toString() == storedPassword) { // 저장된 비밀번호와 일치
            // 메모 잠금 상태 변경
            memo?.let { memoViewModel.updateMemo(it.copy(isLocked = !it.isLocked)) }
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            reEnterPassword()
        }
        clearPassword()
    }

    private fun openMemo() {
        if (password.toString() == storedPassword) { // 저장된 비밀번호와 일치
            // PasswordFragment 제거
            requireActivity().supportFragmentManager.popBackStack()

            val memoFragment = MemoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(MEMO, memo)
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

    private fun deleteMemo() {
        if (password.toString() == storedPassword) { // 저장된 비밀번호와 일치
            memos?.let {  // 다중 삭제
                it.forEach { memo -> memoViewModel.moveMemoToTrash(memo) }
            } ?: memo?.let { // 단일 삭제
                memoViewModel.moveMemoToTrash(it)
            }
            requireContext().showToast(getString(R.string.delete_memo_result, deleteCount))
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            reEnterPassword()
        }
        clearPassword()
    }

    // 비밀번호 재입력
    private fun reEnterPassword() {
        binding.textView.text = getString(PasswordString.RE_ENTER.resId)
        VibrateUtil.vibrate(requireContext())
    }

    // 비밀번호 초기화
    private fun clearPassword() {
        password.clear()
        updateDots()
        isInputLocked = false
    }

    private fun setupBtnCancel() {
        binding.btnCancel.setOnClickListener {
            if (password.isEmpty()) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                password.deleteAt(password.length - 1)
                updateDots()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}