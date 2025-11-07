package com.example.memorynotenew.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memorynotenew.room.entity.Trash
import kotlinx.coroutines.flow.Flow

// DB 접근 메서드 정의 (DB 작업 캡슐화)
@Dao
interface TrashDao {
    // suspend : 코루틴 내에서 일시 중단 가능 (비동기 실행)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(trash: Trash)

    @Delete
    suspend fun deleteTrash(trash: Trash)

    /* Flow
    -비동기 데이터 스트림 처리 (코루틴 기반)
    -순차적/지속적 데이터 방출 (emit)
    -구독 방식으로 실시간 데이터 수신 (collect)
    -UI 생명주기와 독립적 (LiveData와의 차이점) */
    @Query("select * from trash order by deletedAt")
    fun getAllTrash(): Flow<List<Trash>>

    // 삭제 기준 시각보다 오래된 데이터만 삭제
    @Query("delete from trash where deletedAt < :cutoffTime")
    suspend fun deleteOldTrash(cutoffTime: Long)

    @Query("delete from trash")
    suspend fun deleteAllTrash()
}