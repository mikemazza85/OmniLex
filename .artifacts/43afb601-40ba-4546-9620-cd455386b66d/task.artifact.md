# Phase 4 Task List - Dual-Layered Semantic Definitions

- `[x]` **Data Layer: Semantic Refinement**
    - `[x]` Update `Sense` entity in `LexicalModels.kt` to include `experientialDefinition` and `academicDefinition`
    - `[x]` Increment database version in `OmniLexDatabase.kt` and handle migration
    - `[x]` Update `WordNetImporter.kt` and `SampleLexicon.kt` to populate both definition fields
- `[x]` **Presentation Layer: Dual-Layer UI**
    - `[x]` Define "Academic Blue" color in the UI theme
    - `[x]` Update `EntryContent` in `OmniLexApp.kt` to render the stacked White-over-Blue meaning block
    - `[x]` Ensure both definition layers use `ClickableLexicalText`
- `[ ]` **Verification**
    - `[ ]` Verify visual stacking on the physical device
    - `[ ]` Verify cross-layer word interactivity
