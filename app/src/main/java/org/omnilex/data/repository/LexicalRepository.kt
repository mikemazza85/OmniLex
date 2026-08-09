package org.omnilex.data.repository

import kotlinx.coroutines.flow.*
import org.omnilex.data.local.OmniLexDao
import org.omnilex.data.model.*

class LexicalRepository(
    private val dao: OmniLexDao
) 
{
    fun search(rawQuery: String): Flow<List<SearchResult>> {
        val query = SearchNormalizer.normalize(rawQuery)
        if (query.isBlank()) return flowOf(emptyList())
        val spelling = dao.searchSpelling(query)
        val phonetic = dao.searchPhonetic(SearchNormalizer.soundex(query))
        return combine(spelling, phonetic) { words, sounds ->
            (words + sounds).distinctBy { it.id }.take(50)
        }
    }

    fun entry(id: String): Flow<EntryDetail?> = combine(dao.observeEntry(id), dao.observeSenses(id), dao.observeRelationships(id)) { entry, senses, links ->
        entry?.let { EntryDetail(it, senses, links) }
    }

    suspend fun seedIfEmpty() {
        if (dao.countEntries() != 0) return
        dao.insertEntries(SampleLexicon.entries)
        dao.insertSenses(SampleLexicon.senses)
        dao.insertRelationships(SampleLexicon.relationships)
    }
}

object SearchNormalizer {
    fun normalize(value: String): String = value.trim().lowercase().replace("*", "%").replace(Regex("[^a-z0-9%' -]"), "")
    /** Lightweight phonetic index for Phase 1; replace/augment with IPA index in Phase 2. */
    fun soundex(raw: String): String {
        val word = normalize(raw).filter { it.isLetter() }
        if (word.isEmpty()) return ""
        val map = mapOf('b' to '1','f' to '1','p' to '1','v' to '1','c' to '2','g' to '2','j' to '2','k' to '2','q' to '2','s' to '2','x' to '2','z' to '2','d' to '3','t' to '3','l' to '4','m' to '5','n' to '5','r' to '6')
        val digits = word.drop(1).mapNotNull { map[it] }.fold(StringBuilder()) { acc, c -> if (acc.lastOrNull() != c) acc.append(c); acc }.toString()
        return ("${word.first().uppercaseChar()}$digits" + "000").take(4)
    }
}

/** Contract for Phase 2 source importers. Importers must preserve licensing and source-level provenance. */
interface LexicalImporter { suspend fun import(): ImportReport }
data class ImportReport(val sourceId: String, val inserted: Int, val updated: Int, val warnings: List<String>)

/** Contract for Phase 3 visualization providers. UI can remain independent of layout engine. */
interface LexicalGraphRepository { suspend fun neighborhood(entryId: String, depth: Int = 1): LexicalGraph }
data class LexicalGraph(val nodes: List<GraphNode>, val edges: List<GraphEdge>)
data class GraphNode(val entryId: String, val label: String, val weight: Float)
data class GraphEdge(val fromId: String, val toId: String, val type: RelationshipType, val confidence: Float)
