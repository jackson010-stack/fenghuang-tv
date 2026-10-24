package cn.dytt.replica.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 电影天堂 APK（cc.vod.film）接口的加解密方案。
 *
 * 响应体形如 {"code":1,"msg":"...","data":"<hex>"}，data 是 AES/CFB/NoPadding 密文的十六进制，
 * 但布局被刻意打乱：IV 夹在密文中间 —— cipherHex[0:16] + ivHex(32) + cipherHex[16:]。
 */
object Crypto {

    private const val KEY_TEXT = "UeFk58Si151OmxqH"
    private const val TRANSFORM = "AES/CFB/NOPadding"

    private val secretKey: SecretKeySpec =
        SecretKeySpec(KEY_TEXT.toByteArray(Charsets.UTF_8), "AES")

    fun decrypt(payload: String): String {
        val first: String
        val ivHex: String
        val tail: String
        if (payload.length >= 48) {
            first = payload.substring(0, 16)
            ivHex = payload.substring(16, 48)
            tail = payload.substring(48)
        } else {
            val cut = payload.length - 32
            first = payload.substring(0, cut)
            ivHex = payload.substring(cut)
            tail = ""
        }
        val iv = unhex(ivHex.padStart(32, '0'))
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return String(cipher.doFinal(unhex(first + tail)), Charsets.UTF_8)
    }

    fun encrypt(plain: String): String {
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val hex = hex(cipher.doFinal(plain.toByteArray(Charsets.UTF_8))).uppercase()
        return hex.substring(0, 16) + hex(iv).uppercase() + hex.substring(16)
    }

    private fun unhex(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        for (i in out.indices) {
            out[i] = ((Character.digit(s[i * 2], 16) shl 4) or Character.digit(s[i * 2 + 1], 16)).toByte()
        }
        return out
    }

    private fun hex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append("%02x".format(x))
        return sb.toString()
    }
}
