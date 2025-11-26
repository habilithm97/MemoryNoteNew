package com.example.memorynotenew.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.THIRTY_DAYS_MS
import com.example.memorynotenew.common.Constants.USERS
import com.example.memorynotenew.model.BackupResult
import com.example.memorynotenew.repository.FirebaseRepository
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.database.MemoDatabase
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    // IO 스레드에서 비동기 코루틴으로 동작
    private fun <T> launchIO(block: suspend () -> T) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
    // 백업 결과를 내부에서 업데이트하고 외부엔 읽기만 허용하는 LiveData
    private val _backupResult = MutableLiveData<BackupResult>()
    val backupResult: LiveData<BackupResult> get() = _backupResult

    fun insertMemo(memo: Memo) = launchIO { memoRepository.insertMemo(memo) }
    fun updateMemo(memo: Memo) = launchIO { memoRepository.updateMemo(memo) }
    fun deleteMemo(memo: Memo) = launchIO { memoRepository.deleteMemo(memo) }
    fun deleteTrash(trash: Trash) = launchIO { memoRepository.deleteTrash(trash) }
    fun emptyTrash() = launchIO { memoRepository.deleteAllTrash() }

    fun deleteOldTrash() = launchIO {
        val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS // 30일 전 시각
        memoRepository.deleteOldTrash(cutoff)
    }

    fun moveMemoToTrash(memo: Memo) = launchIO {
        val trash = Trash(
            memoId = memo.id,
            content = memo.content,
            deletedAt = System.currentTimeMillis()
        )
        with(memoRepository) {
            insertTrash(trash) // 휴지통에 추가
            deleteMemo(memo) // 메모 삭제
        }
    }

    fun restoreMemo(trash: Trash) = launchIO {
        val memo = Memo(
            id = 0,
            content = trash.content,
            date = System.currentTimeMillis(),
            isLocked = false
        )
        with(memoRepository) {
            insertMemo(memo) // 메모 추가
            deleteTrash(trash) // 휴지통에서 삭제
        }
    }

    fun backupMemos() = launchIO {
        try {
            // Flow에서 메모 리스트를 한 번만 가져옴
            val memos = memoRepository.getAllMemos().first()

            firebaseRepository.backup(memos) // 로컬 -> 서버

            val uid = firebaseRepository.auth.currentUser?.uid ?: return@launchIO
            val db = FirebaseFirestore.getInstance()

            /** 잔여 서버 문서 삭제 */
            // Firestore 경로 : users/{uid}/memo
            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            // memoCollection 문서 가져오기
            val snapshots = memoCollection.get().await()

            // 서버의 문서를 하나씩 순회
            snapshots.documents.forEach { doc ->
                // 로컬에 doc.id와 동일한 id가 없는 경우 -> 서버에만 존재하는 문서
                if (memos.none {
                    it.id.toString() == doc.id}) {
                    doc.reference.delete() // 서버에만 해당 문서 삭제
                    Log.d("MemoViewModel", "[DELETE] Removed server-only memo id = ${doc.id}")
                }
            }
            _backupResult.postValue(BackupResult(true, memos.size))
            Log.d("MemoViewModel", "[SUCCESS] Backup completed for memo(s) (count = ${memos.size})")
        } catch (e: Exception) {
            _backupResult.postValue(BackupResult(false))
            Log.e("MemoViewModel", "[FAIL] Backup failed: ${e.message}", e)
        }
    }
}