package com.example.memorynotenew.room.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.memorynotenew.common.Constants.IS_LOCKED
import com.example.memorynotenew.common.Constants.MEMOS
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

// DB 테이블에 매핑되는 데이터 클래스
@Parcelize // Parcelable 자동 구현
@Entity(tableName = MEMOS)
data class Memo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var content: String,
    var date: Long,
    // Firestore에서 "isLocked" 필드 이름을 명시적으로 지정
    @get:PropertyName(IS_LOCKED)
    @set:PropertyName(IS_LOCKED)
    var isLocked: Boolean = false
) : Parcelable // 컴포넌트 간 데이터 전달을 위한 직렬화 (객체 -> 바이트 형태)