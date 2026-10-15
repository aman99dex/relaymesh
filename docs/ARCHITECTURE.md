# Architecture

```
UI (Jetpack Compose)
  └─ Chat logic: contacts, conversations, settings
       └─ Crypto: identity (Ed25519) → E2E sessions (Signal protocol) → groups (MLS, later)
            └─ Envelope: an encrypted, signed blob with a message ID, a TTL, and padding
                 └─ Transport router: picks the best path that's available right now
                      ├─ Internet: Nostr relays (messages), WebRTC (calls)
                      ├─ BLE mesh: GATT links, TTL flooding, store-and-forward
                      ├─ Wi-Fi Aware: a fast single-hop link (calls, files)
                      ├─ LAN / hotspot
                      └─ LoRa via Meshtastic radio (later)
```

## The one rule

**A message is encrypted into an envelope before any transport sees it.** Transports move
opaque bytes and nothing else. So:

- The same message can travel over the internet, over a stranger's phone in the mesh, or over
  LoRa, and every one of those paths is equally untrusted.
- Adding a transport never touches the crypto code, and changing the crypto never touches a
  transport.
- Switching between online and offline is a routing decision, not a feature that needs its own
  code.

## Modules

| Module | Contains | Why separate |
|---|---|---|
| `core/` | Pure Kotlin/JVM: identity, contact cards, envelope format, routing logic | No Android dependency, so it unit-tests on any PC in seconds and can be ported to Rust or iOS later |
| `app/` | Android only: UI, Keystore, the encrypted database, radios, background services | Everything that needs a real phone |

## Where data lives

All data stays on the device. Nothing is stored on a server.

| Data | Where | Protected by |
|---|---|---|
| Identity private key | `files/identity.key` | AES-256-GCM, using a Keystore master key that can't be exported (StrongBox when available) |
| Messages, contacts | `databases/relaymesh.db` | SQLCipher (whole-file AES-256); its random key is sealed the same way |
| Display name, settings | SharedPreferences | Not secret |
| Backups | None | `allowBackup=false` + data-extraction rules |

See the decision records in [`adr/`](adr) for why each choice was made, and the
[`learn/`](learn) notes for the concepts behind them.
