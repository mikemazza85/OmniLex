# OmniLex architecture

## Phase 1 flow

`Compose screen → OmniLexViewModel → LexicalRepository → Room DAO → SQLite`

The UI observes Kotlin `Flow`s, so local imports or contributor updates automatically refresh search and entry details.

## Core model

- `LexicalEntry` represents a searchable form, with IPA, segmented phonemes, language/dialect, and a 0–100 completeness score.
- `Sense` separates definitions from the entry so homonyms and multiple meanings are first-class data.
- `LexicalRelationship` is a directed, typed, confidence-ranked edge between entries. Every edge can retain a source and editorial note.
- `LexicalSource` is reserved for provenance and licensing records.

## Search

Phase 1 combines normalized infix matching with a compact Soundex-style phonetic index. An asterisk means a wildcard (`b*nk`). Phase 2 should add an FTS table, IPA/phoneme token indexes, morphology, phrase tokenization, and language-aware analyzers.

## Import and reconciliation (Phase 2)

Implement `LexicalImporter` for one licensed source at a time. Each importer should normalize records to the core model, preserve original identifiers and license details, and send conflicting records to an explicit reconciliation layer. Never silently merge or redistribute data without validating source terms.

## Graph (Phase 3)

`LexicalGraphRepository` emits graph-neutral nodes and edges. A Compose Canvas or a graph rendering library can then choose radial, force-directed, or accessibility-first list presentation without changing the database model.

## Advanced relationships (Phase 4)

The relationship vocabulary already contains dialect, homonym/homophone, anagram, etymological, ambiguity, and oxymoronic links. More detailed evidence, reviewer state, and source citations should be stored as additive tables rather than replacing existing claims.
