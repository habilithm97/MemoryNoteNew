package com.example.memorynotenew.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.FragmentSignInBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // 구글 로그인 결과 처리 ActivityResultLauncher
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        // 결과가 비정상일 경우 처리 중단
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        // Intent에서 구글 로그인 계정을 가져오는 작업 생성
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            // 구글 로그인 계정 가져오기 (실패 시 ApiException 발생)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                // 구글 로그인 계정 id 토큰으로 Firebase 인증
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.w("SignInFragment",
                    "구글 로그인 실패 : idToken이 null 입니다. (계정은 가져왔으나 토큰 발급 실패)")
                // "Google 로그인에 실패했습니다."
                requireContext().showToast(getString(R.string.google_sign_in_failed))
            }
        } catch (e: ApiException) {
            Log.e("SignInFragment",
                "구글 로그인 실패 : 계정 정보를 가져오는 중 ApiException 발생", e)
            // "Google 로그인에 실패했습니다."
            requireContext().showToast(getString(R.string.google_sign_in_failed))
        }
    }

    private val googleSignInClient by lazy {
        // 구글 로그인 옵션 설정
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Firebase 인증용 id 토큰 요청
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail() // 사용자 이메일 정보 요청
            .build()
        // 설정한 옵션으로 구글 로그인 클라이언트 생성
        GoogleSignIn.getClient(requireContext(), googleSignInOptions)
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
            // 소프트 키보드 높이 만큼 rootLayout 하단 패딩 적용
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { linearLayout, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                linearLayout.updatePadding(bottom = imeInsets.bottom)
                insets
            }
            // 구글 로그인 버튼
            btnSignInWithGoogle.setOnClickListener {
                signInWithGoogle()
            }
        }
    }

    private fun signInWithGoogle() {
        /** 구글 로그인 시 계정 선택 화면 항시 표시 */
        // 기존에 로그인된 계정 제거
        googleSignInClient.signOut().addOnCompleteListener {
            // 구글 로그인 화면을 띄우기 위한 Intent 생성
            val signInIntent = googleSignInClient.signInIntent
            // 구글 로그인 액티비티 실행
            resultLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgress(true)

        // id 토큰을 사용해 Firebase 인증용 Credential 객체 생성
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        // Firebase 인증 시도 (구글 계정과 연결)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    Log.e("SignInFragment", "" +
                            "Firebase 인증 실패 : 구글 계정과 연결하는 과정에서 오류 발생", task.exception)
                    // "Google 로그인에 실패했습니다."
                    requireContext().showToast(getString(R.string.google_sign_in_failed))
                }
            }
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}