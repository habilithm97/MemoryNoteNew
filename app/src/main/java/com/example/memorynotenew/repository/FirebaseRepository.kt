package com.example.memorynotenew.repository

import android.util.Log
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val memoCollection = db.collection(Constants.MEMOS)
    private val trashCollection = db.collection(Constants.TRASH)

    // Room -> Firebase
    suspend fun backupMemos(memos: List<Memo>) {
        if (memos.isEmpty()) return
        try {
            val batch = db.batch() // 여러 작업을 한 번에 처리하도록 준비
            memos.forEach { memo ->
                batch.set(memoCollection.document(memo.id.toString()), memo)
            }
            // 한 번에 Firebase에 저장하고 끝날 때까지 대기
            batch.commit().await()
            Log.d("FirebaseRepository", "메모 ${memos.size}개 백업 성공")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "메모 백업 실패: ${e.message}", e)
        }
    }
    suspend fun backupTrash(trash: List<Trash>) {
        if (trash.isEmpty()) return
        try {
            val batch = db.batch()
            trash.forEach {
                batch.set(trashCollection.document(it.id.toString()), it)
            }
            batch.commit().await()
            Log.d("FirebaseRepository", "휴지통 메모 ${trash.size}개 백업 성공")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "휴지통 메모 백업 실패: ${e.message}", e)
        }
    }
}