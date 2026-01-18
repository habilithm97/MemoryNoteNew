package com.example.memorynotenew.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentSignInBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var progressBar: ProgressBar

    // 구글 로그인 결과 처리 ActivityResultLauncher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        // 구글 로그인 액티비티 정상 종료 여부 확인
        if (result.resultCode == Activity.RESULT_OK) {
            // result에서 GoogleSignInAccount를 가져오는 task 생성
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Task에서 GoogleSignInAccount 객체를 가져옴 (실패 시 ApiException 발생)
                val account = task.getResult(ApiException::class.java)
                // 구글 계정의 id 토큰을 사용해 Firebase 인증 수행
                //firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("SignInFragment", "Google sign in failed", e)
                // "Google 로그인에 실패했습니다."
                requireContext().showToast(getString(R.string.google_sign_in_failed))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            // 소프트 키보드 높이 만큼 mainLinearLayout 하단 패딩 적용
            ViewCompat.setOnApplyWindowInsetsListener(mainLinearLayout) { linearLayout, insets ->
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
                signIn()
            }
            this@SignInFragment.progressBar = progressBar

            // 인증 메일 재발송 버튼
            btnResendEmail.setOnClickListener {
                resendEmail()
            }
            // 비밀번호 찾기 버튼
            btnForgotPassword.setOnClickListener {
                replaceFragment(ForgotPasswordFragment())
            }
            // 구글 로그인 버튼
            btnGoogleSignIn.setOnClickListener {
                //signInWithGoogle()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun signIn() {
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
                        } else {
                            subLinearLayout.visibility = View.VISIBLE
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

    private fun resendEmail() {
        val user = auth.currentUser

        user?.sendEmailVerification()
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    // "인증 메일을 보냈습니다. 이메일을 확인해주세요."
                    requireContext().showToast(getString(R.string.verification_email_sent))
                } else {
                    Log.e("SignInFragment", "Failed to resend verification email", it.exception)
                    // "인증 메일 발송에 실패했습니다."
                    requireContext().showToast(getString(R.string.verification_email_failed))
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