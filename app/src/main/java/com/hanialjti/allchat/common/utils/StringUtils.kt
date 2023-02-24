package com.hanialjti.allchat.common.utils

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

object StringUtils {
    fun sha1(bytes: ByteArray): String? {
        return try {
            val crypt: MessageDigest = MessageDigest.getInstance("SHA-1")
            crypt.reset()
            crypt.update(bytes)
            byteToHex(crypt.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }

    private fun byteToHex(hash: ByteArray): String {
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x", b)
        }
        val result: String = formatter.toString()
        formatter.close()
        return result
    }
}