package com.example.memorynotenew.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memorynotenew.R
import com.example.memorynotenew.adapter.MemoAdapter
import com.example.memorynotenew.databinding.FragmentListBinding

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null // nullable
    private val binding get() = _binding!! // non-null, 항상 null-safe한 접근 가능
    private val memoAdapter by lazy { MemoAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            recyclerView.apply {
                adapter = memoAdapter
                layoutManager = LinearLayoutManager(requireContext()).apply {
                    reverseLayout = true
                    stackFromEnd = true
                }
                setHasFixedSize(true) // 아이템 크기 고정 -> 성능 최적화
            }
            fab.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, MemoFragment())
                    .addToBackStack(null) // 백 스택에 추가
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}