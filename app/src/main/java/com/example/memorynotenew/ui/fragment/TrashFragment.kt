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
        memoViewModel.getAllTrash.observe(viewLifecycleOwner) { trashList ->
            with(trashAdapter) {
                submitList(trashList)
                if (itemCount > 0) {
                    binding.recyclerView.smoothScrollToPosition(itemCount - 1)
                }
            }
        }
    }

    fun setMultiSelect(isMultiSelect: Boolean) {
        trashAdapter.isMultiSelect = isMultiSelect
    }

    fun toggleSelectAll() {
        trashAdapter.toggleSelectAll()
    }

    fun deleteSelectedMemos() {
        val selectedMemos = trashAdapter.getSelectedMemos() // 선택한 메모 가져오기
        if (selectedMemos.isEmpty()) { // 없으면
            ToastUtil.showToast(requireContext(), getString(R.string.select_memo_to_delete))
        } else { // 있으면
            showDeleteDialog(selectedMemos)
        }
    }

    private fun showDeleteDialog(selectedMemos: List<Trash>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_dialog_msg))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteTrash(selectedMemos)
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTrash(selectedMemos: List<Trash>) {
        selectedMemos.forEach {
            memoViewModel.deleteTrash(it)
        }
        trashAdapter.isMultiSelect = false
        (activity as? MainActivity)?.toggleMenuVisibility(this@TrashFragment, isMultiSelect = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}