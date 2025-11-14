package com.example.memorynotenew.repository

import android.util.Log
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun backupMemos(memos: List<Memo>) {
        backupItems(memos, Constants.MEMO, Constants.MEMOS)
    }

    suspend fun backupTrash(trash: List<Trash>) {
        backupItems(trash, Constants.TRASH, Constants.TRASH)
    }

    private suspend fun <T> backupItems(
        items: List<T>,
        baseCollection: String,
        itemType: String
    ) {
        if (items.isEmpty()) return
        try {
            val uid = auth.currentUser?.uid ?: return // 로그인 사용자 UID
            val collection = db.collection(Constants.USERS)
                .document(uid) // 사용자별 문서
                .collection(baseCollection) // 하위 컬렉션 (메모/휴지통)

            val batch = db.batch() // 여러 작업을 한 번에 처리하는 batch
            items.forEach {
                val id = when (it) {
                    is Memo -> it.id.toString()
                    is Trash -> it.id.toString()
                    else -> throw IllegalArgumentException("Unsupported type.")
                }
                batch.set(collection.document(id), it)
            }
            batch.commit().await()
            Log.d("FirebaseRepository", "$itemType backup successful: ${items.size} items")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "$itemType backup failed: ${e.message}", e)
        }
    }

    suspend fun delete(itemId: String, baseCollection: String) {
        try {
            val uid = auth.currentUser?.uid ?: return
            // Firebase 경로 : users/{uid}/{baseCollection}/{itemId}
            db.collection(Constants.USERS)
                .document(uid)
                .collection(baseCollection)
                .document(itemId)
                .delete()
                .await() // 코루틴에서 작업이 끝날 때까지 기다림 (비동기 처리)
            Log.d("FirebaseRepository", "$baseCollection item $itemId deleted successfully")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to delete $itemId from $baseCollection: ${e.message}", e)
        }
    }
}