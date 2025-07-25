package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.databinding.FragmentPasswordBinding

class PasswordFragment : Fragment() {
    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null, 항상 null-safe한 접근 가능
    private lateinit var passwordPurpose: PasswordPurpose

    private lateinit var dots: List<View>

    private var password = StringBuilder()

    private var isLocked = false // 입력 잠금 상태

    companion object {
        private const val PURPOSE = "password_purpose"

        // purpose를 받아 프래그먼트 인스턴스 생성
        fun newInstance(purpose: PasswordPurpose) : PasswordFragment {
            return PasswordFragment().apply {
                // enum 값을 문자열로 변환하여 arguments에 저장
                arguments = Bundle().apply {
                    putString(PURPOSE, purpose.name)
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

        setupKeypad()
        setupBtnCancel()
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
        if (isLocked || password.length >= 4) return

        password.append(number)
        updateDots()

        // 4자리 입력 완료
        if (password.length == 4) {
            isLocked = true // 입력 잠금
            Handler(Looper.getMainLooper()).postDelayed({
                clearPassword()
            }, 500)
        }
    }

    private fun updateDots() {
        for (i in dots.indices) {
            // 현재 인덱스가 입력된 비밀번호 길이보다 작으면 선택 상태로 표시
            dots[i].isSelected = i < password.length
        }
    }

    private fun clearPassword() {
        isLocked = false // 입력 잠금 해제
        password.clear()
        updateDots()
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