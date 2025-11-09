package com.example.memorynotenew.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.common.Constants.THIRTY_DAYS_MS
import com.example.memorynotenew.repository.FirebaseRepository
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.database.MemoDatabase
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    init { // ViewModel 생성 시 자동으로 백업 시작
        startBackup()
    }

    private fun <T> launchIO(block: suspend () -> T) {
        // ViewModel 생명주기 동안 코루틴 실행, 종료 시 자동 취소
        // 입출력 작업을 비동기로 실행
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
    fun insertMemo(memo: Memo) = launchIO { memoRepository.insertMemo(memo) }
    fun updateMemo(memo: Memo) = launchIO { memoRepository.updateMemo(memo) }
    fun deleteMemo(memo: Memo) = launchIO { memoRepository.deleteMemo(memo) }
    // 휴지통으로 이동
    fun moveMemoToTrash(memo: Memo) = launchIO {
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
    // 메모 복원
    fun restoreMemo(trash: Trash) = launchIO {
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
    // 휴지통에서 완전히 삭제
    fun deleteTrash(trash: Trash) = launchIO { memoRepository.deleteTrash(trash) }
    // 휴지통 비우기
    fun emptyTrash() = launchIO { memoRepository.deleteAllTrash() }
    // 30일 지난 휴지통 메모 자동 삭제
    fun deleteOldTrash() = launchIO {
        // 오늘부터 30일 전 시간
        val cutoffTime = System.currentTimeMillis() - THIRTY_DAYS_MS
        memoRepository.deleteOldTrash(cutoffTime)
    }

    // Room -> Firebase 실시간 백업
    fun startBackup() {
        // 백업 중복 실행 방지
        if (backupStarted) return
        backupStarted = true

        startBackupFlow(
            items = memoRepository.getAllMemos(),
            backup = { firebaseRepository.backupMemos(it) },
            itemType = Constants.MEMOS
        )
        startBackupFlow(
            items = memoRepository.getAllTrash(),
            backup = { firebaseRepository.backupTrash(it) },
            itemType = Constants.TRASH
        )
    }

    private fun <T> startBackupFlow(
        items: Flow<List<T>>, // Room DB 데이터 스트림 (백업용)
        backup: suspend (List<T>) -> Unit, // 전달 받은 리스트를 백업하는 함수
        itemType: String
    ) {
        viewModelScope.launch {
            items
                .distinctUntilChanged() // 동일 데이터 중복 백업 방지
                .collect { // 새 데이터 수집 시 실행
                if (it.isEmpty()) return@collect
                try {
                    backup(it)
                } catch (e: Exception) {
                    Log.e("MemoViewModel", "$itemType backup failed: ${e.message}", e)
                }
            }
        }
    }
}