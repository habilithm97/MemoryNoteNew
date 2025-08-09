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
import com.example.memorynotenew.common.PasswordStep
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
    // ViewModel (Fragment 생성과 함께 초기화)
    private val memoViewModel: MemoViewModel by viewModels()

    // View Binding (onCreateView ~ onDestroyView)
    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null, 항상 null-safe한 접근 가능

    // Argument
    private val memo: Memo by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(Constants.MEMO, Memo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(Constants.MEMO)!!
        }
    }
    // 상태
    private lateinit var passwordPurpose: PasswordPurpose
    private lateinit var currentStep: PasswordStep
    private var isInputLocked = false // 입력 잠금 상태
    private var isLocked = false // 메모 잠금 상태

    // UI
    private lateinit var dots: List<View>

    // 데이터
    private var storedPassword: String? = null // 저장된 비밀번호
    private var password = StringBuilder()
    private var firstInput: StringBuilder? = null // 첫 번째로 입력한 비밀번호

    companion object {
        private const val PURPOSE = "password_purpose"

        // purpose를 받아 프래그먼트 인스턴스 생성
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

        val name = arguments?.getString(PURPOSE)
            ?: throw IllegalArgumentException("PasswordFragment is required")
        passwordPurpose = PasswordPurpose.valueOf(name) // 문자열을 enum 값으로 변환
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

        storedPassword = PasswordManager.getSavedPassword(requireContext())

        currentStep = if (storedPassword.isNullOrEmpty()) {
            PasswordStep.NEW
        } else {
            PasswordStep.ENTER
        }
        setupTitle()
        setupKeypad()
        setupBtnCancel()
    }

    private fun setupTitle() {
        val title = when (currentStep) {
            PasswordStep.NEW -> getString(PasswordString.NEW.resId)
            PasswordStep.ENTER -> getString(PasswordString.ENTER.resId)
        }
        binding.textView.text = title
    }

    private fun setupKeypad() {
        with(binding) {
            dots = listOf(dot1, dot2, dot3, dot4)

            val buttons = listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)
            for (btn in buttons) {
                btn.setOnClickListener {
                    onPwKeyPressed(btn.text.toString())
                }
            }
        }
    }

    private fun onPwKeyPressed(number: String) {
        // 입력 처리 중이거나 4자리 이상이면 추가 입력 제한
        if (isInputLocked || password.length >= 4) return

        password.append(number)
        updateDots()

        // 4자리 입력 완료
        if (password.length == 4) {
            isInputLocked = true // 입력 잠금

            // 액/프 소멸 시 코루틴 자동 취소 (메모리 누수 방지)
            lifecycleScope.launch {
                delay(500)
                when (passwordPurpose) {
                    PasswordPurpose.SETTINGS -> {
                        when (currentStep) {
                            PasswordStep.NEW -> newPassword()
                            PasswordStep.ENTER -> updatePassword()
                        }
                    }
                    PasswordPurpose.LOCK -> lockMemo()
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
        if (firstInput == null) { // 첫 번째 입력
            firstInput = StringBuilder(password) // 첫 번째 입력 저장
            password.clear()
            binding.textView.text = getString(PasswordString.CONFIRM.resId)
        } else { // 두 번째 입력 이후
            if (firstInput.toString() == password.toString()) { // 첫 번째 입력과 일치
                PasswordManager.savePassword(requireContext(), password.toString()) // 비밀번호 저장

                val message = if (storedPassword.isNullOrEmpty()) {
                    R.string.password_saved // 비밀번호 저장 완료!
                } else {
                    R.string.password_changed // 비밀번호 변경 완료!
                }
                ToastUtil.showToast(requireContext(), getString(message))
                firstInput = null
                requireActivity().supportFragmentManager.popBackStack()
                return
            } else { // 첫 번째 입력과 불일치
                reEnterPassword()
            }
        }
        clearPassword()
    }

    private fun updatePassword() {
        // 저장된 비밀번호와 일치 -> 새 비밀번호 입력 단계로 이동
        if (storedPassword == password.toString()) {
            currentStep = PasswordStep.NEW
            binding.textView.text = getString(PasswordString.NEW.resId)
        } else { // 저장된 비밀번호와 불일치 -> 재입력 요청
            reEnterPassword()
        }
        clearPassword()
    }

    private fun lockMemo() {
        // 저장된 비밀번호와 일치
        if (storedPassword == password.toString()) {
            if (memo.isLocked) { // 메모가 잠겨 있으면 -> MemoFragment로 이동
                val memoFragment = MemoFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(Constants.MEMO, memo)
                    }
                }
                parentFragmentManager.apply {
                    popBackStack() // PasswordFragment 제거
                    beginTransaction()
                        .replace(R.id.container, memoFragment)
                        .addToBackStack(null)
                        .commit()
                }
            } else { // 메모가 잠겨 있지 않으면 -> ListFragment로 이동
                memoViewModel.updateMemo(memo.copy(isLocked = true))
                requireActivity().supportFragmentManager.popBackStack()
            }
        } else { // 저장된 비밀번호와 불일치 -> 재입력 요청
            reEnterPassword()
        }
        clearPassword()
    }

    private fun reEnterPassword() {
        binding.textView.text = getString(PasswordString.RE_ENTER.resId)
        VibrateUtil.vibrate(requireContext())
    }

    private fun clearPassword() {
        password.clear()
        updateDots()
        isInputLocked = false
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