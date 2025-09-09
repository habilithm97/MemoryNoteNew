package com.example.memorynotenew.room.memo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room이 내부적으로 구현체 자동 생성 (annotation processing) -> 추상 클래스로 선언
@Database(entities = [Memo::class], version = 1, exportSchema = false)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object { // 클래스 수준의 싱글톤 객체 (static처럼 동작)
        @Volatile // 여러 스레드가 항상 최신 값을 읽도록 보장
        private var INSTANCE: MemoDatabase? = null
        private const val DB_NAME = "memo.db"

        fun getInstance(context: Context): MemoDatabase {
            // synchronized : 멀티 스레드 환경에서 DB 중복 생성 방지
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java, DB_NAME)
                    .build()
                INSTANCE = instance // 새로 생성한 instance를 INSTANCE에 저장
                instance
            }
        }
    }
}