package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentForgotPasswordBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            // 소프트 키보드 높이 만큼 linearLayout 하단 패딩 적용
            ViewCompat.setOnApplyWindowInsetsListener(linearLayout) { linearLayout, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                linearLayout.updatePadding(bottom = imeInsets.bottom)
                insets
            }
            // 비밀번호 재설정 요청 버튼
            btnVertify.setOnClickListener {
                handleResetRequest()
            }
        }
    }

    private fun handleResetRequest() {
        with(binding) {
            val email = edtEmail.text.toString()

            // "올바른 이메일 형식이 아닙니다."
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                requireContext().showToast(getString(R.string.invalid_email_format))
                return
            }
            // 비밀번호 재설정 요청
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        // "비밀번호 재설정 메일을 보냈습니다. 이메일을 확인해주세요."
                        requireContext().showToast(getString(R.string.password_reset_email_sent))
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Log.e("ForgotPasswordFragment", "Failed to send password reset email.", it.exception)

                        val message = when (it.exception) {
                            // "네트워크 오류가 발생했습니다."
                            is FirebaseNetworkException -> getString(R.string.network_error)
                            // "비밀번호 재설정에 실패했습니다."
                            else -> getString(R.string.password_reset_failed)
                        }
                        requireContext().showToast(message)
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}