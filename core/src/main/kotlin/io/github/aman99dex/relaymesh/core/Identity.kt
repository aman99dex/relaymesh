package io.github.aman99dex.relaymesh.core

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Long-term identity: one Ed25519 key pair. The public key *is* the user's address —
 * no phone number, no account, no server. Later keys (Signal sessions, devices) get
 * signed by this root key, so it is the single thing a contact has to verify.
 */
class Identity private constructor(private val priv: Ed25519PrivateKeyParameters) {
    val publicKey: ByteArray = priv.generatePublicKey().encoded

    fun privateKeyBytes(): ByteArray = priv.encoded

    fun sign(message: ByteArray): ByteArray = Ed25519Signer().run {
        init(true, priv)
        update(message, 0, message.size)
        generateSignature()
    }

    companion object {
        const val KEY_SIZE = 32
        const val SIG_SIZE = 64

        fun generate() = Identity(Ed25519PrivateKeyParameters(SecureRandom()))

        fun fromPrivateKey(bytes: ByteArray): Identity {
            require(bytes.size == KEY_SIZE) { "Ed25519 private key must be $KEY_SIZE bytes" }
            return Identity(Ed25519PrivateKeyParameters(bytes, 0))
        }

        fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
            if (publicKey.size != KEY_SIZE || signature.size != SIG_SIZE) return false
            return Ed25519Signer().run {
                init(false, Ed25519PublicKeyParameters(publicKey, 0))
                update(message, 0, message.size)
                verifySignature(signature)
            }
        }
    }
}

/** Human-comparable fingerprint: first 160 bits of SHA-256(publicKey), as 10 hex groups. */
fun fingerprint(publicKey: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(publicKey).take(20)
        .chunked(2) { "%02X%02X".format(it[0], it[1]) }
        .joinToString(" ")

fun ByteArray.toB64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)
fun String.fromB64(): ByteArray = Base64.getUrlDecoder().decode(this)
