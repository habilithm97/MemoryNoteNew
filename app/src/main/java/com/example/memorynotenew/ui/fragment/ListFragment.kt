package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.R
import com.example.memorynotenew.adapter.MemoAdapter
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.common.PopupAction
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

        // 키보드(IME) 인셋에 따라 RecyclerView 하단 패딩 조정
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()) // 키보드 크기 가져오기
            view.updatePadding(bottom = imeInsets.bottom) // 키보드 높이만큼 하단 패딩 적용
            insets
        }
        setupAdapter()
        setupRecyclerView()
        setupObserver()
        setupSearchView()
        setupFabAdd()
        setupFabScroll()
    }

    private fun setupAdapter() {
        memoAdapter = MemoAdapter(
            onItemClick = { memo ->
                binding.searchView.setQuery("", false) // 검색어 초기화

                val memoFragment = MemoFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(Constants.MEMO, memo)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, memoFragment)
                    .addToBackStack(null)
                    .commit()
            }, onItemLongClick = { memo, popupAction ->
                when (popupAction) {
                    PopupAction.DELETE ->
                        showDeleteDialog(memo)
                    PopupAction.LOCK -> {
                        val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.LOCK)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.container, passwordFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        )
    }

    private fun showDeleteDialog(memo: Memo) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete)) // 삭제
            .setMessage(getString(R.string.delete_dialog_msg)) // 선택한 메모를 삭제할까요?
            .setNegativeButton(getString(R.string.cancel), null) // 취소
            .setPositiveButton(getString(R.string.delete)) { dialog, _ -> // 삭제
                memoViewModel.deleteMemo(memo)
                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView() {
        with(binding) {
            recyclerView.apply {
                adapter = memoAdapter
                layoutManager = LinearLayoutManager(requireContext()).apply {
                    reverseLayout = true
                    stackFromEnd = true
                }
                setHasFixedSize(true) // 아이템 크기 고정 -> 성능 최적화

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        // 위로 더 이상 스크롤 할 수 없으면 최상단
                        val isAtTop = !canScrollVertically(-1)
                        fabScroll.visibility = if (isAtTop) View.GONE else View.VISIBLE
                    }
                })
            }
        }
    }

    private fun setupObserver() {
        memoViewModel.getAllMemos.observe(viewLifecycleOwner) { memoList ->
            with(memoAdapter) {
                submitMemoList(memoList)
                if (itemCount > 0) {
                    binding.recyclerView.smoothScrollToPosition(itemCount - 1)
                }
            }
        }
    }

    private fun setupSearchView() {
        with(binding) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                // 검색어 입력 시 호출
                override fun onQueryTextChange(newText: String?): Boolean {
                    val query = newText.orEmpty() // null이면 "" 처리

                    with(memoAdapter) {
                        filterList(query) { // 필터링
                            // 검색어가 비어 있고, 메모가 하나 이상 있으면
                            if (query.isEmpty() && itemCount > 0) {
                                with(recyclerView) {
                                    post { // RecyclerView 업데이트 후 실행
                                        smoothScrollToPosition(itemCount - 1)
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                // 키보드 검색 버튼 클릭 시 호출
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
            })
        }
    }

    private fun setupFabAdd() {
        with(binding) {
            fabAdd.setOnClickListener {
                searchView.setQuery("", false) // 검색어 초기화
                
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, MemoFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun setupFabScroll() {
        with(binding) {
            fabScroll.setOnClickListener {
                with(memoAdapter) {
                    if (itemCount > 0) {
                        recyclerView.smoothScrollToPosition(itemCount - 1)
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