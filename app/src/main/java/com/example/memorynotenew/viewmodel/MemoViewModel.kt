package com.example.memorynotenew.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.room.memo.MemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/* Context는 UI 컴포넌트 종료 시 사라지지만,
 ApplicationContext는 앱 전체 생명주기 동안 유지 */
class MemoViewModel(application: Application) : AndroidViewModel(application) {
    private val memoRepository: MemoRepository
    val getAllMemos: LiveData<List<Memo>>

    init { // ViewModel 생성 시 한 번만 초기화
        val memoDao = MemoDatabase.getInstance(application).memoDao()
        memoRepository = MemoRepository(memoDao)
        // UI에서 쉽게 관찰할 수 있도록 LiveData로 변환
        getAllMemos = memoRepository.getAllMemos().asLiveData()
    }

    fun insertMemo(memo: Memo) {
        // viewModelScope : ViewModel 생명주기 내에서 Coroutine 실행
        viewModelScope.launch(Dispatchers.IO) { // DB I/O 작업을 백그라운드에서 처리
            memoRepository.insertMemo(memo)
        }
    }

    fun updateMemo(memo: Memo) {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.updateMemo(memo)
        }
    }

    fun deleteMemo(memo: Memo) {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.deleteMemo(memo)
        }
    }
}