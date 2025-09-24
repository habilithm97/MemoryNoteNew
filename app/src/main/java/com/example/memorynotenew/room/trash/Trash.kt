package com.example.memorynotenew.room.trash

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// DB 테이블에 매핑되는 데이터 클래스
@Parcelize // Parcelable 자동 구현
@Entity
data class Trash(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoId: Long, // 원래 메모 id
    val content: String,
    val deleteAt: Long // 삭제된 시간
) : Parcelable // 컴포넌트 간 데이터 전달을 위한 직렬화 (객체 -> 바이트 형태)