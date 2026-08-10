# Implementation Plan - Phase 3.5: Intelligent Navigation & View Preferences

This plan finalizes Phase 3 by adding "Contextual Intelligence" to word navigation and allowing users to choose their preferred default view (Standard List vs. Interactive Spider).

## User Review Required

> [!IMPORTANT]
> **Intelligent Context Resolution**: When you click a word like "bank" in the definition of "river", the app will automatically resolve to the "river bank" entry instead of the "financial bank" by analyzing existing relationships.

> [!TIP]
> **View Defaults**: We are adding a preference setting. If you prefer the Interactive Spider view, you can set it as default so that every word you click or search opens directly in the graph.

## Proposed Changes

### 1. Data Layer: Contextual Intelligence

#### [MODIFY] [OmniLexDao.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/data/local/OmniLexDao.kt)
- Add `getEntriesByHeadword(headword: String)`: Finds all homonyms for a given spelling.
- Add `getRelationshipsBetween(entryId: String, candidateIds: List<String>)`: Fetches any semantic links between the current context and the list of possible words.

#### [MODIFY] [LexicalRepository.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/data/repository/LexicalRepository.kt)
- Implement `resolveContextualEntry(targetHeadword, contextEntryId)`:
    - Fetches all entries matching `targetHeadword`.
    - If only one entry exists, return it.
    - If multiple (homonyms) exist, query relationships to `contextEntryId`.
    - Returns the candidate with the highest relationship confidence.
    - Fallback: Returns the full list if no relationships exist (triggers a chooser UI).

### 2. Presentation Layer: Global Interactivity & Preferences

#### [MODIFY] [OmniLexViewModel.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/ui/OmniLexViewModel.kt)
- Add `preferGraphView: StateFlow<Boolean>`: Tracks user preference for the default opening view.
- Add `navigationIntent: StateFlow<NavigationTarget?>`: A state to trigger navigation to either an entry or a disambiguation dialog.
- Add `toggleViewPreference()`: Allows the user to switch defaults.

#### [MODIFY] [OmniLexApp.kt](file:///C:/Users/Mikem/Documents/Codex/2026-07-23/OmniLex%20Phase%201/app/src/main/java/org/omnilex/ui/OmniLexApp.kt)
- **ClickableLexicalText**: A new helper that uses `AnnotatedString` to make every word in a definition clickable.
- **Disambiguation Dialog**: A simple popup to choose between homonyms if the "Intelligent Resolver" finds no clear association.
- **View Toggle UI**: Add a "Default View: Graph/List" switch in the Search Top Bar.

## Verification Plan

### Manual Verification
- **Test Contextual Hit**: Open "river". Click "bank" in the definition. Ensure it opens the river-bank entry.
- **Test Miss-click Fallback**: Click a word with no relationship to the current one. Ensure it shows the "Choose Meaning" dialog or searches.
- **Test View Preference**: Set default to "Graph". Search for "wisdom". Ensure it opens the Spider View directly.
