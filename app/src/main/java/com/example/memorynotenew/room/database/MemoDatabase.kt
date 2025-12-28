package com.example.memorynotenew.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.memorynotenew.room.dao.MemoDao
import com.example.memorynotenew.room.dao.TrashDao
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.room.entity.Trash

// Room이 내부적으로 구현체 자동 생성 (annotation processing) -> 추상 클래스로 선언
@Database(entities = [Memo::class, Trash::class], version = 4, exportSchema = false)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun trashDao() : TrashDao

    companion object { // 클래스 수준의 싱글톤 객체 (static처럼 동작)
        @Volatile // 멀티 스레드 환경에서 항상 최신 값을 읽도록 보장
        private var INSTANCE: MemoDatabase? = null
        private const val DB_NAME = "memo.db"

        fun getInstance(context: Context): MemoDatabase {
            // synchronized : 멀티 스레드 환경에서 DB 중복 생성 방지
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance // 새로 생성한 instance를 INSTANCE에 저장
                instance
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // trash 테이블 생성
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trash` (" + // 이미 있으면 생성 안 함
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`memoId` INTEGER NOT NULL, " +
                            "`content` TEXT NOT NULL, " +
                            "`deletedAt` INTEGER NOT NULL)"
                )
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // memo 테이블에 iv 컬럼 추가
                db.execSQL("ALTER TABLE MEMOS ADD COLUMN iv TEXT")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 새 trash 테이블 생성 (memoId 컬럼 제외)
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trash_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`content` TEXT NOT NULL, " +
                            "`deletedAt` INTEGER NOT NULL)"
                )
                // 기존 데이터 복사 (memeId 컬럼 제외)
                db.execSQL(
                    "INSERT INTO trash_new (id, content, deletedAt) " +
                            "SELECT id, content, deletedAt FROM trash"
                )
                db.execSQL("DROP TABLE trash") // 기존 trash 테이블 삭제

                // 새 테이블 이름을 trash로 변경
                db.execSQL("ALTER TABLE trash_new RENAME TO trash")
            }
        }
    }
}