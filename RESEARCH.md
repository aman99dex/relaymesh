# RelayMesh research: off-grid phone-to-phone messaging and calls on Android

Goal: phones talk directly to each other with no internet, SIM, or server. Messages hop from
phone to phone over whatever radio is available, so a network shutdown doesn't stop them.

## 1. Short answer

- **Texting over multiple hops: yes, proven.** Briar, bitchat, Bridgefy, and Meshtastic all do
  it. Bluetooth LE mesh with store-and-forward is the standard pattern.
- **Live voice calls: yes, but only over 1 to 2 hops, and only over Wi-Fi (Aware/Direct) or a
  good single BLE link.** Every hop adds delay and packet loss, so a live call over 5 BLE hops
  becomes unusable.
- **Voice over many hops: use push-to-talk voice notes** (walkie-talkie style). Compress them
  with Codec2 (about 1 KB per 5 seconds) and send them through the mesh like a text message.
  Reticulum's Sideband app already sends voice notes this way, even over LoRa.
- **Video:** only over a direct Wi-Fi link (one hop).
- **"Cannot be stopped" has limits:** no ISP or server can block local radio, but jamming,
  confiscating devices, app-store removal, and Android killing background apps can all stop it.
  See section 6.

## 2. Every transport a stock Android phone has

| Transport | Range | Throughput | Hops/mesh | Calls? | Notes |
|---|---|---|---|---|---|
| **BLE (Bluetooth Low Energy)** | 10–100 m (Coded PHY: ~300 m+ on supported phones) | ~100–700 kbps in practice | Yes, at the app layer (bitchat, Bridgefy) | Codec2/Opus-lowbitrate, 1–2 hops | Best default. Runs in the background, every phone has it, iOS can interoperate. |
| **Bluetooth Classic (RFCOMM)** | ~10–30 m | ~1–2 Mbps | App layer | Yes, 1 hop | Needs pairing or an insecure RFCOMM socket. Briar uses it. |
| **Wi-Fi Aware (NAN)** | ~50–100 m | 10s–100s of Mbps | App layer; the NAN spec has data paths, but no built-in routing | **Yes, including video, for 1 hop** | Android 8+, but **hardware support varies** (check `PackageManager.FEATURE_WIFI_AWARE`). Most Pixels and recent Samsungs have it. bitchat-android has a `WifiAwareMeshService`. |
| **Wi-Fi Direct (P2P)** | ~50–100 m | High | Awkward: one group owner per group, so a phone can't easily be in two groups | Yes, 1 hop | Shows connection dialogs and is unreliable across vendors. Works, but painful. |
| **Local hotspot / same Wi-Fi LAN** | Range of the access point | High | Through the AP | Yes | Useful in camps or buildings: one phone hosts, everyone joins. |
| **Google Nearby Connections** | Mixes BT, BLE, and Wi-Fi | Upgrades to Wi-Fi automatically | **Point-to-point only**; you build the routing | Yes, 1 hop | Easiest API, but closed source and needs Google Play Services. Doesn't work on de-Googled phones. |
| **UWB** | ~10–50 m | Ranging, not data | No | No | Android exposes UWB for ranging only, not general data. Skip it. |
| **NFC** | ~4 cm | Low | No | No | Good only for exchanging keys or contacts by tapping. |
| **Infrared (IR blaster)** | Line of sight, a few meters | Very low | No | No | **Transmit only** (`ConsumerIrManager`). Phones have no IR receiver, and few phones have a blaster (mainly Xiaomi/Redmi/POCO). Not usable for two-way communication. |
| **Ultrasound / audio modem** (e.g. ggwave) | 1–5 m, same room | ~10s–100s of bps | No | No | Works on every phone through the speaker and mic. Fun for key exchange or a tiny beacon. |
| **Camera + screen (QR codes)** | Line of sight | Low to medium | No | No | Good for key exchange and offline sync of small data. |
| **SD card / USB / file export** | Physical | Any | "Sneakernet" | No | Briar supports this. It's slow, but nobody can block it. |

