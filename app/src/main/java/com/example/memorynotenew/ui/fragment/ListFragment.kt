package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memorynotenew.R
import com.example.memorynotenew.adapter.MemoAdapter
import com.example.memorynotenew.constants.Constants
import com.example.memorynotenew.databinding.FragmentListBinding
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.viewmodel.MemoViewModel

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null // nullable
    private val binding get() = _binding!! // non-null, 항상 null-safe한 접근 가능
    private val memoViewModel: MemoViewModel by viewModels()
    private lateinit var memoAdapter: MemoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMemoAdapter()

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
            memoViewModel.getAllMemos.observe(viewLifecycleOwner) { memoList ->
                memoAdapter.apply {
                    submitMemoList(memoList)
                    if (itemCount > 0) {
                        recyclerView.smoothScrollToPosition(itemCount - 1)
                    }
                }
            }
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                // 검색어 입력 시 호출
                override fun onQueryTextChange(newText: String?): Boolean {
                    memoAdapter.filterList(newText ?: "") // null이면 "" 사용
                    return true
                }
                // 키보드 검색 버튼 클릭 시 호출
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
            })
        }
    }

    private fun setupMemoAdapter() {
        memoAdapter = MemoAdapter(
            onItemClick = { memo ->
                val memoFragment = MemoFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(Constants.MEMO, memo)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, memoFragment)
                    .addToBackStack(null)
                    .commit()
            }, onItemLongClick = { memo ->
                showDeleteDialog(memo)
            }
        )
    }

    private fun showDeleteDialog(memo: Memo) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_dialog_title)) // 삭제하기
            .setMessage(getString(R.string.delete_dialog_msg)) // 선택한 메모를 삭제할까요?
            .setNegativeButton(getString(R.string.cancel), null) // 취소
            .setPositiveButton(getString(R.string.delete)) { dialog, _ -> // 삭제
                memoViewModel.deleteMemo(memo)
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}