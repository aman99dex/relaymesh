# ADR 0005: License under AGPL-3.0

**Status:** Accepted, 2026-10-16

## Context
`libsignal` (planned for M1) is AGPL-3.0. Any app that links to it must also be AGPL-compatible.

## Decision
License RelayMesh under AGPL-3.0 from the first commit.

## Consequences
- \+ We can use libsignal. Forks must stay open source, which suits a privacy tool where users
  need to be able to audit the code.
- − Companies can't build a closed-source product on it. That doesn't matter for this project.
