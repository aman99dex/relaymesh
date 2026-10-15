# LEARN M0: identity, contact exchange, and encryption at rest

## 1. Your identity is a key pair
`core/.../Identity.kt` creates an **Ed25519** key pair. The private key signs; the public key
verifies, and it doubles as your address. No phone number or server is involved, so nobody can
take your account away.

**Try it:** read `IdentityTest`, `matches RFC 8032 test vector 1`. Test vectors are published
input/output pairs. If your crypto matches them, you're using the primitive correctly. Always
test crypto against vectors, not just round-trips: a round-trip test passes even when both
sides make the same mistake.

## 2. A contact card is signed, but signing isn't verifying
`ContactCard.kt` puts `pubkey + name + signature` into a QR code. The signature proves:
- the sender holds the private key for that public key, and
- nobody changed the name after it was signed (see the test `card with swapped name is rejected`).

It does **not** prove *who* is holding the phone. An attacker can show you *their own* valid
card. That's why the app shows a **fingerprint**: you compare it with your contact in person,
or over a channel you already trust. Signal's "safety numbers" are the same idea.

**Domain separation:** we sign `"relaymesh-card-v1" + key + name`, not just `key + name`. Without
the prefix, a signature made for a card could be replayed as a valid signature on some other
message type that happens to have the same bytes.

## 3. Encryption at rest: envelope encryption
Problem: the identity key and the database must be encrypted, but the *encryption key* has to
be stored somewhere.

```
Android Keystore (hardware) ── AES-256 master key, can never be exported
        │ seals (AES-GCM)
        ├── files/identity.key   → Ed25519 private key
        └── files/db.key         → random 32 bytes → SQLCipher encrypts relaymesh.db
```

- **Keystore / StrongBox:** the master key lives in a secure chip (StrongBox) or the TEE.
  Even with root access, an attacker can only *ask* the chip to decrypt on that same device.
  They can't copy the key off it.
- **AES-GCM** is *authenticated* encryption: tampering with the sealed file makes decryption
  fail instead of returning garbage. The 12-byte IV is stored in front of the ciphertext.
- **AtomicFile:** if the phone dies mid-write, you keep the old file instead of a corrupted
  half-written key. Losing the identity key would mean losing your identity.

## 4. Things done on purpose
- `allowBackup=false` + `data_extraction_rules.xml`: Google backups never get the database.
  The data would be useless anyway, because the Keystore key doesn't leave the phone.
- `FLAG_SECURE`: blocks screenshots and recent-apps thumbnails of chats.
- `core/` has no Android imports, so `./gradlew :core:test` runs on your PC in seconds.

## Quiz
1. Why is a round-trip test (sign, then verify) not enough to check an Ed25519 implementation?
2. An attacker swaps the QR code on a poster for their own. What stops them, and what doesn't?
3. Why not store the SQLCipher password directly in the Keystore?
   (Hint: what can a Keystore key do, and what can't it do?)
4. What happens if someone restores this app's data on a different phone?

<details><summary>Answers</summary>

1. A buggy implementation can be consistently wrong on both sides. Only known vectors catch that.
2. Nothing in the crypto stops it: the card is validly signed by the attacker. Comparing
   fingerprints in person does.
3. A Keystore key can't be exported as raw bytes, and SQLCipher needs raw bytes, so we keep a
   random key on disk sealed by the Keystore key.
4. Opening the sealed files fails, because the Keystore master key exists only on the original
   phone. That's intended. Encrypted export/transfer is a later feature.
</details>
