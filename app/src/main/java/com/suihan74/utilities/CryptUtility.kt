package com.suihan74.utilities

import android.util.Base64
import java.io.Serializable
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptUtility {
    private const val ALGORITHM = "AES"
    private const val MODE = "AES/CBC/PKCS5Padding"
    private const val BLOCK_SIZE = 32

    data class EncryptedData(
        val data : String,
        val iv : String
    ) : Serializable

    fun encrypt(dataStr: String, secretKeyStr: String) : EncryptedData {
        val keyBytes = ByteArray(BLOCK_SIZE)
        val secret = secretKeyStr.toByteArray()
        for (i in 0 until BLOCK_SIZE) {
            keyBytes[i] = secret[i % secret.size]
        }

        val key = SecretKeySpec(keyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(MODE).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }

        val encryptedData = cipher.doFinal(dataStr.toByteArray())

        return EncryptedData(
            Base64.encodeToString(encryptedData, Base64.NO_WRAP),
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
    }

    fun decrypt(data: EncryptedData, secretKeyStr: String) : String {
        val keyBytes = ByteArray(BLOCK_SIZE)
        val secret = secretKeyStr.toByteArray()
        for (i in 0 until BLOCK_SIZE) {
            keyBytes[i] = secret[i % secret.size]
        }

        val key = SecretKeySpec(keyBytes, ALGORITHM)
        val ips = IvParameterSpec(Base64.decode(data.iv, Base64.NO_WRAP))

        val cipher = Cipher.getInstance(MODE).apply {
            init(Cipher.DECRYPT_MODE, key, ips)
        }

        val res = cipher.doFinal(Base64.decode(data.data, Base64.NO_WRAP))
        return res.toString(Charset.defaultCharset())
    }
}
