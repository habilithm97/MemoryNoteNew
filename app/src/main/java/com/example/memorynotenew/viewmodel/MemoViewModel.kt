package com.example.memorynotenew.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.database.MemoDatabase
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/* Context는 UI 컴포넌트 종료 시 사라지지만,
 ApplicationContext는 앱 전체 생명주기 동안 유지 */
class MemoViewModel(application: Application) : AndroidViewModel(application) {
    private val memoRepository: MemoRepository
    val getAllMemos: LiveData<List<Memo>>
    val getAllTrash: LiveData<List<Trash>>

    init { // ViewModel 생성 시 한 번만 초기화
        val db = MemoDatabase.getInstance(application)
        val memoDao = db.memoDao()
        val trashDao = db.trashDao()
        memoRepository = MemoRepository(memoDao, trashDao)
        // UI에서 쉽게 관찰할 수 있도록 LiveData로 변환
        getAllMemos = memoRepository.getAllMemos().asLiveData()
        getAllTrash = memoRepository.getAllTrash().asLiveData()
    }

    fun insertMemo(memo: Memo) {
        // viewModelScope : ViewModel 생명주기 내에서 Coroutine 실행
        viewModelScope.launch(Dispatchers.IO) { // I/O 스레드에서 비동기 작업 실행
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

    // 휴지통으로 이동
    fun moveMemoToTrash(memo: Memo) {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.deleteMemo(memo) // 메모 삭제

            val trash = Trash( // Trash 객체 생성
                memoId = memo.id,
                content = memo.content,
                deletedAt = System.currentTimeMillis() // 삭제한 시점
            )
            memoRepository.insertTrash(trash) // 휴지통에 추가
        }
    }

    // 메모 복원
    fun restoreMemo(trash: Trash) {
        viewModelScope.launch(Dispatchers.IO) {
            val memo = Memo( // Memo 객체 생성
                id = trash.memoId,
                content = trash.content,
                date = System.currentTimeMillis(), // 복원한 시점
                isLocked = false
            )
            with(memoRepository) {
                insertMemo(memo) // 메모 복원
                deleteTrash(trash) // 휴지통에서 제거
            }
        }
    }

    // 30일 지난 휴지통 메모 자동 삭제
    fun deleteOldTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            // 오늘부터 30일 전 시간
            val cutoffTime = System.currentTimeMillis() - Constants.THIRTY_DAYS_MS
            memoRepository.deleteOldTrash(cutoffTime)
        }
    }


    // 휴지통에서 완전히 삭제
    fun deleteTrash(trash: Trash) {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.deleteTrash(trash)
        }
    }
}