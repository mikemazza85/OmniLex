package org.omnilex.data.model

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "lexical_entries_fts")
@Fts4
data class LexicalEntryFts(
    val entryId: String,
    val headword: String,
    val normalizedHeadword: String,
    val ipa: String?,
    val phonemeSegments: String?
)
