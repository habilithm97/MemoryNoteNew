package com.example.memorynotenew.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.common.Constants.THIRTY_DAYS_MS
import com.example.memorynotenew.repository.FirebaseRepository
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.database.MemoDatabase
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/* Context는 UI 컴포넌트 종료 시 사라지지만,
 ApplicationContext는 앱 전체 생명주기 동안 유지 */
class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val memoRepository = MemoRepository(
        MemoDatabase.getInstance(application).memoDao(),
        MemoDatabase.getInstance(application).trashDao()
    )
    private val firebaseRepository =  FirebaseRepository()

    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()
    val getAllTrash: LiveData<List<Trash>> = memoRepository.getAllTrash().asLiveData()

    private var backupStarted = false

    init {
        startBackup()
    }

    fun insertMemo(memo: Memo) {
        // viewModelScope : ViewModel 생명주기 동안 코루틴 실행, 종료 시 자동 취소
        viewModelScope.launch(Dispatchers.IO) { // 입출력 작업을 비동기로 실행
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
            // Trash 객체 생성
            val trash = Trash(
                memoId = memo.id,
                content = memo.content,
                deletedAt = System.currentTimeMillis() // 삭제한 시점
            )
            with(memoRepository) {
                insertTrash(trash) // 휴지통에 추가
                deleteMemo(memo) // 메모 삭제
            }
        }
    }

    // 메모 복원
    fun restoreMemo(trash: Trash) {
        viewModelScope.launch(Dispatchers.IO) {
            // Memo 객체 생성
            val memo = Memo(
                id = trash.memoId,
                content = trash.content,
                date = System.currentTimeMillis(), // 복원한 시점
                isLocked = false
            )
            with(memoRepository) {
                insertMemo(memo) // 메모 복원
                deleteTrash(trash) // 휴지통에서 삭제
            }
        }
    }

    // 휴지통에서 완전히 삭제
    fun deleteTrash(trash: Trash) {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.deleteTrash(trash)
        }
    }

    // 휴지통 비우기
    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            memoRepository.deleteAllTrash()
        }
    }

    // 30일 지난 휴지통 메모 자동 삭제
    fun deleteOldTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            // 오늘부터 30일 전 시간
            val cutoffTime = System.currentTimeMillis() - THIRTY_DAYS_MS
            memoRepository.deleteOldTrash(cutoffTime)
        }
    }

    // Room -> Firebase 실시간 백업
    fun startBackup() {
        if (backupStarted) return
        backupStarted = true

        viewModelScope.launch {
            memoRepository.getAllMemos()
                .distinctUntilChanged() // 동일 데이터 중복 업로드 방지
                .collect { memos ->
                    try {
                        firebaseRepository.backupMemos(memos)
                    } catch (e: Exception) {
                        Log.e("Backup", "메모 백업 실패: ${e.message}")
                    }
                }
        }
        viewModelScope.launch {
            memoRepository.getAllTrash()
                .distinctUntilChanged()
                .collect { trash ->
                    try {
                        firebaseRepository.backupTrash(trash)
                    } catch (e: Exception) {
                        Log.e("Backup", "휴지통 백업 실패: ${e.message}")
                    }
                }
        }
    }
}