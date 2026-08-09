# OmniLex Phase 2 Implementation Plan: Data Engine & Unified Schema

Phase 2 transforms OmniLex from a sample app into a robust linguistic data engine. We will extend the schema to support the deep attributes required for the "Wonder Wheel" in Phase 3 and implement an import pipeline for real-world datasets.

## User Review Required

> [!IMPORTANT]
> **Data Size & Performance**: Importing full datasets like WordNet or Wiktionary will significantly increase the app's storage footprint and may impact initial build times. I recommend starting with a subset of WordNet (e.g., core 5,000 words) for the first iteration of the importer to ensure stability.

> [!WARNING]
> **Database Migrations**: Changes to the Room schema will require a database migration or a destructive recreation. Since this is still in development, I will assume a destructive recreation (clearing existing sample data) is acceptable to simplify the process.

## Proposed Changes

### 1. Data Model & Schema Expansion
We will enhance the Room entities to capture the full breadth of the OmniLex vision.

#### [MODIFY] [LexicalModels.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/model/LexicalModels.kt)
- Add `frequency: Double?` to `LexicalEntry` (based on Zipf scale or raw counts).
- Add `etymologyText: String?` to `LexicalEntry` (or create a dedicated `Etymology` table if complex history is needed).
- Add `morphemes: String?` for structural word breakdown.

#### [NEW] [LexicalFts.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/model/LexicalFts.kt)
- Create an FTS4/FTS5 entity for `lexical_entries` to enable fast infix/suffix/wildcard searching.

---

### 2. Search Engine Upgrade
Transition from simple `LIKE` queries to a powerful FTS-based engine.

#### [MODIFY] [OmniLexDao.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/local/OmniLexDao.kt)
- Add `@RawQuery` or specialized FTS search methods for partial matching, phoneme fragments, and wildcards.

#### [MODIFY] [LexicalRepository.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/repository/LexicalRepository.kt)
- Refactor the `search` method to prioritize exact matches and use the new FTS index for fuzzy/fragment matching.

---

### 3. Import Pipeline (The Core of Phase 2)
Implement a robust way to ingest external linguistic data.

#### [NEW] [WordNetImporter.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/importing/WordNetImporter.kt)
- Initial implementation of `LexicalImporter` for the WordNet database.
- Handles mapping of Synsets to `LexicalEntry` and `Sense`, and Pointers to `LexicalRelationship`.

#### [NEW] [ImportService.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/java/org/omnilex/data/importing/ImportManager.kt)
- A coordinator to manage multiple importers and track overall progress.

---

### 4. UI & Detailed Information
Update the UI to reflect the new data density.

#### [MODIFY] [OmniLexApp.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex Phase 1/app/src/main/ui/OmniLexApp.kt)
- Enhance the Detail Screen to display frequency stats, etymology, and source provenance.
- Improve the "Completeness" indicator to show *exactly* what data is missing (e.g., "Missing IPA", "Missing Antonyms").

## Verification Plan

### Automated Tests
- **Unit Tests**: Test the FTS search logic with various wildcard and fragment patterns.
- **Integration Tests**: Verify that `WordNetImporter` correctly populates the database from a sample WordNet file.

### Manual Verification
- Deploy to an emulator/device.
- Run a sample import.
- Search for "ough" to verify infix phoneme/spelling matching.
- Inspect the detail view of a word like "bank" to see multi-source reconciliation.
