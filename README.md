# RelayMesh

[![CI](https://github.com/aman99dex/relaymesh/actions/workflows/ci.yml/badge.svg)](https://github.com/aman99dex/relaymesh/actions/workflows/ci.yml)

An end-to-end encrypted, local-first Android messenger. It works over the internet like a normal
chat app, and when there's no signal it relays messages phone to phone over Bluetooth LE and
Wi-Fi Aware.

- **No account, no phone number.** Your identity is a key pair created on your phone.
- **Your data stays on the device**, in an encrypted database whose key is protected by the
  Android Keystore.
- **Every path is untrusted.** Messages are sealed before any transport (internet relay or a
  stranger's phone in the mesh) sees them.

**Status:** M0 (identity, QR contact exchange, encrypted storage). See the [roadmap](docs/ROADMAP.md).

## Try it
- **No setup:** download `relaymesh-debug-apk` from the latest
  [CI run](https://github.com/aman99dex/relaymesh/actions) and install it on your phone.
- **Develop:** open the folder in Android Studio and run `app`. Core tests: `./gradlew :core:test`.

## Docs
- [Architecture](docs/ARCHITECTURE.md)
- [Decision records](docs/adr): why Kotlin, envelope-first, the crypto stack, Nostr/WebRTC, AGPL
- [Learning notes](docs/learn): concepts behind each milestone, with quizzes
- [Transport research](RESEARCH.md): BLE, Wi-Fi Aware, LoRa, IR, and whether calls are possible

## License
AGPL-3.0. See [ADR 0005](docs/adr/0005-license-agpl.md).
