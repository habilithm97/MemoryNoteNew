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

class FirestoreRepository {
    val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun backup(memos: List<Memo>) {
        if (memos.isEmpty()) {
            Log.d("FirestoreRepository", "[SKIP] No memos to back up.")
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Log.d("FirestoreRepository", "[SKIP] Backup skipped - User not signed in.")
            return
        }
        try {
            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            val batch = db.batch() // 여러 작업을 한 번에 처리하는 batch

            memos.forEach {
                batch.set(memoCollection.document(it.id.toString()), it)
            }
            batch.commit().await()
            Log.d("FirestoreRepository", "[SUCCESS] Backup completed successfully.")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "[FAIL] Backup failed: ${e.message}", e)
        }
    }

    suspend fun load(): List<Memo> {
        val uid = auth.currentUser?.uid ?: run {
            Log.d("FirestoreRepository", "[SKIP] Load skipped - User not signed in.")
            return emptyList()
        }
        try {
            val memoCollection = db.collection(USERS)
                .document(uid)
                .collection(MEMO)

            /* snapshot
             -Firestore에서 데이터를 가져올 때 반환되는 실제 데이터 및 메타 정보는 담은 객체
             -단순 데이터 리스트가 아니라 문서 컬렉션에 대한 상태를 포함 */
            val snapshot = memoCollection.get().await()

            if (snapshot.isEmpty) {
                Log.d("FirestoreRepository", "[SKIP] No memos found on server.")
                return emptyList()
            }
            val memos = snapshot.documents.mapNotNull {
                val content = it.getString(CONTENT) ?: return@mapNotNull null
                val date = it.getLong(DATE) ?: return@mapNotNull null
                val isLocked = it.getBoolean(IS_LOCKED) ?: false
                val iv = it.getString(IV) ?: return@mapNotNull null

                Memo(id = 0,
                    content = content,
                    date = date,
                    isLocked = isLocked,
                    iv = iv
                )
            }
            Log.d("FirestoreRepository", "[SUCCESS] Load completed successfully.")
            return memos
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "[FAIL] Load failed: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun delete(itemId: String, baseCollection: String) {
        val uid = auth.currentUser?.uid ?: run {
            Log.d("FirestoreRepository", "[SKIP] Delete skipped - User not signed in.")
            return
        }
        try { // Firestore 경로 : users/{uid}/baseCollection/{itemId}
            db.collection(USERS)
                .document(uid)
                .collection(baseCollection)
                .document(itemId)
                .delete()
                .await()
            Log.d("FirestoreRepository", "[SUCCESS] Deleted $baseCollection item (id = $itemId) from server")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "[FAIL] Delete failed: ${e.message}", e)
        }
    }
}

/**
 * await()
  -작업이 완료될 때까지 코루틴 일시 중단
   -> 결과를 안전하게 받고, UI 스레드를 막지 않기 위해
 */