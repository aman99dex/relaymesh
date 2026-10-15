# ADR 0003: Crypto stack built only from vetted primitives

**Status:** Accepted, 2026-10-16

## Context
"Highly encrypted" is only real if no one hand-rolls the cryptography. We need identity,
asynchronous E2E sessions (peers are often offline), link encryption between neighbors,
encryption at rest, and eventually groups.

## Decision

| Layer | Choice | Milestone |
|---|---|---|
| Root identity | Ed25519 (BouncyCastle lightweight API in `core/`) | M0 |
| Contact verification | Self-signed QR card + fingerprint compared in person | M0 |
| At rest | Android Keystore AES-256-GCM master key (StrongBox if present) seals the identity key and a random SQLCipher key | M0 |
| 1:1 sessions | `libsignal` (PQXDH + Double Ratchet); the Signal identity key is signed by the Ed25519 root key | M1 |
| Neighbor links | Noise XX handshake | M2 |
| Groups | MLS (RFC 9420) | M6 |

Rules: domain-separate every signature (prefix with a context string such as
`relaymesh-card-v1`), test against published test vectors (RFC 8032), and keep secrets out of
logs and backups.

## Consequences
- \+ Forward secrecy and post-compromise security come from a protocol that has been audited.
- \+ One root key to verify per contact; device and session keys hang off it.
- − `libsignal` is AGPL-3.0, so the whole app is AGPL (see ADR 0005).
- − Adds BouncyCastle (~large jar). Acceptable for M0; R8 shrinks it in release builds.

## Alternatives considered
- **Writing a Double Ratchet ourselves:** a great learning exercise, but not for production. If
  you want to learn it, do it in a throwaway branch and compare against libsignal's test vectors.
- **Android Keystore Ed25519:** not available across our minSdk range.
