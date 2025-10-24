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
import com.example.memorynotenew.databinding.FragmentSignUpBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.util.UUID

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private lateinit var auth: FirebaseAuth

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
        auth = FirebaseAuth.getInstance()

        handleVerification()
        handleSignUp()
    }

    private fun handleVerification() {
        with(binding) {
            btnVerify.setOnClickListener {
                val email = edtEmail.text.toString().trim()

                // 이메일을 입력해주세요.
                if (email.isEmpty()) {
                    requireContext().showToast(getString(R.string.email_enter))
                    return@setOnClickListener
                }
                // 올바른 이메일 형식이 아닙니다.
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    requireContext().showToast(getString(R.string.email_invalid))
                    return@setOnClickListener
                }
                // 임시 사용자 생성 및 인증 메일 발송
                val tempPassword = UUID.randomUUID().toString() // 임시 비밀번호
                    auth.createUserWithEmailAndPassword(email, tempPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val tempUser = auth.currentUser // 임시 사용자
                            tempUser?.sendEmailVerification() // 인증 메일 발송
                                // 인증 메일 발송 완료. 이메일을 확인하세요.
                                ?.addOnCompleteListener {
                                    tvSentEmail.visibility = View.VISIBLE
                                } // 인증 메일 발송 실패
                                ?.addOnFailureListener {
                                    tvSentEmail.text = getString(R.string.email_failed)
                                }
                        } else { // 생성 실패
                            val exception = task.exception
                            if (exception is FirebaseAuthUserCollisionException) {
                                // 이메일이 이미 존재합니다.
                                tvSentEmail.text = getString(R.string.email_exists)
                            } else { // 인증 실패
                                tvSentEmail.text = getString(R.string.verify_failed)
                                requireContext().showToast("${exception?.message}")
                            }
                        }
                    }
            }
        }
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) { // 이메일이 인증되었습니다.
                binding.tvSentEmail.text = getString(R.string.email_verified)
            } else { // 이메일 인증이 완료되지 않았습니다.
                binding.tvSentEmail.text = getString(R.string.email_unverified)
            }
        }
    }

    private fun handleSignUp() {
        with(binding) {
            btnSignUp.setOnClickListener {
                val email = edtEmail.text.toString().trim()
                val password = edtPw.text.toString().trim()
                val confirmPassword = edtPwConfirm.text.toString().trim()

                // 모든 항목을 입력해주세요.
                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    requireContext().showToast(getString(R.string.enter_all))
                    return@setOnClickListener
                }
                // 올바른 이메일 형식이 아닙니다.
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    requireContext().showToast(getString(R.string.email_invalid))
                    return@setOnClickListener
                }
                // 비밀번호는 8자 이상이어야 합니다.
                if (password.length < 8) {
                    requireContext().showToast(getString(R.string.password_invalid))
                    return@setOnClickListener
                }
                // 비밀번호가 일치하지 않습니다.
                if (confirmPassword != password) {
                    requireContext().showToast(getString(R.string.password_mismatch))
                    return@setOnClickListener
                }
                /*
                // 회원가입
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) { // 회원가입 성공
                        val user = auth.currentUser // 현재 생성된 사용자
                        user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) { // 인증 메일 전송 완료
                                requireContext().showToast(getString(R.string.email_verification_sent))
                                requireActivity().supportFragmentManager.popBackStack()
                            } else { // 인증 메일 전송 실패
                                requireContext().showToast(getString(R.string.email_verification_failed))
                            }
                        }
                    } else { // 회원가입 실패
                        requireContext().showToast(getString(R.string.sign_up_failed))
                    }
                }*/
            }
        }
    }

    override fun onResume() {
        super.onResume()

        checkEmailVerification()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}