package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.memorynotenew.databinding.FragmentTrashBinding

class TrashFragment : Fragment() {
    private var _binding: FragmentTrashBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}