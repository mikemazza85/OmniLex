# Walkthrough - Phase 4: Dual-Layered Semantic Definitions

Implemented a specialized "Dual Definition" model that distinguishes between experiential learned concepts and formal academic meanings, inspired by high-utility dictionary interfaces.

## Key Features

### 1. The Stacked Meaning Block
Dictionary entries now present a cognitive hierarchy for every sense:
- **Experiential Layer (White)**: The primary, usage-based definition. This represents the "learned concept" as understood in natural language.
- **Academic Layer (Light Blue)**: The secondary, formal definition. This provides technical and educational context for deeper study.

### 2. Universal Semantic Interactivity
- **Cross-Layer Hyperlinks**: Both the Experiential and Academic layers are fully interactive.
- **Intelligent Navigation**: Clicking a word in the formal Blue section correctly resolves to its corresponding entry, leveraging the Phase 3.5 context-engine.

### 3. Data Schema Evolution
- Updated the `Sense` entity to store these two distinct semantic layers.
- Enhanced the `SampleLexicon` and `WordNetImporter` to provide contrasting definitions for common terms (e.g., "bank", "knowledge", "river").

## Technical Details
- **UI Architecture**: Used a nested `Surface` within the meaning card to create the visually distinct blue-background academic section.
- **Color Schema**: Standardized on `#E3F2FD` (Soft Blue) for the academic layer to ensure high legibility while providing clear visual grouping.

## Verification Result
- **Visual Stacking**: Confirmed on physical device ZD222CYT8R via the "bank" and "knowledge" entries.
- **Interaction**: Verified that users can jump between related experiential concepts by tapping keywords in either layer.

render_diffs(file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/ui/OmniLexApp.kt)
render_diffs(file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/data/model/LexicalModels.kt)
