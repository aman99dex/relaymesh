package io.github.aman99dex.relaymesh.core

/**
 * What a contact QR code carries: `relaymesh:1:<publicKey>:<name>:<signature>` (base64url fields).
 *
 * The card is self-signed, which proves the sender holds the private key and that the name
 * wasn't altered after signing. It does NOT prove who is holding the phone — that is what
 * comparing fingerprints in person is for.
 */
class ContactCard(val name: String, val publicKey: ByteArray) {

    fun encode(signer: Identity): String {
        require(signer.publicKey.contentEquals(publicKey)) { "Card must be signed by its own key" }
        val sig = signer.sign(signedBytes(name, publicKey))
        return listOf(PREFIX, publicKey.toB64(), name.toByteArray().toB64(), sig.toB64()).joinToString(":")
    }

    companion object {
        private const val PREFIX = "relaymesh:1"
        const val MAX_NAME = 64

        // Domain separation: a signature made for a card can never be replayed as some other message type.
        private fun signedBytes(name: String, publicKey: ByteArray) =
            "relaymesh-card-v1".toByteArray() + publicKey + name.toByteArray()

        /** Returns null for anything that isn't a valid, correctly signed card. Never throws on bad input. */
        fun decode(text: String): ContactCard? {
            if (!text.startsWith("$PREFIX:")) return null
            val parts = text.removePrefix("$PREFIX:").split(":")
            if (parts.size != 3) return null
            return runCatching {
                val pub = parts[0].fromB64()
                val name = String(parts[1].fromB64())
                val sig = parts[2].fromB64()
                if (name.isBlank() || name.length > MAX_NAME) return null
                if (!Identity.verify(pub, signedBytes(name, pub), sig)) return null
                ContactCard(name, pub)
            }.getOrNull()
        }
    }
}
