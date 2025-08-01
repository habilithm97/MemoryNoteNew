package com.example.memorynotenew.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PasswordManager {
    private const val FILE_NAME = "secret_shared_prefs"
    private const val PASSWORD_KEY = "password_key"

    fun savePassword(context: Context, password: String) {
        // AES256-GCM 방식으로 MasterKey 생성 (보안을 위한 기본키)
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // 암호화된 sharedPreferences 객체 생성
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKeyAlias, // 생성한 보안키로 암호화
            // 키(이름) 암호화 방식
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            // 값(데이터) 암호화 방식
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        // 암호화된 sharedPreferences에 비밀번호 저장
        with(sharedPreferences.edit()) {
            putString(PASSWORD_KEY, password)
            apply() // 비동기 방식으로 커밋 (commit()보다 성능 좋음)
        }
    }

    fun getSavedPassword(context: Context): String? {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString("password_key", null)
    }
}