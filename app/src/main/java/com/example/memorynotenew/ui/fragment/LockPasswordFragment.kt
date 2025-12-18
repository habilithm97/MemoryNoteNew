package com.example.memorynotenew.ui.fragment

import  android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.DELETE_COUNT
import com.example.memorynotenew.common.Constants.LOCK_PW_PURPOSE
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.MEMOS
import com.example.memorynotenew.common.LockPasswordState
import com.example.memorynotenew.common.LockPasswordPurpose
import com.example.memorynotenew.common.LockPasswordString
import com.example.memorynotenew.databinding.FragmentPasswordBinding
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.security.LockPasswordManager
import com.example.memorynotenew.ui.activity.SettingsActivity
import com.example.memorynotenew.utils.ToastUtil.showToast
import com.example.memorynotenew.utils.VibrateUtil
import com.example.memorynotenew.viewmodel.MemoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockPasswordFragment : Fragment() {
    private var _binding: FragmentPasswordBinding? = null // nullable
    private val binding get() = _binding!! // non-null (생명주기 내 안전)

    // 프래그먼트 생명주기에 맞춰 생성/관리되는 ViewModel
    private val memoViewModel: MemoViewModel by viewModels()

    private var lockPassword = StringBuilder() // 잠금 비밀번호
    private var storedLockPassword: String? = null // 저장된 잠금 비밀번호
    private lateinit var lockPasswordState: LockPasswordState
    private var isInputLocked = false // 입력 잠금 여부
    private lateinit var lockPasswordPurpose: LockPasswordPurpose
    private var confirmLockPassword: StringBuilder? = null
    private var deleteCount: Int = 1

    private val dots: List<View> by lazy {
        with(binding) { listOf(dot1, dot2, dot3, dot4) }
    }
    private val memo: Memo?
        // 접근할 때마다 실행되어 항상 최신 arguments 값을 읽음
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(MEMO, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(MEMO)
        }

    private val memos: List<Memo>?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelableArrayList(MEMOS, Memo::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelableArrayList(MEMOS)
        }

    /* 정적 멤버 (static) 처럼 사용
      클래스 인스턴스를 생성하지 않고도 접근 가능
     -> 프래그먼트를 만들 때 사용하는 정적 팩토리 함수 정의 */
    companion object {
        // 프래그먼트를 만들면서 필요한 데이터를 안전하게 arguments에 담아 전달
        fun newInstance(lockPasswordPurpose: LockPasswordPurpose,
                        memo: Memo? = null, // 단일 메모
                        deleteCount: Int = 1,
                        memos: ArrayList<Memo>? = null // 다중 메모
        ) : LockPasswordFragment {
            return LockPasswordFragment().apply {
                // enum 값을 문자열로 변환하여 arguments에 저장
                arguments = Bundle().apply {
                    putString(LOCK_PW_PURPOSE, lockPasswordPurpose.name)

                    memo?.let { putParcelable(MEMO, it) }
                    memos?.let { putParcelableArrayList(MEMOS, it) }

                    // DELETE일 때만 deleteCount 전달
                    if (lockPasswordPurpose == LockPasswordPurpose.DELETE) {
                        putInt(DELETE_COUNT, deleteCount)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLockPasswordPurpose()
        initDeleteCount()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLockPasswordState()
        setupSubTitle()
        setupKeypad()
        setupBtnCancel()
        observeViewModel()
    }

    private fun initLockPasswordPurpose() {
        lockPasswordPurpose = arguments?.getString(LOCK_PW_PURPOSE)?.let {
            try {
                // 가져온 문자열을 enum 값으로 변환
                LockPasswordPurpose.valueOf(it)
            } catch (e: IllegalArgumentException) {
                // 변환 실패 시 (잘못된 문자열) 기본값 (설정)
                LockPasswordPurpose.SETTING
            }
        } ?: LockPasswordPurpose.SETTING
    }

    private fun initDeleteCount() {
        deleteCount = if (lockPasswordPurpose == LockPasswordPurpose.DELETE) {
            // DELETE -> DELETE_COUNT, 값이 없으면 기본값 1
            arguments?.getInt(DELETE_COUNT) ?: 1
        } else { // DELETE x -> 기본값 1
            1
        }
    }

    private fun initLockPasswordState() {
        storedLockPassword = LockPasswordManager.getLockPassword(requireContext())

        lockPasswordState = if (storedLockPassword.isNullOrEmpty()) { // 저장된 잠금 비밀번호 x
            LockPasswordState.NEW
        } else {
            LockPasswordState.EXISTING
        }
    }

    private fun setupSubTitle() {
        val subTitle = when (lockPasswordState) {
            LockPasswordState.NEW -> getString(LockPasswordString.NEW.resId) // 새 비밀번호 입력
            LockPasswordState.EXISTING -> getString(LockPasswordString.ENTER.resId) // 기존 비밀번호 입력
        }
        binding.textView.text = subTitle
    }

    private fun setupKeypad() {
        with(binding) {
            listOf(btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0)
                .forEach { btn ->
                    btn.setOnClickListener {
                        onNumberEntered(btn.text.toString())
                    }
                }
        }
    }

    private fun onNumberEntered(number: String) {
        // 4자리 이상이거나 입력 처리 중이면 추가 입력 제한
        if (lockPassword.length >= 4 || isInputLocked) return

        lockPassword.append(number)
        updateDots()

        // 4자리 입력 완료
        if (lockPassword.length == 4) {
            completeInput()
        }
    }

    private fun updateDots() {
        // 현재 index가 입력된 비밀번호 길이보다 작으면 선택 상태로 표시
        dots.forEachIndexed { index, dot ->
            dot.isSelected = index < lockPassword.length
        }
    }

    private fun completeInput() {
        isInputLocked = true // 입력 잠금

        // 지연 후 안전한 화면 처리, 프래그먼트 소멸 시 자동 취소 (메모리 누수 방지)
        lifecycleScope.launch {
            delay(500)

            when (lockPasswordPurpose) {
                LockPasswordPurpose.SETTING -> { // 설정
                    when (lockPasswordState) {
                        LockPasswordState.NEW -> newLockPassword()
                        LockPasswordState.EXISTING -> updateLockPassword()
                    }
                }
                LockPasswordPurpose.LOCK -> toggleLock() // 메모 잠금 및 잠금 해제
                LockPasswordPurpose.OPEN -> openMemo() // 메모 열기
                LockPasswordPurpose.DELETE -> deleteMemo() // 메모 삭제
                LockPasswordPurpose.BACKUP -> backupMemo() // 메모 백업
            }
        }
    }

    private fun setupBtnCancel() {
        binding.btnCancel.setOnClickListener {
            if (lockPassword.isEmpty()) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                lockPassword.deleteAt(lockPassword.length - 1)
                updateDots()
            }
        }
    }

    private fun observeViewModel() {
        memoViewModel.backupResult.observe(viewLifecycleOwner) {
            if (it.isSuccess) {
                // "메모 백업에 성공했습니다."
                requireContext().showToast(getString(R.string.backup_success))
            } else {
                // "메모 백업에 실패했습니다."
                requireContext().showToast(getString(R.string.backup_failed))
            }
            requireActivity().finish()
        }
    }

    private fun newLockPassword() {
        with(binding) {
            if (confirmLockPassword == null) { // 첫 번째 입력
                confirmLockPassword = StringBuilder(lockPassword)
                lockPassword.clear()
                textView.text = getString(LockPasswordString.CONFIRM.resId)
            } else { // 두 번째 입력~
                if (lockPassword.toString() == confirmLockPassword.toString()) { // 첫 번째 입력과 일치
                    LockPasswordManager.saveLockPassword(root.context, lockPassword.toString())

                    val message = if (storedLockPassword.isNullOrEmpty()) {
                        R.string.lock_password_saved // "잠금 비밀번호가 저장되었습니다."
                    } else {
                        R.string.lock_password_changed // "잠금 비밀번호가 변경되었습니다."
                    }
                    requireContext().showToast(getString(message))
                    confirmLockPassword = null
                    requireActivity().supportFragmentManager.popBackStack()
                    return
                } else { // 첫 번째 입력과 불일치
                    reEnterLockPassword()
                }
            }
            clearLockPassword()
        }
    }

    private fun reEnterLockPassword() {
        binding.textView.text = getString(LockPasswordString.RE_ENTER.resId)
        VibrateUtil.vibrate(requireContext())
    }

    private fun clearLockPassword() {
        lockPassword.clear()
        updateDots()
        isInputLocked = false
    }

    private fun updateLockPassword() {
        if (lockPassword.toString() == storedLockPassword) { // 저장된 비밀번호와 일치
            lockPasswordState = LockPasswordState.NEW
            binding.textView.text = getString(LockPasswordString.NEW.resId)
        } else { // 저장된 비밀번호와 불일치
            reEnterLockPassword()
        }
        clearLockPassword()
    }

    private fun toggleLock() {
        if (lockPassword.toString() == storedLockPassword) { // 저장된 비밀번호와 일치
            // 메모 잠금 상태 변경
            memo?.let { memoViewModel.updateMemo(it.copy(isLocked = !it.isLocked)) }
            requireActivity().supportFragmentManager.popBackStack()
        } else { // 저장된 비밀번호와 불일치
            reEnterLockPassword()
        }
        clearLockPassword()
    }

    private fun openMemo() {
        if (lockPassword.toString() == storedLockPassword) { // 저장된 비밀번호와 일치
            requireActivity().supportFragmentManager.popBackStack()

            val memoFragment = MemoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(MEMO, memo)
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, memoFragment)
                .addToBackStack(null)
                .commit()
        } else { // 저장된 비밀번호와 불일치
            reEnterLockPassword()
        }
        clearLockPassword()
    }

    private fun deleteMemo() {
        if (lockPassword.toString() == storedLockPassword) { // 저장된 비밀번호와 일치
            memo?.let { // 단일 삭제
                memoViewModel.moveMemoToTrash(it)
            } ?: memos?.forEach { // 다중 삭제
                memoViewModel.moveMemoToTrash(it)
            }
            // "n개의 메모가 삭제되었습니다."
            requireContext().showToast(getString(R.string.delete_memo_result, deleteCount))
            requireActivity().supportFragmentManager.popBackStack()
        } else { // 저장된 비밀번호와 불일치
            reEnterLockPassword()
        }
        clearLockPassword()
    }

    private fun backupMemo() {
        if (lockPassword.toString() == storedLockPassword) { // 저장된 비밀번호와 일치
            memoViewModel.backupMemos()
        } else {
            reEnterLockPassword()
        }
        clearLockPassword()
    }

    override fun onResume() {
        super.onResume()

        (activity as? SettingsActivity)?.setupActionBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // 메모리 누수 방지
    }
}