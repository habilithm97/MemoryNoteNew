package com.example.memorynotenew.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Query
import com.example.memorynotenew.room.entity.Memo
import kotlinx.coroutines.flow.Flow

// DB 접근 메서드 정의 (DB 작업 캡슐화)
@Dao
interface MemoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: Memo)

    @Update
    suspend fun updateMemo(memo: Memo)

    @Delete
    suspend fun deleteMemo(memo: Memo)

    @Query("select * from memos order by date")
    fun getAllMemos(): Flow<List<Memo>>
}

/*
* suspend
 -오래 걸리는 작업 비동기 처리 (Room DB, 네트워크)
 -UI 스레드 차단 없이 중단/재개

* Flow
 -비동기 데이터 스트림 처리 (코루틴 기반)
 -순차적/지속적 데이터 방출 (emit)
 -UI 생명주기와 독립적 (LiveData와의 차이점)
 -구독 방식으로 실시간 데이터 수신 (collect)
 */