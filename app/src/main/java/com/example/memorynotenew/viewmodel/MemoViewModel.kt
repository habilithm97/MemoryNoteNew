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

    fun backup() = launchIO {
        try {
            // Flow에서 현재 리스트를 한 번만 가져옴
            val memos = memoRepository.getAllMemos().first()
            val trash = memoRepository.getAllTrash().first()

            // 로컬 데이터 -> Firestore
            with(firebaseRepository) {
                backupMemos(memos)
                backupTrash(trash)
            }
            // 로그인된 현재 사용자 UID (없으면 백업 중단)
            val uid = firebaseRepository.auth.currentUser?.uid ?: return@launchIO
            val db = FirebaseFirestore.getInstance()

            /** 서버에 남아 있는 로컬에 없는 문서 삭제 **/
            // Firestore 경로 : users/{uid}/memo
            val memoCollection = db.collection(Constants.USERS)
                .document(uid)
                .collection(Constants.MEMO)

            // Firestore에서 memo 컬렉션 전체 문서 가져오기
            val memoSnapshots = memoCollection.get().await()

            // 서버의 memo 문서를 하나씩 순회
            memoSnapshots.documents.forEach { doc ->
                // 로컬 memos에 doc.id와 동일한 id가 없는 경우 -> 서버에만 존재하는 문서
                if (memos.none { it.id.toString() == doc.id }) {
                    doc.reference.delete() // 서버에서 해당 문서 삭제
                }
            }
            val trashCollection = db.collection(Constants.USERS)
                .document(uid)
                .collection(Constants.TRASH)
            val trashSnapshots = trashCollection.get().await()
            trashSnapshots.documents.forEach { doc ->
                if (trash.none { it.id.toString() == doc.id }) {
                    doc.reference.delete()
                }
            }
            Log.d("MemoViewModel", "Back-up: memos=${memos.size}, trash=${trash.size}")
        } catch (e: Exception) {
            Log.e("MemoViewModel", "Back-up failed: ${e.message}", e)
        }
    }
}