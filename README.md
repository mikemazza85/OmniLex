# OmniLex

An offline-first, open-source-ready linguistic reference explorer. This repository contains Phase 1: a Kotlin/Jetpack Compose Android app with a Room lexical store, search pipeline, entry detail screen, and relationship architecture.

## Run it

Open this folder in Android Studio Ladybug or newer, allow Gradle to sync, then run the `app` configuration on an Android 8.0+ device/emulator. The project uses JDK 17.

## What works now

- Prefix, infix, wildcard, IPA-fragment, and phonetic-code search.
- Offline Room database seeded with example entries (`bank`, `river`, `financial institution`).
- Definitions, IPA pronunciations, semantic/domain labels, and typed relationship links.
- Detail view with an expandable relationship list and data-completeness indicators.

## Planned phases

| Phase | Scope | Architectural hook already present |
| --- | --- | --- |
| 2 | Import/license-aware source adapters and reconciliation | `LexicalSource`, provenance fields, `ImportService` contract |
| 3 | Interactive radial / force-directed lexical graph | `LexicalGraphRepository`, `GraphNode`, `GraphEdge` |
| 4 | Dialects, ambiguity, oxymorons, contribution workflow | typed relationship vocabulary and completeness flags |

## Data and licensing

No proprietary dictionary content is included. Only ingest sources whose licenses permit the intended redistribution. Preserve provenance and license metadata for every imported record.
