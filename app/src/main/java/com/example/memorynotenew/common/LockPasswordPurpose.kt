package com.example.memorynotenew.common

enum class LockPasswordPurpose {
    SETTING, // 설정
    LOCK, // 메모 잠금 및 잠금 해제
    OPEN, // 메모 열기
    DELETE, // 메모 삭제
    BACKUP // 메모 백업
}