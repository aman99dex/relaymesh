package io.github.aman99dex.relaymesh

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Envelope encryption for on-device secrets. One AES-256 master key lives inside the Android
 * Keystore (StrongBox secure chip when the phone has one) and can never be exported. Our real
 * secrets (identity key, database key) are stored on disk only in sealed form.
 */
object KeyVault {
    private const val ALIAS = "relaymesh_master"
    private const val IV_SIZE = 12

    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as SecretKey?)?.let { return it }

        fun generate(strongBox: Boolean): SecretKey =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                if (strongBox && Build.VERSION.SDK_INT >= 28) spec.setIsStrongBoxBacked(true)
                init(spec.build())
                generateKey()
            }
        // StrongBoxUnavailableException extends ProviderException; caught as the parent so API 26-27 still loads.
        return try { generate(strongBox = true) } catch (_: ProviderException) { generate(strongBox = false) }
    }

    fun seal(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, masterKey()) }
        return c.iv + c.doFinal(plain)
    }

    fun open(sealed: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, sealed, 0, IV_SIZE))
        return c.doFinal(sealed, IV_SIZE, sealed.size - IV_SIZE)
    }

    /** Reads the sealed secret in [file], creating it with [create] on first use. Writes are atomic. */
    fun secret(file: File, create: () -> ByteArray): ByteArray {
        val af = AtomicFile(file)
        try { return open(af.readFully()) } catch (_: FileNotFoundException) { }
        return create().also { plain ->
            val out = af.startWrite()
            try { out.write(seal(plain)); af.finishWrite(out) } catch (e: IOException) { af.failWrite(out); throw e }
        }
    }
}
