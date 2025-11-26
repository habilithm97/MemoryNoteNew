package com.example.memorynotenew.model

// 결과 값을 담는 순수 데이터 객체이기 때문에 data class를 사용함
data class BackupResult(
    val isSuccess: Boolean,
    val count: Int = 0
)