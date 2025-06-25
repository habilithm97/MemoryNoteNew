package com.example.memorynotenew.repository

import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.room.memo.MemoDao
import kotlinx.coroutines.flow.Flow

/* 데이터 저장소와 ViewModel 사이의 다리
 -ViewModel은 데이터 출처를 몰라도 됨 (데이터 소스 추상화)
 */
class MemoRepository(private val memoDao: MemoDao) {
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
}