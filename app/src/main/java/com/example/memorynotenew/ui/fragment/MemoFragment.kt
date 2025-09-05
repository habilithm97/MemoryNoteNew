package com.example.memorynotenew.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.databinding.FragmentMemoBinding
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.viewmodel.MemoViewModel

class MemoFragment : Fragment() {
    private var _binding: FragmentMemoBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)
    private var memo: Memo? = null
    private val memoViewModel: MemoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 소프트 키보드 높이 만큼 EditText 하단 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.editText) { editText, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            editText.updatePadding(bottom = imeInsets.bottom)
            insets
        }
        memo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(MEMO, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(MEMO)
        }
        if (memo != null) {
            binding.editText.setText(memo!!.content)
        }
    }

    override fun onPause() {
        super.onPause()

        val currentMemo = binding.editText.text.toString()
        val date = System.currentTimeMillis()

        when {
            // 메모가 비어 있지 않고, 기존 메모와 같지 않으면 -> 추가/수정
            currentMemo.isNotBlank() && currentMemo != memo?.content -> {
                if (memo != null) { // 수정 모드
                    updateMemo(currentMemo, date)
                } else { // 추가 모드
                    saveMemo(currentMemo, date)
                }
            }
            // 기존 메모가 존재하고 메모가 비어 있으면 삭제
            memo != null && currentMemo.isBlank() -> {
                memoViewModel.deleteMemo(memo!!)
            }
        }
    }

    private fun saveMemo(currentMemo: String, date: Long) {
        val memo = Memo(content = currentMemo, date  = date)
        memoViewModel.insertMemo(memo)
    }

    private fun updateMemo(currentMemo: String, date: Long) {
        // 기존 Memo 객체의 content만 수정하여 새로운 객체 생성
        val updatedMemo = memo?.copy(content = currentMemo, date =  date)
        if (updatedMemo != null) {
            memoViewModel.updateMemo(updatedMemo)
        }
    }

    override fun onResume() {
        super.onResume()

        // 새 메모일 경우 소프트 키보드 자동 표시
        if (memo == null) {
            with(binding) {
                editText.requestFocus()
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}