### Add-on hardware (cheap, and greatly increases range)

| Option | Range | Calls? | Notes |
|---|---|---|---|
| **LoRa radio + Meshtastic firmware** (~$20–40, pairs with the phone over BLE) | 1–10+ km per hop, more with line of sight | No live calls; tiny voice clips are possible | Huge existing community and firmware. Your app can talk to the radio over its BLE API. |
| **Reticulum (RNode / LoRa, packet radio, anything)** | Kilometers | Voice notes (Codec2), LXST telephony in Sideband on good links | The most transport-agnostic design available; worth studying even if you don't use it. |
| **USB OTG radios / ham packet radio** | Kilometers | Depends | Needs a license for ham bands. |

**Takeaway:** use BLE as the always-on backbone, Wi-Fi Aware or Direct as a high-bandwidth
"booster" when two phones are close, and optional LoRa for long range.

## 3. How hopping actually works

1. **Discovery:** every phone advertises over BLE and scans for others nearby.
2. **Link:** connect over GATT (BLE) or upgrade to Wi-Fi Aware when both phones support it.
3. **Routing:** pick one of two strategies.
   - **Flooding with TTL** (bitchat): each phone rebroadcasts a packet it hasn't seen before
     until its hop count (TTL) runs out, e.g. 7 hops. Simple and robust, but uses a lot of
     bandwidth. Deduplicate with a seen-ID cache or Bloom filter.
   - **Store-and-forward / delay-tolerant networking (DTN)** (Briar, Reticulum): phones hold
     messages and hand them over whenever they meet someone. Messages arrive late but reliably,
     even across a network that is never fully connected at once.
   - Best to combine both: flood live packets, and store-and-forward anything not acknowledged.
4. **Security:** end-to-end encryption with the Noise Protocol (bitchat uses Noise XX) or the
   Signal double ratchet. Relay phones see only ciphertext. Sign packets so relays can't forge
   them. Use ephemeral IDs to limit tracking.
5. **Fragmentation:** a BLE packet carries roughly 20–500 bytes depending on the negotiated MTU,
   so split messages into fragments and reassemble them on arrival.

## 4. Calls in detail

| Mode | Codec | Bitrate | Works over |
|---|---|---|---|
| Live call, Wi-Fi Aware/Direct, 1 hop | Opus | 16–32 kbps (HD) | Fine; video is also possible |
| Live call, BLE, 1 hop | Opus 6–8 kbps or Codec2 3.2 kbps | Low | Feasible; expect some latency |
| Live call, BLE, 2–3 hops | Codec2 1.2–2.4 kbps | Very low | Marginal: latency builds up and quality is robotic but intelligible |
| **Push-to-talk voice note, any number of hops** | Codec2 | ~1.2 kbps (5 s ≈ 750 bytes) | **Reliable.** It's just a message. |
| Over LoRa | Codec2 700C–1.2k | Tiny | Voice notes only |

Libraries: **Opus** (`libopus` via the NDK, or the WebRTC audio stack) and **Codec2** (C library,
compile it with the NDK). For live calls, use a jitter buffer and send audio over UDP-style
datagrams, not TCP streams.

## 5. Existing open-source projects to learn from (or fork)

| Project | What it is | License | Why look |
|---|---|---|---|
| **bitchat-android** (permissionlesstech) | BLE mesh chat with TTL flooding, Noise encryption, and a Wi-Fi Aware transport | Changed to GPLv3 in Feb 2026 (check the LICENSE file) | Closest match to this idea; read `BluetoothMeshService` / `WifiAwareMeshService`. No external audit yet. |
| **Briar** | Bluetooth, Wi-Fi, Tor, and SD-card sync; DTN-style delivery | GPLv3 | Mature and well-designed; text only, no calls. |
| **Meshtastic Android** | Phone app for LoRa radios | GPLv3 | The long-range add-on path |
| **Sideband / Reticulum / LXMF** | Transport-agnostic mesh with voice notes and LXST calls | Reticulum License | Best design reference for "use any signal you find" |
| **Berty / Wesh** | P2P messenger over BLE and LAN | Apache/MIT | Go-based protocol |
| **Bridgefy SDK** | Commercial BLE mesh | Proprietary | Researchers have found serious security flaws in past versions, so use it as a cautionary example |

