# ADR 0001: Native Kotlin + Jetpack Compose, with a pure-JVM core module

**Status:** Accepted, 2026-10-16

## Context
The hard parts of this app are BLE GATT, Wi-Fi Aware, foreground services, the Android Keystore,
and low-level audio, and all of them are native Android APIs. iOS can't use Wi-Fi Aware/Direct,
so a cross-platform UI would gain little. This is also a learning project and needs to be
readable.

## Decision
- Kotlin + Jetpack Compose for the app.
- Protocol logic (identity, envelope, routing) lives in `core/`, a plain Kotlin/JVM module with
  no Android imports.
- No DI framework (Hilt) and no navigation library until the app is big enough to need them.

## Consequences
- \+ Direct access to every radio API, with no plugin layer to debug.
- \+ `core/` tests run on any PC in seconds, with no emulator.
- \+ `core/` can later be ported to Rust (via UniFFI) if iOS or desktop becomes a goal.
- − Android only. That's acceptable: the offline mesh is Android-first anyway.

## Alternatives considered
- **Flutter / React Native:** every hard feature would still need a Kotlin plugin, so you'd
  debug two layers instead of one.
- **Rust core from day one:** good on a resume, but it means learning Rust, the NDK, and
  Android at the same time. It's an upgrade path, not the starting point.
