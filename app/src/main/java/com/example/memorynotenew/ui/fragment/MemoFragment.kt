package com.example.memorynotenew.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.databinding.FragmentMemoBinding
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.ui.activity.MainActivity
import com.example.memorynotenew.viewmodel.MemoViewModel

class MemoFragment : Fragment() {
    private var _binding: FragmentMemoBinding? = null
    private val binding get() = _binding!!
    private val memoViewModel: MemoViewModel by viewModels()
    private var selectedMemo: Memo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showUpButton(true) // 업 버튼 활성화

        selectedMemo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(Constants.MEMO, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(Constants.MEMO)
        }
        if (selectedMemo != null) {
            binding.editText.setText(selectedMemo!!.content)
        }
    }

    override fun onPause() {
        super.onPause()

        val currentMemo = binding.editText.text.toString()
        // 메모가 비어 있지 않고, 선택된 메모와 다르면
        if (currentMemo.isNotBlank() && currentMemo != selectedMemo?.content) {
            if (selectedMemo != null) { // 수정 모드
                updateMemo(currentMemo)
            } else { // 추가 모드
                saveMemo(currentMemo)
            }
        }
    }

    private fun saveMemo(currentMemo: String) {
        val date = System.currentTimeMillis()
        val memo = Memo(content = currentMemo, date  = date)
        memoViewModel.insertMemo(memo)
    }

    private fun updateMemo(currentMemo: String) {
        val date = System.currentTimeMillis()
        // 기존 Memo 객체의 content만 수정하여 새로운 객체 생성
        val updatedMemo = selectedMemo?.copy(content = currentMemo, date =  date)

        if (updatedMemo != null) {
            memoViewModel.updateMemo(updatedMemo)
        }
    }

    override fun onResume() {
        super.onResume()

        // 새 메모일 경우 소프트 키보드 자동으로 표시
        if (selectedMemo == null) {
            with(binding) {
                editText.requestFocus()
                // requireContext : null이면 예외
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        (activity as? MainActivity)?.showUpButton(false) // 업 버튼 비활성화
        _binding = null
    }
}