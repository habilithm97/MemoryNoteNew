package com.example.memorynotenew.repository

import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Room 데이터와 Firebase Firestore를 연결
class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val memoCollection = db.collection(Constants.MEMOS)
    private val trashCollection = db.collection(Constants.TRASH)

    // Room -> Firebase
    // .set(it) : 문서가 없으면 생성, 있으면 덮어씀
    // .await() : 코루틴 내에서 Firebase 작업이 끝날 때까지 대기
    suspend fun syncMemos(memos: List<Memo>) {
        memos.forEach { memoCollection.document(it.id.toString()).set(it).await() }
    }
    suspend fun syncTrash(trash: List<Trash>) {
        trash.forEach { trashCollection.document(it.id.toString()).set(it).await() }
    }

    // Firebase -> Room
    fun observeMemos(onChange: (List<Memo>) -> Unit) {
        // Firestore 실시간 리스너 (컬렉션 변경 시 호출)
        // snapshot : 현재 컬렉션 상태를 담고 있음
        memoCollection.addSnapshotListener { snapshot, _ ->
            val memos = snapshot?.toObjects(Memo::class.java) ?: emptyList()
            onChange(memos)
        }
    }
    fun observeTrash(onChange: (List<Trash>) -> Unit) {
        trashCollection.addSnapshotListener { snapshot, _ ->
            val trash = snapshot?.toObjects(Trash::class.java) ?: emptyList()
            onChange(trash)
        }
    }
}