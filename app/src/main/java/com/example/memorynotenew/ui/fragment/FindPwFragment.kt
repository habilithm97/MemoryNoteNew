package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentFindPwBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class FindPwFragment : Fragment() {
    private var _binding: FragmentFindPwBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        with(binding) {
            // 소프트 키보드 높이 만큼 linearLayout 하단 패딩 적용
            ViewCompat.setOnApplyWindowInsetsListener(linearLayout) { linearLayout, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                linearLayout.updatePadding(bottom = imeInsets.bottom)
                insets
            }
            // 인증하기 버튼
            btnVertify.setOnClickListener {
                handleFindPassword()
            }
        }
    }

    private fun handleFindPassword() {
        with(binding) {
            val email = edtEmail.text.toString().trim()

            // 올바른 이메일 형식이 아닙니다.
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                requireContext().showToast(getString(R.string.invalid_email_format))
                return
            }
            // 비밀번호 재설정 요청
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) { // 성공
                        // 비밀번호 재설정 메일을 보냈습니다. 이메일을 확인해주세요.
                        requireContext().showToast(getString(R.string.password_reset_email_sent))
                        requireActivity().supportFragmentManager.popBackStack()
                    } else { // 실패
                        val message = when (task.exception) {
                            // 등록된 이메일이 없습니다.
                            is FirebaseAuthInvalidUserException -> getString(R.string.email_not_found)
                            // 네트워크 오류가 발생했습니다.
                            is FirebaseNetworkException -> getString(R.string.network_error)
                            // 비밀번호 재설정에 실패했습니다.
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