package com.example.memorynotenew.repository

import android.util.Log
import com.example.memorynotenew.common.Constants.CONTENT
import com.example.memorynotenew.common.Constants.DATE
import com.example.memorynotenew.common.Constants.IS_LOCKED
import com.example.memorynotenew.common.Constants.IV
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.USERS
import com.example.memorynotenew.room.entity.Memo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun backup(memos: List<Memo>) {
        if (memos.isEmpty()) {
            Log.d("FirebaseRepository", "[SKIP] No memos to back up.")
            return
        }
        try {
            val uid = auth.currentUser?.uid ?: return

            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            val batch = db.batch() // 여러 작업을 한 번에 처리하는 batch
            memos.forEach {
                batch.set(memoCollection.document(it.id.toString()), it)
            }
            batch.commit().await()
            Log.d("FirebaseRepository", "[SUCCESS] Backup completed successfully.")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "[FAIL] Backup failed: ${e.message}", e)
        }
    }

    suspend fun load(): List<Memo> {
        try {
            val uid = auth.currentUser?.uid ?: return emptyList()

            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            val snapshot = memoCollection.get().await()
            if (snapshot.isEmpty) {
                Log.d("FirebaseRepository", "[SKIP] No memos found on server.")
                return emptyList()
            }
            val memos = snapshot.documents.mapNotNull {
                val content = it.getString(CONTENT) ?: return@mapNotNull null
                val date = it.getLong(DATE) ?: return@mapNotNull null
                val isLocked = it.getBoolean(IS_LOCKED) ?: false
                val iv = it.getString(IV) ?: return@mapNotNull null

                Memo(
                    id = 0,
                    content = content,
                    date = date,
                    isLocked = isLocked,
                    iv = iv
                )
            }
            return memos
            Log.d("FirebaseRepository", "[SUCCESS] Load completed successfully.")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "[FAIL] Load failed: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun delete(itemId: String, baseCollection: String) {
        try {
            val uid = auth.currentUser?.uid ?: return
            // Firebase 경로 : users/{uid}/{baseCollection}/{itemId}
            db.collection(USERS)
                .document(uid)
                .collection(baseCollection)
                .document(itemId)
                .delete()
                .await() // 코루틴에서 작업이 끝날 때까지 대기 (비동기 처리)
            Log.d("FirebaseRepository", "[SUCCESS] Deleted $baseCollection item (id = $itemId) from server")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "[FAIL] Delete failed: ${e.message}", e)
        }
    }
}