package io.github.aman99dex.relaymesh.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentityTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun `matches RFC 8032 test vector 1`() {
        val id = Identity.fromPrivateKey(hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"))
        assertContentEquals(hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"), id.publicKey)
        assertContentEquals(
            hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"),
            id.sign(ByteArray(0)),
        )
    }

    @Test fun `verify rejects tampered message and wrong sizes`() {
        val id = Identity.generate()
        val sig = id.sign("hi".toByteArray())
        assertTrue(Identity.verify(id.publicKey, "hi".toByteArray(), sig))
        assertFalse(Identity.verify(id.publicKey, "ho".toByteArray(), sig))
        assertFalse(Identity.verify(id.publicKey.copyOf(31), "hi".toByteArray(), sig))
    }

    @Test fun `private key round-trips`() {
        val id = Identity.generate()
        assertContentEquals(id.publicKey, Identity.fromPrivateKey(id.privateKeyBytes()).publicKey)
    }

    @Test fun `fingerprint is 10 groups of 4 hex`() {
        assertTrue(Regex("([0-9A-F]{4} ){9}[0-9A-F]{4}").matches(fingerprint(Identity.generate().publicKey)))
    }

    @Test fun `card round-trips`() {
        val id = Identity.generate()
        val card = ContactCard.decode(ContactCard("Aman", id.publicKey).encode(id))!!
        assertEquals("Aman", card.name)
        assertContentEquals(id.publicKey, card.publicKey)
    }

    @Test fun `card with swapped name is rejected`() {
        val id = Identity.generate()
        val parts = ContactCard("Aman", id.publicKey).encode(id).split(":").toMutableList()
        parts[3] = "Mallory".toByteArray().toB64()
        assertNull(ContactCard.decode(parts.joinToString(":")))
    }

    @Test fun `garbage never throws`() {
        listOf("", "hello", "relaymesh:1:", "relaymesh:1:a:b:c", "relaymesh:1:!!:!!:!!", "relaymesh:2:x:y:z")
            .forEach { assertNull(ContactCard.decode(it), it) }
    }
}
