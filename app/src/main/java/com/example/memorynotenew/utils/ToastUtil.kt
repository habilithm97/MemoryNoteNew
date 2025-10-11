package com.example.memorynotenew.utils

import android.content.Context
import android.widget.Toast

object ToastUtil {
    private var toast: Toast? = null

    fun Context.showToast(message: String) { // Context 확장 함수
        toast?.cancel() // 기존 Toast 취소
        // applicationContext -> 안전
        toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
        toast?.show()
    }
}