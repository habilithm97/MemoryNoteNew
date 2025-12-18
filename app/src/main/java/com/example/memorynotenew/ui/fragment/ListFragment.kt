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
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.LockPasswordPurpose
import com.example.memorynotenew.common.PopupAction
import com.example.memorynotenew.databinding.FragmentListBinding
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.security.LockPasswordManager
import com.example.memorynotenew.ui.activity.MainActivity
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.viewmodel.MemoViewModel
import com.google.android.gms.ads.AdRequest

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    private lateinit var memoAdapter: MemoAdapter
    private val memoViewModel: MemoViewModel by viewModels()

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
        setupAdapter() // RecyclerView 어댑터 설정
        setupRecyclerView() // RecyclerView 설정
        observeViewModel() // ViewModel의 LiveData 관찰 -> UI 자동 갱신
        setupSearchView() // SearchView 설정
        setupFabAdd() // 새 메모 추가 FloatingActionButton 설정
        setupFabScroll() // 스크롤 FloatingActionButton 설정
        setupAdView() // 하단 광고바 설정

        with(requireActivity()) {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                finish()
            }
        }
    }

    private fun setupAdapter() {
        memoAdapter = MemoAdapter(
            onItemClick = { memo ->
                clearSearchQuery() // 검색어 초기화

                val targetFragment = if (memo.isLocked) { // 잠금 o
                    LockPasswordFragment.newInstance(LockPasswordPurpose.OPEN, memo)
                } else { // 잠금 x
                    MemoFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable(MEMO, memo)
                        }
                    }
                }
                replaceFragment(targetFragment)
            }, onItemLongClick = { memo, popupAction ->
                clearSearchQuery()

                when (popupAction) {
                    PopupAction.DELETE ->
                        showDeleteDialog(listOf(memo), false)
                    PopupAction.LOCK -> {
                        val storedLockPassword = LockPasswordManager.getLockPassword(requireContext())

                        if (storedLockPassword.isNullOrEmpty()) { // 저장된 잠금 비밀번호 x
                            // "먼저 잠금 비밀번호를 설정해주세요."
                            requireContext().showToast(getString(R.string.set_lock_password_first))
                        } else { // 저장된 비밀번호 o
                            val lockPasswordFragment = LockPasswordFragment.newInstance(LockPasswordPurpose.LOCK, memo)
                            replaceFragment(lockPasswordFragment)
                        }
                    }
                }
            }
        )
    }

    private fun clearSearchQuery() {
        binding.searchView.setQuery("", false)
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteDialog(selectedMemos: List<Memo>, isMultiDelete: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.dialog_delete))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                if (isMultiDelete) { // 다중 삭제
                    multiDeleteMemo(selectedMemos)
                } else { // 단일 삭제
                    // 리스트가 비어 있지 않으면 첫 번째 메모 삭제
                    selectedMemos.firstOrNull()?.let {
                        singleDeleteMemo(it)
                    }
                }
            }
            .show()
    }

    private fun singleDeleteMemo(memo: Memo) {
        if (memo.isLocked) { // 잠금 o
            val lockPasswordFragment = LockPasswordFragment.newInstance(LockPasswordPurpose.DELETE, memo)
            replaceFragment(lockPasswordFragment)
        } else { // 잠금 x
            memoViewModel.moveMemoToTrash(memo)
            // "1개의 메모가 삭제되었습니다."
            requireContext().showToast(getString(R.string.delete_memo_result, 1))
        }
    }

    private fun multiDeleteMemo(selectedMemos: List<Memo>) {
        val lockedMemos = selectedMemos.filter { it.isLocked } // 잠긴 메모 리스트
        val unlockedMemos = selectedMemos.filterNot { it.isLocked } // 잠기지 않은 메모 리스트

        // 잠기지 않은 메모는 바로 삭제
        unlockedMemos.forEach { memoViewModel.moveMemoToTrash(it) }

        if (lockedMemos.isNotEmpty()) { // 잠긴 메모가 하나라도 있으면
            val lockPasswordFragment = LockPasswordFragment.newInstance(
                LockPasswordPurpose.DELETE,
                deleteCount = selectedMemos.size,
                memos = ArrayList(lockedMemos) // Bundle에 담기 위해 ArrayList로 변환
            )
            replaceFragment(lockPasswordFragment)
        } else { // 잠긴 메모가 없으면
            // "n개의 메모가 삭제되었습니다."
            requireContext().showToast(getString(R.string.delete_memo_result, selectedMemos.size))
        }
        memoAdapter.isMultiSelect = false
        (activity as? MainActivity)?.toggleMenuVisibility(this, false)
    }

    private fun setupRecyclerView() {
        with(binding) {
            recyclerView.apply {
                adapter = memoAdapter
                layoutManager = LinearLayoutManager(root.context).apply {
                    reverseLayout = true
                    stackFromEnd = true
                }
                setHasFixedSize(true) // 아이템 크기 고정 -> 성능 최적화

                // RecyclerView 스크롤 시 호출
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

    private fun observeViewModel() {
        memoViewModel.getAllMemos.observe(viewLifecycleOwner) {
            with(binding) {
                if (it.isEmpty()) { // 메모 x
                    recyclerView.visibility = View.GONE
                    tvEmpty.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.no_memos) // "메모가 없습니다."
                    }
                } else { // 메모 o
                    recyclerView.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE

                    with(memoAdapter) {
                        submitMemos(it)
                        if (itemCount > 0) {
                            recyclerView.smoothScrollToPosition(itemCount - 1)
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchView() {
        with(binding) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                // 검색어 입력 시
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
                // 키보드 검색 버튼 클릭 시
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
            })
        }
    }

    private fun setupFabAdd() {
        binding.fabAdd.setOnClickListener {
            clearSearchQuery()
            replaceFragment(MemoFragment())
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

    // 다중 선택 토글 (MainActivity에서 사용)
    fun toggleMultiSelect(isMultiSelect: Boolean) {
        memoAdapter.isMultiSelect = isMultiSelect
    }

    // 전체 선택 토글 (MainActivity에서 사용)
    fun toggleSelectAll() {
        memoAdapter.toggleSelectAll()
    }

    // selectedMemos 삭제 (MainActivity에서 사용)
    fun deleteSelectedMemos() {
        val selectedMemos = memoAdapter.getSelectedMemos()

        if (selectedMemos.isEmpty()) {
            // "삭제할 메모를 선택해주세요."
            requireContext().showToast(getString(R.string.select_memo_to_delete))
        } else {
            showDeleteDialog(selectedMemos, true)
        }
    }

    // 메모 존재 여부 확인
    fun hasMemos(): Boolean {
        return memoAdapter.itemCount > 0
    }

    private fun setupAdView() {
        with(binding.adView) {
            post { // attach 후 실행
                // 광고 요청 객체 생성 후 광고 로드
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.adView.resume()
    }

    override fun onPause() {
        super.onPause()

        binding.adView.pause()
    }

    override fun onDestroyView() {
        binding.adView.destroy() // 뷰가 살아있을 때 정리
        _binding = null // 메모리 누수 방지
        super.onDestroyView() // 프래그먼트 기본 뷰 정리
    }
}