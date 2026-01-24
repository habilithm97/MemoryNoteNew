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
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.USERS
import com.example.memorynotenew.databinding.FragmentDeleteAccountBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class DeleteAccountFragment : Fragment() {
    private var _binding: FragmentDeleteAccountBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // 구글 로그인 재인증 결과를 받기 위한 ActivityResultLauncher
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        // 결과가 비정상 시 처리 중단
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        // Intent에서 구글 로그인 계정을 가져오는 작업 생성
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            // 구글 로그인 계정 가져오기 (실패 시 ApiException 발생)
            val account = task.getResult(ApiException::class.java)

            // 구글 로그인 계정의 id 토큰으로 Firebase 인증용 Credential 생성
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val user = auth.currentUser ?: return@registerForActivityResult
            val uid = user.uid

            // Credential로 Firebase 사용자 재인증
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    Log.d("DeleteAccountFragment", "구글 로그인 계정 재인증 성공")
                    proceedDeleteAccount(user, uid) // 회원탈퇴 진행
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteAccountFragment", "구글 로그인 계정 재인증 실패", e)
                    // "Google 계정 재인증에 실패했습니다."
                    requireContext().showToast(getString(R.string.google_reauth_failed))
                }
        } catch (e: ApiException) {
            Log.e("DeleteAccountFragment", "구글 로그인 실패", e)
            // "Google 로그인에 실패했습니다."
            requireContext().showToast(getString(R.string.google_sign_in_failed))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeleteAccountBinding.inflate(inflater, container, false)
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
            setupUI() // 로그인 방식을 분기하여 UI 설정

            // 회원탈퇴 버튼
            btnDeleteAccount.setOnClickListener {
                deleteAccount()
            }
        }
    }

    private fun setupUI() {
        val isGoogleUser = isGoogleUser() // 로그인 방식 확인

        with(binding) {
            if (isGoogleUser) {
                pwLayout.visibility = View.GONE
                tvGuide.visibility = View.VISIBLE
            }
        }
    }

    private fun isGoogleUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { // 인증 제공자 목록 중
            it.providerId == "google.com" // 구글 제공자가 있으면 true
        }
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        if (isGoogleUser()) {
            deleteGoogleUser() // 구글 로그인 회원탈퇴
        } else {
            deleteEmailUser(user, uid) // 이메일 로그인 회원탈퇴
        }
    }

    private fun deleteGoogleUser() {
        val confirmText = binding.edtConfirm.text.toString()
        val requiredText = getString(R.string.delete_account)

        // "확인 문구를 입력해주세요."
        if (confirmText.isBlank()) {
            requireContext().showToast(getString(R.string.confirm_blank))
            return
        }
        // "확인 문구가 일치하지 않습니다."
        if (confirmText != requiredText) {
            requireContext().showToast(getString(R.string.confirm_mismatch))
            return
        }
        // 구글 로그인 옵션 설정
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Firebase 인증용 id 토큰 요청
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail() // 사용자 이메일 요청
            .build()

        // 설정한 옵션으로 구글 로그인 클라이언트 생성
        val googleSignInClient = GoogleSignIn.getClient(requireContext(), googleSignInOptions)
        // 구글 로그인 화면 실행, 결과는 resultLauncher에서 처리
        resultLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun deleteEmailUser(user: FirebaseUser, uid: String) {
        with(binding) {
            val password = edtPw.text.toString()
            val confirmText = edtConfirm.text.toString()
            val requiredText = getString(R.string.delete_account)

            // "모든 항목을 입력해주세요."
            if (password.isBlank() || confirmText.isBlank()) {
                requireContext().showToast(getString(R.string.fill_all_fields))
                return
            }
            // "확인 문구가 일치하지 않습니다."
            if (confirmText != requiredText) {
                requireContext().showToast(getString(R.string.confirm_mismatch))
                return
            }
            val email = user.email ?: return
            // 이메일과 비밀번호로 Firebase 인증용 Credential 생성
            val credential = EmailAuthProvider.getCredential(email, password)
            val uid = user.uid

            // Credential로 Firebase 사용자 재인증
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    Log.d("DeleteAccountFragment", "이메일 재인증 성공")
                    proceedDeleteAccount(user, uid) // 회원탈퇴 진행
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteAccountFragment", "이메일 재인증 실패", e)
                    // "이메일 재인증에 실패했습니다."
                    requireContext().showToast(getString(R.string.email_reauth_failed))
                }
        }
    }

    private fun proceedDeleteAccount(user: FirebaseUser, uid: String) {
        // Firestore 사용자 데이터 삭제
        deleteUserData(uid) { success ->
            if (!success) {
                Log.d("DeleteAccountFragment", "Firebase 사용자 데이터 삭제 실패")
                // "회원탈퇴 처리 중 오류가 발생했습니다. 다시 시도해주세요."
                requireContext().showToast(getString(R.string.delete_account_failed_try_again))
                return@deleteUserData
            }
            Log.d("DeleteAccountFragment", "Firebase 사용자 데이터 삭제 성공")

            // Firebase Auth 계정 삭제
            user.delete()
                .addOnSuccessListener {
                    Log.d("DeleteAccount", "Firebase Auth 계정 삭제 성공")
                    // "회원탈퇴에 성공했습니다."
                    requireContext().showToast(getString(R.string.delete_account_success))
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteAccountFragment", "Firebase Auth 계정 삭제 실패", e)
                    // "회원탈퇴에 실패했습니다."
                    requireContext().showToast(getString(R.string.delete_account_failed))
                }
        }
    }

    private fun deleteUserData(uid: String, onComplete: (Boolean) -> Unit) {
        val userDoc = db.collection(USERS).document(uid) // 사용자 문서
        val memoCollection = userDoc.collection(MEMO) // 메모 컬렉션

        memoCollection.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.d("DeleteAccountFragment", "삭제할 메모 없음")
                } else {
                    Log.d("DeleteAccountFragment", "삭제할 메모 개수: ${snapshot.size()}")
                }
                val batch = db.batch() // 여러 문서를 한 번에 삭제하기 위한 batch

                snapshot.documents.forEach { doc ->
                    Log.d("DeleteAccountFragment", "삭제할 메모: ${doc.id}")
                    batch.delete(doc.reference) // batch에 각 메모 문서 삭제 작업 추가
                }
                batch.delete(userDoc) // 사용자 문서도 batch에 포함 → atomic 삭제

                batch.commit() // batch 삭제 실행
                    .addOnSuccessListener {
                        Log.d("DeleteAccountFragment", "Firestore 사용자 데이터 삭제 성공")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeleteAccountFragment", "Firestore 사용자 데이터 삭제 실패", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteAccountFragment", "메모 컬렉션 조회 실패", e)
                onComplete(false)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}