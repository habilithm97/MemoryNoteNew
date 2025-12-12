package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.USERS
import com.example.memorynotenew.databinding.FragmentDeleteAccountBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeleteAccountFragment : Fragment() {
    private var _binding: FragmentDeleteAccountBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

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
            // 회원탈퇴 버튼
            btnDeleteAccount.setOnClickListener {
                deleteAccount()
            }
        }
    }

    private fun deleteAccount() {
        with(binding) {
            val password = edtPw.text.toString()
            val confirmText = edtConfirm.text.toString()
            val requiredText = getString(R.string.delete_account) // "회원탈퇴"

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
            val user = auth.currentUser ?: return
            val uid = user.uid
            val email = user.email
            val credential = email?.let { EmailAuthProvider.getCredential(it, password) }

            credential?.let {
                // 현재 사용자 재인증
                user.reauthenticate(it)
                    .addOnSuccessListener {
                        // Firestore 사용자 데이터 전체 삭제
                        deleteUserData(uid) { success ->
                            if (!success) return@deleteUserData
                            user.delete() // Auth 사용자 계정 삭제
                                .addOnSuccessListener {
                                    // "회원탈퇴에 성공했습니다."
                                    requireContext().showToast(getString(R.string.delete_account_success))
                                    requireActivity().supportFragmentManager.popBackStack()
                                }
                                .addOnFailureListener {
                                    // "회원탈퇴에 실패했습니다."
                                    requireContext().showToast(getString(R.string.delete_account_failed))
                                }
                        }
                    }
                    .addOnFailureListener {
                        // "비밀번호가 틀렸습니다."
                        requireContext().showToast(getString(R.string.incorrect_password))
                    }
            }
        }
    }

    private fun deleteUserData(uid: String, onComplete: (Boolean) -> Unit) {
        val userDoc = db.collection(USERS).document(uid) // 사용자 문서
        val memoCollection = userDoc.collection(MEMO) // 메모 컬렉션

        memoCollection.get() // 메모 컬렉션 하위 문서 가져오기
            .addOnSuccessListener {
                val batch = db.batch() // 여러 문서를 한 번에 삭제하기 위한 batch

                for (doc in it.documents) { // 조회된 모든 메모 문서들 반복
                    batch.delete(doc.reference) // batch에 각 메모 문서 삭제 작업 추가
                }
                batch.commit() // batch 삭제 실행
                    .addOnSuccessListener {
                        userDoc.delete() // user/{uid} 사용자 문서 삭제
                            .addOnSuccessListener {
                                onComplete(true)
                            }
                            .addOnFailureListener {
                                onComplete(false)
                            }
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}