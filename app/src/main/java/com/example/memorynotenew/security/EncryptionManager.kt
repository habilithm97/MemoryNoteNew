package com.example.memorynotenew.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {
    // Android Keystore 안에서 사용할 AES 키 이름 (별칭, 앱 외부에서 절대 읽을 수 없음)
    private const val KEY_ALIAS = "memo_aes_key"
    private const val ANDROID_KS = "AndroidKeyStore"

    private fun getOrCreateKey(): SecretKey {
        val keystore = KeyStore.getInstance(ANDROID_KS).apply {
            load(null) // Keystore 초기화
        }
        // 키가 있으면 가져오기
        keystore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey // 가져온 키를 SecretKey로 변환해 반환
        }
        // 키가 없으면 생성
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, // AES 알고리즘
            ANDROID_KS // 키 생성 위치
        )
        // AES 키 설정
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) // 암호화/복호화
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM) // GCM 모드 (안전한 암호화 + 무결성 제공)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) // 패딩 없음 (GCM 모드는 패딩 필요 없음)
            .setRandomizedEncryptionRequired(true) // 매번 랜덤 IV 사용
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    /** 문자열 암호화 */
    // 입력 : 평문 문자열, 출력 : 암호문 + IV
    fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getOrCreateKey()

        cipher.init(Cipher.ENCRYPT_MODE, secretKey) // 암호화 모드로 초기화 (랜덤 IV 자동 생성)
        val iv = cipher.iv // 랜덤 IV (복호화 시 필요)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)) // 평문 -> 암호문
        return Pair(cipherText, iv)
    }

    /** 문자열 복호화 */
    fun decrypt(cipherText: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getOrCreateKey()

        val spec = GCMParameterSpec(128, iv) // 인증 태그 길이 128
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec) // 복호화 모드로 초기화
        val plainBytes = cipher.doFinal(cipherText) // 암호문 -> 평문
        return String(plainBytes, Charsets.UTF_8)
    }
}

/**
 * AES (Advanced Encryption Standard) : 대칭키 암호화 알고리즘
 * IV (Initialization Vector) : 암호화 시작점을 랜덤하게 만드는 값, 보안 필수
 * Cipher : 암호화/복호화를 실제로 수행하는 객체
 */