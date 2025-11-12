package com.example.memorynotenew.common

object Constants {
    const val MEMO = "memo"
    const val MEMOS = "memos"
    const val TRASH = "trash"
    const val USERS = "users"

    const val SIGN_IN_PREF = "sign_in_preference"
    const val PW_PREF = "password_preference"
    const val BACKUP_PREF = "backup_preference"
    const val SIGN_OUT_PREF = "sign_out_preference"

    const val PURPOSE = "password_purpose" // 비밀번호 사용 목적
    const val COUNT = "delete_count" // 삭제한 메모 개수

    // 30일 * 24시간 * 60분 * 60초 * 1000밀리초
    const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    // 1일 * 24시간 * 60분 * 60초 * 1000밀리초
    const val ONE_DAYS_MS = 24L * 60 * 60 * 1000
    const val MAX_TRASH_DAYS = 30
}