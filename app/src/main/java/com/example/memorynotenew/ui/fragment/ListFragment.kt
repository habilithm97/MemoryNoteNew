package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
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
import com.example.memorynotenew.ui.activity.MainActivity
import com.example.memorynotenew.utils.PasswordManager
import com.example.memorynotenew.utils.ToastUtil
import com.example.memorynotenew.viewmodel.MemoViewModel

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)
    private lateinit var memoAdapter: MemoAdapter
    private val memoViewModel: MemoViewModel by viewModels()
    private val safeContext get() = requireContext() // Attach된 시점의 안전한 Context를 반환

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 소프트 키보드 높이 만큼 RecyclerView 하단 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { recyclerView, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            recyclerView.updatePadding(bottom = imeInsets.bottom)
            insets
        }
        setupAdapter()
        setupRecyclerView()
        setupObserver()
        setupSearchView()
        setupFabAdd()
        setupFabScroll()

        with(requireActivity()) {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                finish()
            }
        }
    }

    private fun setupAdapter() {
        memoAdapter = MemoAdapter(
            onItemClick = { memo ->
                binding.searchView.setQuery("", false) // 검색어 초기화

                if (memo.isLocked) { // 메모가 잠겨 있으면 -> PasswordFragment로 이동
                    val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.OPEN, memo)
                    navigateToFragment(passwordFragment)
                } else { // 메모가 잠겨 있지 않으면 -> 바로 MemoFragment로 이동
                    val memoFragment = MemoFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable(Constants.MEMO, memo)
                        }
                    }
                    navigateToFragment(memoFragment)
                }
            }, onItemLongClick = { memo, popupAction ->
                binding.searchView.setQuery("", false) // 검색어 초기화

                when (popupAction) {
                    PopupAction.DELETE ->
                        showDeleteDialog(listOf(memo), isMultiDelete = false)
                    PopupAction.LOCK -> {
                        val storedPassword = PasswordManager.getPassword(safeContext)
                        // 저장된 비밀번호가 없으면 -> 토스트 메시지 출력
                        if (storedPassword.isNullOrEmpty()) {
                            ToastUtil.showToast(safeContext, getString(R.string.password_required))
                        } else { // 저장된 비밀번호가 있으면 -> PasswordFragment로 이동
                            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.LOCK, memo)
                            navigateToFragment(passwordFragment)
                        }
                    }
                }
            }
        )
    }

    private fun showDeleteDialog(selectedMemos: List<Memo>, isMultiDelete: Boolean) {
        AlertDialog.Builder(safeContext)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_dialog_msg))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                if (isMultiDelete) { // 다중 삭제
                    selectedMemos.forEach { memo ->
                        if (memo.isLocked) { // 메모가 잠겨 있으면 -> PasswordFragment로 이동
                            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.DELETE, memo)
                            navigateToFragment(passwordFragment)
                        } else { // 메모가 잠겨 있지 않으면
                            // selectedMemos의 각 memo를 하나씩 삭제
                            memoViewModel.deleteMemo(memo)
                            ToastUtil.showToast(safeContext, getString(R.string.deleted)) // "메모가 삭제되었습니다."
                            memoAdapter.isMultiSelect = false
                            (activity as? MainActivity)?.toggleMenuVisibility(R.id.cancel)
                        }
                    }
                } else { // 단일 삭제
                    selectedMemos.forEach { memo ->
                        if (memo.isLocked) { // 메모가 잠겨 있으면 -> PasswordFragment로 이동
                            val passwordFragment = PasswordFragment.newInstance(PasswordPurpose.DELETE, memo)
                            navigateToFragment(passwordFragment)
                        } else { // 메모가 잠겨 있지 않으면
                            // selectedMemos의 첫 번째 memo 삭제
                            memoViewModel.deleteMemo(selectedMemos.first())
                            ToastUtil.showToast(safeContext, getString(R.string.deleted)) // "메모가 삭제되었습니다."
                        }
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView() {
        with(binding) {
            recyclerView.apply {
                adapter = memoAdapter
                layoutManager = LinearLayoutManager(safeContext).apply {
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
        memoViewModel.getAllMemos.observe(viewLifecycleOwner) { memos ->
            with(memoAdapter) {
                submitMemos(memos)
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
                navigateToFragment(MemoFragment())
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

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun setMultiSelect(isMultiSelect: Boolean) {
        memoAdapter.isMultiSelect = isMultiSelect
    }

    fun toggleSelectAll() {
        memoAdapter.toggleSelectAll()
    }

    fun deleteSelectedMemos() {
        val selectedMemos = memoAdapter.getSelectedMemos() // 선택된 메모 가져오기
        if (selectedMemos.isEmpty()) { // 없으면
            ToastUtil.showToast(safeContext, getString(R.string.select_memo_to_delete))
        } else { // 있으면
            showDeleteDialog(selectedMemos, isMultiDelete = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}