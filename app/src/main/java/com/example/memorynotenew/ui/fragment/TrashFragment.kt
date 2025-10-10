package com.example.memorynotenew.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memorynotenew.R
import com.example.memorynotenew.adapter.TrashAdapter
import com.example.memorynotenew.databinding.FragmentTrashBinding
import com.example.memorynotenew.room.entity.Trash
import com.example.memorynotenew.ui.activity.MainActivity
import com.example.memorynotenew.utils.ToastUtil
import com.example.memorynotenew.viewmodel.MemoViewModel

class TrashFragment : Fragment() {
    private var _binding: FragmentTrashBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)
    private val trashAdapter by lazy { TrashAdapter() }
    private val memoViewModel: MemoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObserver()
        memoViewModel.deleteOldTrash()
    }

    private fun setupRecyclerView() {
        with(binding) {
            recyclerView.apply {
                adapter = trashAdapter
                layoutManager = LinearLayoutManager(root.context).apply {
                    reverseLayout = true
                    stackFromEnd = true
                }
                setHasFixedSize(true) // 아이템 크기 고정 -> 성능 최적화
            }
        }
    }

    private fun setupObserver() {
        memoViewModel.getAllTrash.observe(viewLifecycleOwner) {
            with(binding) {
                if (it.isEmpty()) { // 휴지통이 비어 있으면
                    recyclerView.visibility = View.GONE
                    textView.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.empty_trash)
                    }
                } else { // 휴지통이 비어 있지 않으면
                    recyclerView.visibility = View.VISIBLE
                    textView.visibility = View.GONE

                    with(trashAdapter) {
                        submitList(it)
                        if (itemCount > 0) {
                            recyclerView.smoothScrollToPosition(itemCount - 1)
                        }
                    }
                }
            }
        }
    }

    // 다중 선택 토글
    fun toggleMultiSelect(isMultiSelect: Boolean) {
        trashAdapter.isMultiSelect = isMultiSelect
    }

    // 전체 선택 토글
    fun toggleSelectAll() {
        trashAdapter.toggleSelectAll()
    }

    // selectedTrash 삭제
    fun deleteSelectedTrash() {
        val selectedTrash = trashAdapter.getSelectedTrash() // selectedTrash 가져오기

        if (selectedTrash.isEmpty()) { // 없으면
            ToastUtil.showToast(requireContext(), getString(R.string.select_memo_to_delete))
        } else { // 있으면
            showDeleteDialog(selectedTrash)
        }
    }

    private fun showDeleteDialog(selectedTrash: List<Trash>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_dialog_msg))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteTrash(selectedTrash)
                dialog.dismiss()
            }
            .show()
    }

    // 휴지통에서 완전히 삭제
    private fun deleteTrash(selectedTrash: List<Trash>) {
        selectedTrash.forEach { memoViewModel.deleteTrash(it) }
        trashAdapter.isMultiSelect = false
        (activity as? MainActivity)?.toggleMenuVisibility(this, false)
    }

    // selectedTrash 복원
    fun restoreSelectedTrash() {
        val selectedTrash = trashAdapter.getSelectedTrash() // selectedTrash 가져오기

        if (selectedTrash.isEmpty()) { // 없으면
            ToastUtil.showToast(requireContext(), getString(R.string.select_memo_to_restore))
        } else { // 있으면
            showRestoreDialog(selectedTrash)
        }
    }

    private fun showRestoreDialog(selectedTrash: List<Trash>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.restore))
            .setMessage(getString(R.string.restore_dialog_msg))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.restore)) { dialog, _ ->
                restoreMemo(selectedTrash)
                dialog.dismiss()
            }
            .show()
    }

    // 메모 복원
    private fun restoreMemo(selectedTrash: List<Trash>) {
        selectedTrash.forEach { memoViewModel.restoreMemo(it) }
        trashAdapter.isMultiSelect = false
        (activity as? MainActivity)?.toggleMenuVisibility(this, false)
    }

    fun showEmptyTrashDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.empty))
            .setMessage(getString(R.string.empty_trash_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.empty)) { dialog, _ ->
                memoViewModel.emptyTrash()
                dialog.dismiss()
            }
            .show()
    }

    fun hasTrash(): Boolean {
        return trashAdapter.itemCount > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}