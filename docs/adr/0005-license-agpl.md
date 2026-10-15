# ADR 0005: License under AGPL-3.0

**Status:** Accepted, 2026-10-16

## Context
`libsignal` (planned for M1) is AGPL-3.0. Any app that links to it must also be AGPL-compatible.

## Decision
License RelayMesh under AGPL-3.0 from the first commit.

## Consequences
- \+ We can use libsignal. Anyone who distributes a modified version, or runs one as a network
  service, must publish its source under AGPL. That suits a privacy tool where users need to be
  able to audit the code.
- − Closed-source derivatives aren't allowed. That doesn't matter for this project.
