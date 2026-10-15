# ADR 0004: Internet transport uses Nostr relays for messages and WebRTC for calls

**Status:** Proposed (implemented in M1/M4)

## Context
Online delivery needs a place to drop messages for offline recipients. We don't want to run a
server: it costs money, it's a single point of failure, and it's a place where metadata piles up.

## Decision
- **Messages:** publish envelopes to several public Nostr relays (users can add their own),
  wrapped NIP-59-style so relays can't see the sender or recipient.
- **Calls:** WebRTC peer-to-peer, with signaling sent as ordinary encrypted envelopes. Use
  public STUN servers; TURN is configurable.
- **Optional:** route through Tor for IP privacy (a settings toggle).

## Consequences
- \+ No server to deploy; thousands of relays exist, which makes blocking hard.
- \+ Relays hold only ciphertext, and only temporarily.
- − Public relays may drop data; mitigate by publishing to 3 or more and retrying.
- − Without Tor, relays see IP addresses. This is documented in the threat model.

## Alternatives considered
- **Own WebSocket server:** simplest, but it's infrastructure to run and a central choke point.
- **libp2p:** powerful, but heavy on Android and adds DHT complexity we don't need.
