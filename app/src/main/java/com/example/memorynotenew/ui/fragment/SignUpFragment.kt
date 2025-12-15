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
import com.example.memorynotenew.databinding.FragmentSignUpBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 소프트 키보드 높이 만큼 rootLayout 하단 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { rootLayout, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            rootLayout.updatePadding(bottom = imeInsets.bottom)
            insets
        }
        handleSignUp()
    }

    private fun handleSignUp() {
        with(binding) {
            btnSignUp.setOnClickListener {
                val email = edtEmail.text.toString()
                val password = edtPw.text.toString()

                // 영문+숫자 조합 8~16자
                val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")
                val confirmPassword = edtPwConfirm.text.toString()

                // "모든 항목을 입력해주세요."
                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    requireContext().showToast(getString(R.string.fill_all_fields))
                    return@setOnClickListener
                }
                // "올바른 이메일 형식이 아닙니다."
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    requireContext().showToast(getString(R.string.invalid_email_format))
                    return@setOnClickListener
                }
                // 비밀번호 조건이 맞지 않습니다.
                if (!passwordRegex.matches(password)) {
                    requireContext().showToast(getString(R.string.invalid_password))
                    return@setOnClickListener
                }
                // "비밀번호가 일치하지 않습니다."
                if (confirmPassword != password) {
                    requireContext().showToast(getString(R.string.password_mismatch))
                    return@setOnClickListener
                }
                // 회원가입
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val user = auth.currentUser

                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { emailTask ->
                                    if (emailTask.isSuccessful) { // "인증 메일을 보냈습니다. 이메일을 확인해주세요."
                                        requireContext().showToast(getString(R.string.verification_email_sent))
                                        requireActivity().supportFragmentManager.popBackStack()
                                    } else { // "인증 메일 발송에 실패했습니다."
                                        Log.e("SignUpFragment", "Failed to send verification email", emailTask.exception)
                                        requireContext().showToast(getString(R.string.verification_email_failed))
                                        requireActivity().supportFragmentManager.popBackStack()
                                    }
                                }
                        } else {
                            Log.e("SignUpFragment", "Sign up failed.", it.exception)

                            val message = when (it.exception) {
                                is FirebaseAuthUserCollisionException -> // "이메일이 이미 존재합니다."
                                    getString(R.string.email_already_exists)
                                is FirebaseAuthWeakPasswordException -> // "비밀번호의 보안 수준이 약합니다."
                                    getString(R.string.weak_password)
                                else -> getString(R.string.sign_up_failed) // "회원가입에 실패했습니다."
                            }
                            requireContext().showToast(message)
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}