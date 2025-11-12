package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentSignInBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private val memoViewModel: MemoViewModel by viewModels()

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

    private fun handleSignIn() {
        with(binding) {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // 모든 항목을 입력해주세요.
            if (email.isEmpty() || password.isEmpty()) {
                requireContext().showToast(getString(R.string.fill_all_fields))
                return
            }
            // 올바른 이메일 형식이 아닙니다.
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                requireContext().showToast(getString(R.string.invalid_email_format))
                return
            }
            showProgress(true)

            // 로그인
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    showProgress(false)

                    if (task.isSuccessful) { // 성공
                        val user = auth.currentUser

                        if (user != null && user.isEmailVerified) { // 이메일 인증 완료
                            requireActivity().supportFragmentManager.popBackStack()
                            memoViewModel.onUserChanged()
                        } else { // 이메일 인증이 필요합니다.
                            textView.visibility = View.VISIBLE
                        }
                    } else { // 실패
                        val message = when (task.exception) {
                            // 등록된 이메일이 없습니다.
                            is FirebaseAuthInvalidUserException -> getString(R.string.email_not_found)
                            // 비밀번호가 틀렸습니다.
                            is FirebaseAuthInvalidCredentialsException -> getString(R.string.incorrect_password)
                            // 네트워크 오류가 발생했습니다.
                            is FirebaseNetworkException -> getString(R.string.network_error)
                            else -> getString(R.string.sign_in_failed) // 로그인에 실패했습니다.
                        }
                        requireContext().showToast(message)
                    }
                }
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}