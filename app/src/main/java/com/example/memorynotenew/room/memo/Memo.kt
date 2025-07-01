package com.example.memorynotenew.room.memo

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// DB 테이블에 매핑되는 데이터 클래스
@Parcelize // Parcelable 자동 구현
@Entity(tableName = "memos")
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var content: String,
    var date: Long,
    var isLocked: Boolean = false
) : Parcelable // 컴포넌트 간 데이터 전달을 위한 직렬화 (객체 -> 바이트 형태)