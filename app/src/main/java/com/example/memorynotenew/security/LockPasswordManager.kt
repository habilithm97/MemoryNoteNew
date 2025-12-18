package com.example.memorynotenew.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object LockPasswordManager {
    private const val FILE_NAME = "secret_shared_prefs"
    private const val LOCK_PASSWORD_KEY = "password_key"

    // 암호화된 SharedPreferences 가져오기
    private fun prefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext // 메모리 누수 방지

        // AES256-GCM 방식으로 MasterKey (보안 키) 생성
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // EncryptedSharedPreferences 생성 (자동으로 암호화/복호화 처리)
        return EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, // 키 암호화 방식
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // 값 암호화 방식
        )
    }

    fun saveLockPassword(context: Context, password: String) {
        prefs(context).edit().putString(LOCK_PASSWORD_KEY, password).apply() // apply()로 비동기 저장
    }

    fun getLockPassword(context: Context): String? =
        prefs(context).getString(LOCK_PASSWORD_KEY, null)
}