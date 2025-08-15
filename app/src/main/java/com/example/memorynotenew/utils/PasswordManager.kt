package com.example.memorynotenew.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PasswordManager {
    private const val FILE_NAME = "secret_shared_prefs"
    private const val PASSWORD_KEY = "password_key"

    // AES256-GCM 방식으로 MasterKey 생성 (보안키)
    private fun getMasterKey(context: Context) = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 암호화된 sharedPreferences 객체 생성
    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        getMasterKey(context), // 생성한 보안키로 암호화
        // 키(이름) 암호화 방식
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        // 값(데이터) 암호화 방식
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 암호화된 sharedPreferences에 비밀번호 저장
    fun savePassword(context: Context, password: String) {
        val sharedPreferences = getEncryptedPrefs(context)
        // 비동기 방식으로 저장 (commit()보다 빠르지만 저장 성공 여부는 알 수 없음)
        sharedPreferences.edit().putString(PASSWORD_KEY, password).apply()
    }

    fun getPassword(context: Context): String? {
        val sharedPreferences = getEncryptedPrefs(context)
        return sharedPreferences.getString(PASSWORD_KEY, null)
    }
}