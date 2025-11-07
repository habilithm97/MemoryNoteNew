package com.example.memorynotenew.repository

import android.util.Log
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val memoCollection = db.collection(Constants.MEMOS)
    private val trashCollection = db.collection(Constants.TRASH)

    // Room -> Firebase
    private suspend fun <T> backupItems(items: List<T>, collection: CollectionReference, itemType: String) {
        if (items.isEmpty()) return
        try {
            val batch = db.batch() // 여러 작업을 한 번에 처리하는 batch
            items.forEach { item ->
                val id = when (item) {
                    is Memo -> item.id.toString()
                    is Trash -> item.id.toString()
                    else -> throw IllegalArgumentException("Unsupported type.")
                }
                batch.set(collection.document(id), item)
            }
            // 한 번에 Firebase에 저장하고 끝날 때까지 대기
            batch.commit().await()
            Log.d("FirebaseRepository", "$itemType backup successful: ${items.size} items")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "$itemType backup failed: ${e.message}", e)
        }
    }

    suspend fun backupMemos(memos: List<Memo>) {
        backupItems(memos, memoCollection, Constants.MEMO)
    }

    suspend fun backupTrash(trash: List<Trash>) {
        backupItems(trash, trashCollection, Constants.TRASH)
    }
}