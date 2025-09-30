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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(trash: Trash)

    @Delete
    suspend fun deleteTrash(trash: Trash)

    @Query("select * from trash order by deletedAt")
    fun getAllTrash(): Flow<List<Trash>>

    // 삭제한 시간 < 오늘부터 30일 전 -> 자동 삭제
    // ex) 8월 20일 < 8월 31일 -> 삭제o
    // ex) 9월 20일 > 8월 31일 -> 삭제x
    @Query("delete from trash where deletedAt < :cutoffTime")
    suspend fun deleteOldTrash(cutoffTime: Long)
}