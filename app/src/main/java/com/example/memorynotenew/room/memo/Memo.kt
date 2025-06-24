package com.example.memorynotenew.room.memo

import androidx.room.Entity
import androidx.room.PrimaryKey

// DB 테이블에 매핑되는 데이터 클래스
@Entity(tableName = "memos")
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var content: String,
    var date: Long,
    var isLocked: Boolean = false
)