package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.databinding.FragmentPasswordBinding

class PasswordFragment : Fragment() {
    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null, 항상 null-safe한 접근 가능

    private lateinit var passwordPurpose: PasswordPurpose

    companion object {
        private const val PURPOSE = "password_purpose"

        // purpose를 받아 프래그먼트 인스턴스 생성
        fun newInstance(purpose: PasswordPurpose) : PasswordFragment {
            return PasswordFragment().apply {
                // enum 값을 문자열로 변환하여 arguments에 저장
                arguments = Bundle().apply {
                    putString(PURPOSE, purpose.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = arguments?.getString(PURPOSE)
            ?: throw IllegalArgumentException("PasswordFragment is required")
        // 문자열을 enum 값으로 변환
        passwordPurpose = PasswordPurpose.valueOf(name)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}