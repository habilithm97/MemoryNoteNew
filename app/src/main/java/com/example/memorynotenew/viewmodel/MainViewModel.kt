package com.example.memorynotenew.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.THIRTY_DAYS_MS
import com.example.memorynotenew.common.Constants.USERS
import com.example.memorynotenew.repository.FirestoreRepository
import com.example.memorynotenew.repository.MemoRepository
import com.example.memorynotenew.room.database.MemoDatabase
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.example.memorynotenew.security.EncryptionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// AndroidViewModel -> DB 초기화에 필요한 Application Context 사용
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val memoRepository = MemoRepository(
        MemoDatabase.getInstance(application).memoDao(),
        MemoDatabase.getInstance(application).trashDao()
    )
    val firestoreRepository = FirestoreRepository()

    // Flow로 받은 데이터를 UI에서 관찰할 수 있도록 LiveData로 변환
    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()
    val getAllTrash: LiveData<List<Trash>> = memoRepository.getAllTrash().asLiveData()

    data class BackupResult(val isSuccess: Boolean)
    data class LoadResult(val isSuccess: Boolean)

    private val _backupResult = MutableLiveData<BackupResult>()
    val backupResult: LiveData<BackupResult> get() = _backupResult

    private val _loadResult = MutableLiveData<LoadResult>()
    val loadResult: LiveData<LoadResult> get() = _loadResult

    val memos: List<Memo>
        get() = getAllMemos.value ?: emptyList()

    // ViewModelScope에서 IO 스레드로 suspend 작업 실행
    private fun <T> launchIO(block: suspend () -> T) {
        // 지금 실행하지 않고 나중에 IO 코루틴 안에서 실행할 코드 덩어리
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
    fun insertMemo(memo: Memo) = launchIO { memoRepository.insertMemo(memo) }
    fun updateMemo(memo: Memo) = launchIO { memoRepository.updateMemo(memo) }
    fun deleteMemo(memo: Memo) = launchIO { memoRepository.deleteMemo(memo) }
    fun deleteTrash(trash: Trash) = launchIO { memoRepository.deleteTrash(trash) }
    fun emptyTrash() = launchIO { memoRepository.deleteAllTrash() }

    fun deleteOldTrash() = launchIO {
        val cutoffTime = System.currentTimeMillis() - THIRTY_DAYS_MS
        memoRepository.deleteOldTrash(cutoffTime)
    }

    fun moveMemoToTrash(memo: Memo) = launchIO {
        val trash = Trash(
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
        val uid = firestoreRepository.auth.currentUser?.uid ?: run {
            Log.d("MemoViewModel", "[SKIP] Backup skipped - User not signed in.")
            return@launchIO // 람다 종료
        }
        try {
            val memos = memoRepository.getAllMemos().first() // 로컬에서 메모 리스트 가져오기 (단발성)

            // 암호화된 메모 리스트 생성
            val encryptedMemos = memos.map {
                // content 암호화 -> Pair 구조 분해로 각각 꺼냄 (암호화된 데이터, 암호화에 사용된 IV)
                val (cipherBytes, ivBytes) = EncryptionManager.encrypt(it.content)
                
                it.copy( // 기존 Memo 객체를 그대로 유지하면서 content와 iv만 암호화된 값으로 교체
                    // Base64 문자열로 변환해야 Firestore에 저장 가능
                    content = Base64.encodeToString(cipherBytes, Base64.DEFAULT),
                    iv = Base64.encodeToString(ivBytes, Base64.DEFAULT)
                )
            }
            firestoreRepository.backup(encryptedMemos) // 로컬 (암호문) -> 서버

            val db = FirebaseFirestore.getInstance()

            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            val snapshots = memoCollection.get().await()

            // 서버의 문서를 하나씩 순회
            snapshots.documents.forEach { doc ->
                // 로컬에 doc.id와 동일한 id가 없는 경우 -> 서버에만 존재하는 문서
                if (memos.none { memo ->
                    memo.id.toString() == doc.id
                }) {
                    doc.reference.delete() // 서버에만 해당 문서 삭제
                    Log.d("MemoViewModel", "[DELETE] Removed server-only memo id = ${doc.id}")
                }
            }
            _backupResult.postValue(BackupResult(true))
            Log.d("MemoViewModel", "[SUCCESS] Backup completed successfully.")
        } catch (e: Exception) {
            _backupResult.postValue(BackupResult(false))
            Log.e("MemoViewModel", "[FAIL] Backup failed: ${e.message}", e)
        }
    }

    fun loadMemos() = launchIO {
        firestoreRepository.auth.currentUser?.uid ?: run {
            Log.d("MemoViewModel", "[SKIP] Load skipped - User not signed in.")
            return@launchIO
        }
        try {
            val serverMemos = firestoreRepository.load() // 서버에 백업된 메모 가져오기

            val decryptedMemos = mutableListOf<Memo>() // 복호화 완료 메모 임시 저장

            serverMemos.forEach {
                // 서버 저장용 Base64 문자열
                val cipherTextBase64 = it.content
                val ivBase64 = it.iv

                // Base64 -> ByteArray (AES 복호화는 ByteArray로만 가능)
                val cipherText = Base64.decode(cipherTextBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)

                // 복호화 (암호문 -> 평문)
                val plainText = EncryptionManager.decrypt(cipherText, iv)

                decryptedMemos.add(it.copy(id = 0, content = plainText))
            }
            memoRepository.deleteAllMemos()

            decryptedMemos.forEach {
                memoRepository.insertMemo(it)
            }
            _loadResult.postValue(LoadResult(true))
            Log.d("MemoViewModel", "[SUCCESS] Load completed successfully.")
        } catch (e: Exception) {
            _loadResult.postValue(LoadResult(false))
            Log.e("MemoViewModel", "[FAIL] Load failed: ${e.message}", e)
        }
    }
}