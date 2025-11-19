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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* Context는 UI 컴포넌트 종료 시 사라지지만,
 ApplicationContext는 앱 전체 생명주기 동안 유지 */
class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val memoRepository = MemoRepository(
        MemoDatabase.getInstance(application).memoDao(),
        MemoDatabase.getInstance(application).trashDao()
    )
    private val firebaseRepository =  FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()
    val getAllTrash: LiveData<List<Trash>> = memoRepository.getAllTrash().asLiveData()

    // 백업 코루틴 (중복 실행 방지)
    private var memoBackupJob: Job? = null
    private var trashBackupJob: Job? = null

    // 실시간 백업 상태
    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning = _isBackupRunning.asStateFlow()

    init {
        auth.addAuthStateListener { // 로그인 상태 변화 감지
            val user = it.currentUser

            if (user != null) {
                startBackup()
            } else {
                stopBackup()
            }
        }
    }

    private fun startBackup() { // cancel -> 플래그 켬 -> 새 Job
        if (_isBackupRunning.value) return // 중복 실행 방지

        // 기존 백업 Flow 취소 (중복 실행 방지)
        memoBackupJob?.cancel()
        trashBackupJob?.cancel()

        _isBackupRunning.value = true // 실시간 백업 ON

        // 새로운 백업 Flow 시작 (실제 백업은 firebaseRepository에게 위임)
        memoBackupJob = backupItemsFlow(
            items = memoRepository.getAllMemos(),
            itemType = Constants.MEMO,
            backupAction = firebaseRepository::backupMemos
        )
        trashBackupJob = backupItemsFlow(
            items = memoRepository.getAllTrash(),
            itemType = Constants.TRASH,
            backupAction = firebaseRepository::backupTrash
        )
    }

    private fun stopBackup() { // 플래그 끔 -> cancel
        if (!_isBackupRunning.value) return

        _isBackupRunning.value = false // 실시간 백업 OFF

        memoBackupJob?.cancel()
        trashBackupJob?.cancel()
    }

    // 실시간 감시용 백업
    private fun <T> backupItemsFlow(
        items: Flow<List<T>>,
        itemType: String,
        backupAction: suspend (List<T>) -> Unit
    ): Job {
        return viewModelScope.launch {
            items.distinctUntilChanged() // 리스트가 바뀌었을 때만 처리
                .collect { // 방출된 리스트를 받아 처리

                    // 실시간 백업이 꺼져 있으면 return
                    if (!_isBackupRunning.value) return@collect

                    try {
                        backupAction(it) // 위임된 함수 호출
                    } catch (e: Exception) {
                        Log.e("MemoViewModel", "$itemType backup failed: ${e.message}", e)
                    }
            }
        }
    }

    // 즉시 실행용 백업
    private suspend fun <T> backupIfRunning(items: List<T>, itemType: String) {
        if (!_isBackupRunning.value) return

        try {
            when(itemType) {
                Constants.MEMOS -> firebaseRepository.backupMemos(items as List<Memo>)
                Constants.TRASH -> firebaseRepository.backupTrash(items as List<Trash>)
            }
        } catch (e: Exception) {
            Log.e("MemoViewModel", "$itemType backup failed: ${e.message}", e)
        }
    }

    private fun <T> launchIO(block: suspend () -> T) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    fun insertMemo(memo: Memo) = launchIO {
        memoRepository.insertMemo(memo)
        backupIfRunning(listOf(memo), Constants.MEMOS)
    }

    fun updateMemo(memo: Memo) = launchIO {
        memoRepository.updateMemo(memo)
        backupIfRunning(listOf(memo), Constants.MEMOS)
    }

    fun deleteMemo(memo: Memo) = launchIO {
        memoRepository.deleteMemo(memo)
        firebaseRepository.delete(memo.id.toString(), Constants.MEMO)
    }

    // 휴지통으로 이동
    fun moveMemoToTrash(memo: Memo) = launchIO {
        val trash = Trash(
            memoId = memo.id,
            content = memo.content,
            deletedAt = System.currentTimeMillis() // 삭제한 시점
        )
        val trashId = memoRepository.insertTrash(trash) // Room 휴지통에 추가
        memoRepository.deleteMemo(memo) // Room 원본 메모 삭제

        val savedTrash = trash.copy(id = trashId)
        backupIfRunning(listOf(savedTrash), Constants.TRASH) // Firebase 휴지통에 추가
        firebaseRepository.delete(memo.id.toString(), Constants.MEMO) // Firebase 원본 메모 삭제
    }

    // 메모 복원
    fun restoreMemo(trash: Trash) = launchIO {
        val memo = Memo(
            id = 0,
            content = trash.content,
            date = System.currentTimeMillis(),
            isLocked = false
        )
        val memoId = memoRepository.insertMemo(memo) // Room 원본 메모 추가
        memoRepository.deleteTrash(trash) // Room 휴지통에서 삭제

        val savedMemo = memo.copy(id = memoId)
        backupIfRunning(listOf(savedMemo), Constants.MEMOS) // Firebase 원본 메모 추가
        firebaseRepository.delete(trash.id.toString(), Constants.TRASH) // Firebase 휴지통에서 삭제
    }

    // 휴지통에서 완전히 삭제
    fun deleteTrash(trash: Trash) = launchIO {
        memoRepository.deleteTrash(trash)
        firebaseRepository.delete(trash.id.toString(), Constants.TRASH)
    }

    // 휴지통 비우기
    fun emptyTrash() = launchIO {
        val trashList = memoRepository.getAllTrash().first() // Room 휴지통 리스트를 한 번만 가져옴
        memoRepository.deleteAllTrash()
        trashList.forEach { firebaseRepository.delete(it.id.toString(), Constants.TRASH) }
    }

    // 30일 지난 휴지통 메모 자동 삭제
    fun deleteOldTrash() = launchIO {
        val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS // 30일 전 시각
        // Room에서 휴지통 리스트를 한 번만 가져오고, 삭제 시각이 30일 이전인 항목만 필터링
        val oldTrashList = memoRepository.getAllTrash().first().filter { it.deletedAt < cutoff }
        memoRepository.deleteOldTrash(cutoff)
        oldTrashList.forEach { firebaseRepository.delete(it.id.toString(), Constants.TRASH) }
    }

    fun onUserChanged() {
        stopBackup()
    }
}