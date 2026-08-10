package org.omnilex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.omnilex.data.model.*

@Dao
interface OmniLexDao {
    @Query("""
        SELECT e.id, e.headword, e.partOfSpeech, e.ipa, 'spelling' AS matchReason 
        FROM lexical_entries e
        JOIN lexical_entries_fts f ON e.id = f.entryId
        WHERE lexical_entries_fts MATCH :query
        ORDER BY CASE WHEN e.headword = :exact THEN 0 ELSE 1 END, e.headword
        LIMIT :limit
    """)
    fun searchFts(query: String, exact: String, limit: Int = 50): Flow<List<SearchResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFts(items: List<LexicalEntryFts>)

    @Query("SELECT id, headword, partOfSpeech, ipa, 'spelling' AS matchReason FROM lexical_entries WHERE normalizedHeadword LIKE '%' || :query || '%' ORDER BY CASE WHEN normalizedHeadword = :query THEN 0 WHEN normalizedHeadword LIKE :query || '%' THEN 1 ELSE 2 END, headword LIMIT :limit")
    fun searchSpelling(query: String, limit: Int = 50): Flow<List<SearchResult>>

    @Query("SELECT id, headword, partOfSpeech, ipa, 'phonetic' AS matchReason FROM lexical_entries WHERE phoneticCode LIKE :code || '%' ORDER BY headword LIMIT :limit")
    fun searchPhonetic(code: String, limit: Int = 50): Flow<List<SearchResult>>

    @Query("SELECT * FROM lexical_entries WHERE id = :id") fun observeEntry(id: String): Flow<LexicalEntry?>
    @Query("SELECT * FROM senses WHERE entryId = :entryId ORDER BY ordering") fun observeSenses(entryId: String): Flow<List<Sense>>
    @Query("SELECT r.*, e.headword, e.ipa FROM relationships r JOIN lexical_entries e ON e.id = r.toEntryId WHERE r.fromEntryId = :entryId ORDER BY r.type, e.headword")
    fun observeRelationships(entryId: String): Flow<List<RelationshipDisplay>>

    @Query("SELECT * FROM lexical_entries WHERE id = :id") suspend fun getEntry(id: String): LexicalEntry?
    @Query("SELECT r.*, e.headword, e.ipa FROM relationships r JOIN lexical_entries e ON e.id = r.toEntryId WHERE r.fromEntryId = :entryId")
    suspend fun getOutgoingRelationships(entryId: String): List<RelationshipDisplay>
    @Query("SELECT r.*, e.headword, e.ipa FROM relationships r JOIN lexical_entries e ON e.id = r.fromEntryId WHERE r.toEntryId = :entryId")
    suspend fun getIncomingRelationships(entryId: String): List<RelationshipDisplay>

    @Query("SELECT * FROM lexical_entries WHERE headword = :headword")
    suspend fun getEntriesByHeadword(headword: String): List<LexicalEntry>

    @Query("""
        SELECT r.*, e.headword, e.ipa FROM relationships r 
        JOIN lexical_entries e ON (e.id = r.toEntryId OR e.id = r.fromEntryId)
        WHERE (r.fromEntryId = :contextId AND r.toEntryId IN (:candidateIds))
           OR (r.toEntryId = :contextId AND r.fromEntryId IN (:candidateIds))
    """)
    suspend fun getRelationshipsBetween(contextId: String, candidateIds: List<String>): List<RelationshipDisplay>

    @Query("SELECT COUNT(*) FROM lexical_entries") suspend fun countEntries(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntries(items: List<LexicalEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSenses(items: List<Sense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRelationships(items: List<LexicalRelationship>)
}
