package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentSignInBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
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
            // 회원가입 버튼
            btnSignUp.setOnClickListener {
                replaceFragment(SignUpFragment())
            }
            // 로그인 버튼
            btnSignIn.setOnClickListener {
                handleSignIn()
            }
            this@SignInFragment.progressBar = progressBar

            // 비밀번호 찾기 버튼
            btnForgotPw.setOnClickListener {
                replaceFragment(FindPwFragment())
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleSignIn() {
        with(binding) {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            // "모든 항목을 입력해주세요."
            if (email.isEmpty() || password.isEmpty()) {
                requireContext().showToast(getString(R.string.fill_all_fields))
                return
            }
            // "올바른 이메일 형식이 아닙니다."
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                requireContext().showToast(getString(R.string.invalid_email_format))
                return
            }
            showProgress(true)

            // 로그인
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    showProgress(false)

                    if (it.isSuccessful) {
                        val user = auth.currentUser

                        if (user != null && user.isEmailVerified) {
                            requireActivity().supportFragmentManager.popBackStack()
                        } else { // "이메일 인증이 필요합니다."
                            textView.visibility = View.VISIBLE
                        }
                    } else {
                        Log.e("SignInFragment", "Sign in failed.", it.exception)

                        val message = when (it.exception) {
                            // "네트워크 오류가 발생했습니다."
                            is FirebaseNetworkException -> getString(R.string.network_error)
                            // "등록되지 않은 이메일이거나 비밀번호가 틀렸습니다."
                            else -> getString(R.string.sign_in_failed)
                        }
                        requireContext().showToast(message)
                    }
                }
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}