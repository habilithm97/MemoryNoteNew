package com.example.memorynotenew.viewmodel

import android.app.Application
import android.content.Context
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

    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()
    val getAllTrash: LiveData<List<Trash>> = memoRepository.getAllTrash().asLiveData()

    // 백업 코루틴 (중복 실행 방지)
    private var memoBackupJob: Job? = null
    private var trashBackupJob: Job? = null

    private val auth = FirebaseAuth.getInstance()
    // 실시간 백업 상태 저장
    private val backupSharedPref = application.getSharedPreferences(
        Constants.BACKUP_SHARED_PREF,
        Context.MODE_PRIVATE // 이 앱에서만 접근 가능
    )
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /* StateFlow : 코틀린 Flow의 일종, 값이 바뀔 때마다 구독자에게 자동으로 알려줌
     -> UI에서 실시간 백업 상태를 관찰하고 동적으로 화면 갱신 가능 */

    // 실시간 백업 상태를 저장하는 내부 StateFlow
    private val _isBackupRunning = MutableStateFlow(false)
    // 외부에서 읽기 전용으로 접근할 수 있는 StateFlow
    val isBackupRunning = _isBackupRunning.asStateFlow()

    init {
        // ViewModel 생성 시 로그인 상태에 따라 실시간 백업 자동 관리
        auth.addAuthStateListener { // 로그인 상태 변화 감지
            val user = it.currentUser
            if (user != null) { // 로그인 o
                /* 사용자의 실시간 백업 상태 가져오기
                 파일 : BACKUP_SHARED_PREF
                 키 : BACKUP_RUNNING + user.uid
                 값이 없으면 false */
                val running = backupSharedPref.getBoolean(Constants.BACKUP_RUNNING + user.uid, false)
                if (running) {
                    startBackup()
                }
            } else { // 로그인 x
                stopBackup()
            }
        }
    }

    private fun <T> launchIO(block: suspend () -> T) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
    fun insertMemo(memo: Memo) = launchIO { memoRepository.insertMemo(memo) }
    fun updateMemo(memo: Memo) = launchIO { memoRepository.updateMemo(memo) }
    fun deleteMemo(memo: Memo) = launchIO {
        memoRepository.deleteMemo(memo)
        firebaseRepository.delete(memo.id.toString(), Constants.MEMO)
    }

    // 휴지통으로 이동
    fun moveMemoToTrash(memo: Memo) = launchIO {
        val trash = Trash(
            memoId = memo.id, // 원본 메모 id
            content = memo.content,
            deletedAt = System.currentTimeMillis() // 삭제한 시점
        )
        val trashId = memoRepository.insertTrash(trash) // Room 휴지통에 추가
        memoRepository.deleteMemo(memo) // Room 원본 메모 삭제

        val savedTrash = trash.copy(id = trashId)
        with(firebaseRepository) {
            backupTrash(listOf(savedTrash)) // Firebase 휴지통에 추가
            delete(memo.id.toString(), Constants.MEMO) // Firebase 원본 메모 삭제
        }
    }

    // 메모 복원
    fun restoreMemo(trash: Trash) = launchIO {
        val memo = Memo(
            id = 0,
            content = trash.content,
            date = System.currentTimeMillis(),
            isLocked = false
        )
        val memoId = memoRepository.insertMemo(memo) // Room 메모 추가
        memoRepository.deleteTrash(trash) // Room 휴지통에서 삭제

        val savedMemo = memo.copy(id = memoId)
        with(firebaseRepository) {
            backupMemos(listOf(savedMemo)) // Firebase 메모 추가
            delete(trash.id.toString(), Constants.TRASH) // Firebase 휴지통에서 삭제
        }
    }

    // 휴지통에서 완전히 삭제
    fun deleteTrash(trash: Trash) = launchIO {
        memoRepository.deleteTrash(trash)
        firebaseRepository.delete(trash.id.toString(), Constants.TRASH)
    }

    // 휴지통 비우기
    fun emptyTrash() = launchIO {
        val trashList = memoRepository.getAllTrash().first() // Room 휴지통 목록을 한 번만 가져옴
        memoRepository.deleteAllTrash()
        trashList.forEach { firebaseRepository.delete(it.id.toString(), Constants.TRASH) }
    }

    // 30일 지난 휴지통 메모 자동 삭제
    fun deleteOldTrash() = launchIO {
        val cutoffTime = System.currentTimeMillis() - THIRTY_DAYS_MS // 오늘부터 30일 전 시간
        // 휴지통 목록을 한 번 가져오고, 그 중 삭제 대상만 추림
        val oldTrashList = memoRepository.getAllTrash().first().filter { it.deletedAt < cutoffTime }
        memoRepository.deleteOldTrash(cutoffTime)
        oldTrashList.forEach { firebaseRepository.delete(it.id.toString(), Constants.TRASH) }
    }

    fun startBackup() {
        if (_isBackupRunning.value) return // 중복 실행 방지
        _isBackupRunning.value = true // 실시간 백업 ON

        currentUserId?.let { // 현재 사용자 기준으로 백업 상태 저장
            backupSharedPref.edit().putBoolean(Constants.BACKUP_RUNNING + it, true).apply()
        }
        // 기존 백업 Flow 취소 (중복 실행 방지)
        memoBackupJob?.cancel()
        trashBackupJob?.cancel()

        // 새로운 백업 Flow 시작
        memoBackupJob = backupItemsFlow(
            items = memoRepository.getAllMemos(),
            itemType = Constants.MEMOS,
            // firebaseRepository로 백업 위임
            backup = firebaseRepository::backupMemos
        )
        trashBackupJob = backupItemsFlow(
            items = memoRepository.getAllTrash(),
            itemType = Constants.TRASH,
            backup = firebaseRepository::backupTrash
        )
    }

    // Flow<List<T>>를 구독하여 데이터 변경 시 자동 백업
    private fun <T> backupItemsFlow(
        items: Flow<List<T>>,
        itemType: String,
        backup: suspend (List<T>) -> Unit // 실제 백업 함수
    ): Job {
        return viewModelScope.launch {
            items
                .distinctUntilChanged() // 동일 데이터 중복 백업 방지
                .collect { // 데이터 변경 시 수집
                    try {
                        backup(it) // FirebaseRepository로 백업 실행
                    } catch (e: Exception) {
                        Log.e("MemoViewModel", "$itemType backup failed: ${e.message}", e)
                    }
            }
        }
    }

    fun stopBackup() {
        if (!_isBackupRunning.value) return // 중복 실행 방지

        memoBackupJob?.cancel()
        trashBackupJob?.cancel()

        _isBackupRunning.value = false

        currentUserId?.let {
            backupSharedPref.edit().putBoolean(Constants.BACKUP_RUNNING + it, false).apply()
        }
    }

    // 사용자 변경 시 백업 Flow 재시작
    fun onUserChanged() {
        stopBackup()
    }
}