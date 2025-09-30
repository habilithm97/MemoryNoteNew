package com.example.memorynotenew.common

// 하나만 존재하는 싱글톤 객체
object Constants {
    const val MEMO = "memo"
    const val MEMOS = "memos"
    const val DELETE_COUNT = "delete_count"
    const val PASSWORD_PREF = "password_preference"
    const val PURPOSE = "password_purpose"
    // 30일 * 24시간 * 60분 * 60초 * 1000밀리초
    const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    // 1일 * 24시간 * 60분 * 60초 * 1000밀리초
    const val ONE_DAYS_MS = 24L * 60 * 60 * 1000
}