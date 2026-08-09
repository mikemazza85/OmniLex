package org.omnilex.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "lexical_entries", indices = [Index(value = ["normalizedHeadword"], unique = true), Index(value = ["phoneticCode"])])
data class LexicalEntry(
    @PrimaryKey val id: String,
    val headword: String,
    val normalizedHeadword: String,
    val partOfSpeech: String?,
    val ipa: String?,
    val phonemeSegments: String?,
    val phoneticCode: String?,
    val languageTag: String = "en",
    val dialect: String?,
    val completeness: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "senses", foreignKeys = [ForeignKey(entity = LexicalEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE)], indices = [Index("entryId")])
data class Sense(
    @PrimaryKey val id: String,
    val entryId: String,
    val definition: String,
    val conceptualNote: String?,
    val contextualNote: String?,
    val usageLabel: String?,
    val domain: String?,
    val ordering: Int
)

enum class RelationshipType {
    SYNONYM, ANTONYM, HYPERNYM, HYPONYM, MERONYM, HOLONYM,
    HOMONYM, HOMOPHONE, ANAGRAM, ETYMOLOGICAL_ORIGIN,
    DIALECT_VARIANT, OXYMORONIC_PAIR, AMBIGUOUS_WITH, RELATED_CONCEPT
}

@Entity(tableName = "relationships", foreignKeys = [
    ForeignKey(entity = LexicalEntry::class, parentColumns = ["id"], childColumns = ["fromEntryId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LexicalEntry::class, parentColumns = ["id"], childColumns = ["toEntryId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("fromEntryId"), Index("toEntryId")])
data class LexicalRelationship(
    @PrimaryKey val id: String,
    val fromEntryId: String,
    val toEntryId: String,
    val type: RelationshipType,
    val confidence: Float = 1f,
    val sourceId: String? = null,
    val note: String? = null
)

@Entity(tableName = "lexical_sources")
data class LexicalSource(
    @PrimaryKey val id: String,
    val name: String,
    val version: String?,
    val license: String,
    val url: String?,
    val importedAt: Long = System.currentTimeMillis()
)

data class SearchResult(val id: String, val headword: String, val partOfSpeech: String?, val ipa: String?, val matchReason: String)
data class RelationshipDisplay(@Embedded val relationship: LexicalRelationship, val headword: String, val ipa: String?)
data class EntryDetail(val entry: LexicalEntry, val senses: List<Sense>, val relationships: List<RelationshipDisplay>)