## 6. Ways the network can be stopped (and mitigations)

| Threat | Reality | Mitigation |
|---|---|---|
| Internet shutdown | Doesn't matter | This is the main point of the app |
| App-store removal | Possible | Distribute through F-Droid, a direct APK, and phone-to-phone APK sharing (Briar does this) |
| **Android killing the background service** | Very real, especially on Xiaomi/Samsung/Oppo | Foreground service of type `connectedDevice`, ask to be exempted from battery optimization, guide users through dontkillmyapp.com |
| Radio jamming (2.4 GHz) | Possible locally | Use several bands (2.4 GHz BT/Wi-Fi, 5 GHz Wi-Fi Aware, sub-GHz LoRa) plus store-and-forward |
| Confiscated or seized phones | Real | Panic button / wipe, encrypted local storage, no persistent identifiers |
| Traffic analysis / tracking | BLE advertisements can be tracked | Rotating ephemeral IDs, random MAC addresses (Android does this by default) |
| Battery drain | Constant scanning uses a lot of power | Duty-cycled scanning, plus a "relay mode" toggle so users choose when to relay for others |

## 7. Android specifics

- **Permissions (Android 12+):** `BLUETOOTH_SCAN` (with `neverForLocation`), `BLUETOOTH_ADVERTISE`,
  `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES` (Android 13+), and `FOREGROUND_SERVICE_CONNECTED_DEVICE`
  (Android 14+).
- **Hardware differences:** BLE Coded PHY (long range) and Wi-Fi Aware support vary by device.
  Detect them at runtime and fall back gracefully.
- **Android 16:** Briar shipped Bluetooth fixes for Android 16 in 2025–26, so expect behavior to
  change between OS versions.
- **iPhone interop:** works only over BLE. Apple doesn't expose Wi-Fi Aware/Direct to apps in a
  compatible way (bitchat iOS and Android share the BLE protocol).

## 8. Recommended MVP (smallest thing that proves it works)

1. Kotlin app with a BLE advertise/scan/GATT foreground service.
2. Text messages with flooding + TTL=7 + a seen-ID cache, fragmented to fit the BLE MTU.
3. Noise XX or libsodium sealed boxes for end-to-end encryption; exchange keys by scanning a QR code.
4. Test with 3 phones in a line, A … B … C, where A and C are out of range of each other. A
   message from A reaching C proves hopping works.
5. Then add: Codec2 push-to-talk voice notes → Wi-Fi Aware upgrade for 1-hop live calls →
   store-and-forward → optional Meshtastic/LoRa bridge.

Alternative fast path: **fork bitchat-android** (GPLv3 means your fork must also be GPLv3) and
add Codec2 voice notes plus Wi-Fi Aware calls on top.

## Sources

- bitchat-android README: https://cdn.jsdelivr.net/gh/permissionlesstech/bitchat-android@main/README.md
- Bitchat analysis, 2026: https://braindetox.kr/en/posts/bitchat_radicle_p2p_messaging_2026.html
- Briar on F-Droid: https://f-droid.org/de/packages/org.briarproject.briar.android/
- Briar review: https://reclaimthenet.org/recommended-tool/briar
- Sideband (Reticulum/LXMF): https://pypi.org/project/sbapp
- Nearby Connections 2.0: https://android-developers.googleblog.com/2017/07/announcing-nearby-connections-20-fully.html
- Wi-Fi Aware overview: https://9to5google.com/2021/03/21/google-wifinanscan-app-android/
