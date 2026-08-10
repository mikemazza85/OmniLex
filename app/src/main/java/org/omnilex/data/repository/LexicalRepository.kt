package org.omnilex.data.repository

import kotlinx.coroutines.flow.*
import org.omnilex.data.local.OmniLexDao
import org.omnilex.data.model.*

class LexicalRepository(
    val dao: OmniLexDao
) : LexicalGraphRepository
{
    fun search(rawQuery: String): Flow<List<SearchResult>> {
        val query = SearchNormalizer.normalize(rawQuery)
        if (query.isBlank()) return flowOf(emptyList())

        // Format for FTS: append * for prefix matching
        val ftsQuery = if (query.endsWith("%")) query.replace("%", "*") else "$query*"
        
        val ftsResults = dao.searchFts(ftsQuery, query)
        val spelling = dao.searchSpelling(query)
        val phonetic = dao.searchPhonetic(SearchNormalizer.soundex(query))
        
        return combine(ftsResults, spelling, phonetic) { fts, words, sounds ->
            (fts + words + sounds).distinctBy { it.id }.take(50)
        }
    }

    fun entry(id: String): Flow<EntryDetail?> = combine(dao.observeEntry(id), dao.observeSenses(id), dao.observeRelationships(id)) { entry, senses, links ->
        entry?.let { EntryDetail(it, senses, links) }
    }

    override suspend fun neighborhood(entryId: String, depth: Int): LexicalGraph {
        val nodes = mutableMapOf<String, GraphNode>()
        val edges = mutableSetOf<GraphEdge>()
        val visited = mutableSetOf<String>()
        val toVisit = mutableListOf(entryId to 0)

        while (toVisit.isNotEmpty()) {
            val (currentId, currentDepth) = toVisit.removeAt(0)
            if (currentId in visited || currentDepth > depth) continue
            visited.add(currentId)

            val entry = dao.getEntry(currentId) ?: continue
            nodes[currentId] = GraphNode(currentId, entry.headword, entry.completeness / 100f)

            if (currentDepth < depth) {
                val outgoing = dao.getOutgoingRelationships(currentId)
                val incoming = dao.getIncomingRelationships(currentId)

                outgoing.forEach { rel ->
                    edges.add(GraphEdge(currentId, rel.relationship.toEntryId, rel.relationship.type, rel.relationship.confidence))
                    toVisit.add(rel.relationship.toEntryId to currentDepth + 1)
                }
                incoming.forEach { rel ->
                    edges.add(GraphEdge(rel.relationship.fromEntryId, currentId, rel.relationship.type, rel.relationship.confidence))
                    toVisit.add(rel.relationship.fromEntryId to currentDepth + 1)
                }
            }
        }

        // Final pass to ensure all endpoints have nodes
        edges.forEach { edge ->
            listOf(edge.fromId, edge.toId).forEach { id ->
                if (id !in nodes) {
                    dao.getEntry(id)?.let { nodes[id] = GraphNode(id, it.headword, it.completeness / 100f) }
                }
            }
        }

        return LexicalGraph(nodes.values.toList(), edges.toList())
    }

    suspend fun resolveContextualEntry(targetHeadword: String, contextEntryId: String?): ResolutionResult {
        val candidates = dao.getEntriesByHeadword(targetHeadword)
        if (candidates.isEmpty()) return ResolutionResult.NotFound
        if (candidates.size == 1) return ResolutionResult.Resolved(candidates.first().id)
        
        if (contextEntryId == null) return ResolutionResult.Ambiguous(candidates)

        // Find existing relationships to context
        val rels = dao.getRelationshipsBetween(contextEntryId, candidates.map { it.id })
        if (rels.isNotEmpty()) {
            // Pick the candidate with the strongest tie to the current context
            val bestId = rels.maxBy { it.relationship.confidence }.let {
                if (it.relationship.fromEntryId == contextEntryId) it.relationship.toEntryId else it.relationship.fromEntryId
            }
            return ResolutionResult.Resolved(bestId)
        }

        return ResolutionResult.Ambiguous(candidates)
    }

    suspend fun seedIfEmpty() {
        if (dao.countEntries() != 0) return
        dao.insertEntries(SampleLexicon.entries)
        dao.insertSenses(SampleLexicon.senses)
        dao.insertRelationships(SampleLexicon.relationships)
        
        val ftsItems = SampleLexicon.entries.map { 
            LexicalEntryFts(it.id, it.headword, it.normalizedHeadword, it.ipa, it.phonemeSegments) 
        }
        dao.insertFts(ftsItems)
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

sealed class ResolutionResult {
    data class Resolved(val entryId: String) : ResolutionResult()
    data class Ambiguous(val candidates: List<LexicalEntry>) : ResolutionResult()
    object NotFound : ResolutionResult()
}
