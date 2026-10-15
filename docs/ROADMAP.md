# Roadmap

Each milestone ends with a working demo, a LEARN note (`docs/learn/`), and an ADR for every new
decision.

| # | Milestone | Done when | You learn |
|---|---|---|---|
| **M0** ✅ | Identity, QR contact exchange, encrypted storage | Two phones scan each other's codes and see matching fingerprints | Compose, Android Keystore, Ed25519, encryption at rest |
| M1 | 1:1 E2E chat over the internet (libsignal + Nostr relays) | Two phones on different networks chat; the relay sees only ciphertext | Signal protocol (X3DH/PQXDH, Double Ratchet), WebSockets, coroutines/Flow |
| M2 | BLE chat between 2 phones, 1 hop | Messages work in airplane mode with Bluetooth on | GATT, Android 12+ permissions, foreground services |
| M3 | Multi-hop mesh + transport router | A → B → C works with A and C out of range; switches automatically between internet and mesh | TTL flooding, deduplication, store-and-forward / delay-tolerant networking |
| M4 | Voice notes (Codec2/Opus), then calls (WebRTC online, Wi-Fi Aware offline) | A push-to-talk note crosses 3 hops; a 1-hop offline call works | NDK/C interop, audio codecs, jitter buffers |
| M5 | Hardening | THREAT_MODEL.md, panic wipe, metadata padding, reproducible build, F-Droid listing | Security engineering |
| M6 | Stretch goals | MLS groups, Meshtastic LoRa bridge, Rust core | Advanced protocols |

## Measure everything (resume numbers)

Latency per hop, delivery rate over N hops, battery drain per hour in relay mode, and APK size.
Record them in `docs/benchmarks.md` as each milestone lands.
