package com.example.memorynotenew.room.trash

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DB 접근 메서드 정의 (DB 작업 캡슐화)
@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(trash: Trash)

    @Delete
    suspend fun deleteTrash(trash: Trash)

    @Query("select * from trash order by deletedAt")
    fun getAllTrash(): Flow<List<Trash>>

    // 오래된 항목 자동 삭제 (삭제한 시간 < ex 30일 전 -> 삭제)
    @Query("delete from trash where deletedAt < :timeLimit")
    suspend fun deleteOldTrash(timeLimit: Long)
}