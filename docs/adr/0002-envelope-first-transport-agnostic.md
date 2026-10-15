# ADR 0002: Encrypt into an envelope first; transports only carry opaque bytes

**Status:** Accepted, 2026-10-16

## Context
The app must work over the internet when it's available and fall back to BLE, Wi-Fi Aware, LAN,
or LoRa when it isn't. Relaying phones in the mesh belong to strangers.

## Decision
Every message is end-to-end encrypted, signed, and padded into an `Envelope` before routing.
A `Transport` interface (`send(bytes)`, `incoming: Flow<bytes>`, `isAvailable`) has one
implementation per radio. A router picks among them; the transports know nothing about crypto.

This is one of the few places where an interface with several implementations is justified
from day one, because there really will be four or more.

## Consequences
- \+ Every path is treated as hostile, so security doesn't depend on which radio carried the
  message.
- \+ Adding LoRa later means writing one new class.
- \+ The same message ID allows deduplication when a message arrives over two paths.
- − Fixed-size padding wastes bandwidth on BLE. That's an accepted trade for hiding message
  length.

## Alternatives considered
- **Per-transport encryption** (as in "Bluetooth pairing is secure enough"): rejected. Relays
  would see plaintext, and every transport would duplicate the crypto logic.
