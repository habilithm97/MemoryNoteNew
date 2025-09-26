package com.example.memorynotenew.repository

import com.example.memorynotenew.room.dao.MemoDao
import com.example.memorynotenew.room.dao.TrashDao
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash
import kotlinx.coroutines.flow.Flow

/* 데이터 저장소와 ViewModel 사이의 다리
 -ViewModel은 데이터 출처를 몰라도 됨 (데이터 소스 추상화)
 */
class MemoRepository(
    private val memoDao: MemoDao,
    private val trashDao: TrashDao
) {
    suspend fun insertMemo(memo: Memo) {
        memoDao.insertMemo(memo)
    }

    suspend fun updateMemo(memo: Memo) {
        memoDao.updateMemo(memo)
    }

    suspend fun deleteMemo(memo: Memo) {
        memoDao.deleteMemo(memo)
    }

    fun getAllMemos(): Flow<List<Memo>> {
        return memoDao.getAllMemos()
    }

    suspend fun insertTrash(trash: Trash) {
        trashDao.insertTrash(trash)
    }

    suspend fun deleteTrash(trash: Trash) {
        trashDao.deleteTrash(trash)
    }

    fun getAllTrash(): Flow<List<Trash>> {
        return trashDao.getAllTrash()
    }

    suspend fun deleteOldTrash(limit: Long) {
        trashDao.deleteOldTrash(limit)
    }
}