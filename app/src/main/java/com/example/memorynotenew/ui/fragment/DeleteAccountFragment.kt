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
import com.example.memorynotenew.databinding.FragmentDeleteAccountBinding
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class DeleteAccountFragment : Fragment() {
    private var _binding: FragmentDeleteAccountBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

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
            val password = edtPw.text.toString().trim()
            val confirmText = edtConfirm.text.toString().trim()
            val requiredText = getString(R.string.delete_account) // "회원탈퇴"

            if (password.isBlank() || confirmText.isBlank()) { // "모든 항목을 입력해주세요."
                requireContext().showToast(getString(R.string.fill_all_fields))
                return
            }
            if (confirmText != requiredText) { // "확인 문구가 일치하지 않습니다."
                requireContext().showToast(getString(R.string.confirm_mismatch))
                return
            }
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                with(currentUser) {
                    email?.let { email ->
                        val credential = EmailAuthProvider.getCredential(email, password)
                        reauthenticate(credential) // 비밀번호 재인증
                            .addOnSuccessListener { // 성공
                                delete() // 계정 삭제
                                    .addOnSuccessListener { // 성공
                                        // "회원탈퇴에 성공했습니다."
                                        requireContext().showToast(getString(R.string.delete_account_success))
                                        requireActivity().supportFragmentManager.popBackStack()
                                    }
                                    .addOnFailureListener { // 실패
                                        // "회원탈퇴에 실패했습니다."
                                        requireContext().showToast(getString(R.string.delete_account_failed))
                                    }
                            }
                            .addOnFailureListener { // 실패
                                // "비밀번호가 틀렸습니다."
                                requireContext().showToast(getString(R.string.incorrect_password))
                            }
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