package org.omnilex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.omnilex.data.model.EntryDetail
import org.omnilex.data.model.RelationshipType
import org.omnilex.data.repository.LexicalRepository

@Composable
fun OmniLexApp(
    repository: LexicalRepository
) {
    val viewModel: OmniLexViewModel = viewModel(
        factory = OmniLexViewModelFactory(repository)
    )
    val nav = rememberNavController()
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF355C7D))) {
        NavHost(navController = nav, startDestination = "search") {
            composable("search") { SearchScreen(viewModel, onEntry = { nav.navigate("entry/$it") }) }
            composable("entry/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                EntryScreen(backStack.arguments?.getString("id")!!, viewModel, onBack = { nav.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(viewModel: OmniLexViewModel, onEntry: (String) -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("OmniLex") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Explore words as connected language", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = query, onValueChange = viewModel::search, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search word, fragment, or phonetic match") }, placeholder = { Text("Try: bank, riv, save") })
            Text("Phase 1 supports spelling fragments and a lightweight phonetic index. Graph exploration and source imports are prepared for later phases.", style = MaterialTheme.typography.bodySmall)
            if (query.isNotBlank() && results.isEmpty()) Text("No local matches yet.", modifier = Modifier.padding(top = 20.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { result ->
                    ElevatedCard(onClick = { onEntry(result.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(result.headword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(result.partOfSpeech, result.ipa).joinToString("  "), style = MaterialTheme.typography.bodyMedium)
                            }
                            AssistChip(onClick = { onEntry(result.id) }, label = { Text(result.matchReason) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(id: String, viewModel: OmniLexViewModel, onBack: () -> Unit) {
    val detail by viewModel.entry(id).collectAsState(initial = null)
    Scaffold(topBar = { TopAppBar(title = { Text(detail?.entry?.headword ?: "Entry") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        if (detail == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else EntryContent(detail!!, Modifier.padding(padding))
    }
}

@Composable
private fun EntryContent(detail: EntryDetail, modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(detail.entry.headword, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(detail.entry.partOfSpeech, detail.entry.ipa, detail.entry.dialect).joinToString(" · "))
            LinearProgressIndicator(progress = { detail.entry.completeness / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("Data completeness: ${detail.entry.completeness}%", style = MaterialTheme.typography.labelSmall)
        }
        item { Text("Meanings", style = MaterialTheme.typography.titleLarge) }
        items(detail.senses, key = { it.id }) { sense ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(sense.definition, style = MaterialTheme.typography.bodyLarge)
                sense.domain?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                sense.contextualNote?.let { Text("Context: $it", style = MaterialTheme.typography.bodySmall) }
            } }
        }
        item { Text("Connections", style = MaterialTheme.typography.titleLarge) }
        if (detail.relationships.isEmpty()) item { Text("No linked entries in the local dataset.") }
        items(detail.relationships, key = { it.relationship.id }) { link ->
            ListItem(headlineContent = { Text(link.headword) }, supportingContent = { Text(link.relationship.type.label() + " · ${"%.0f".format(link.relationship.confidence * 100)}% confidence") })
            HorizontalDivider()
        }
        item { Text("Graph view (Phase 3)", style = MaterialTheme.typography.titleLarge) }
        item { Text("This entry is already modeled as a graph node. The future interactive radial/spiderweb view will render these connections with type, provenance, and confidence weighting.") }
    }
}

private fun RelationshipType.label() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
