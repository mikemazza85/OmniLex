package org.omnilex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.omnilex.data.importing.ImportState
import org.omnilex.data.model.EntryDetail
import org.omnilex.data.model.LexicalEntry
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
    val navIntent by viewModel.navigationIntent.collectAsState()

    LaunchedEffect(navIntent) {
        when (val intent = navIntent) {
            is NavigationTarget.Entry -> {
                nav.navigate("entry/${intent.id}")
                viewModel.consumeNavigation()
            }
            is NavigationTarget.Graph -> {
                nav.navigate("graph/${intent.id}")
                viewModel.consumeNavigation()
            }
            is NavigationTarget.Search -> {
                viewModel.search(intent.query)
                // Stay on search screen
                viewModel.consumeNavigation()
            }
            else -> {}
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF355C7D))) {
        Box {
            NavHost(navController = nav, startDestination = "search") {
                composable("search") { 
                    SearchScreen(viewModel, onEntry = { viewModel.navigateToEntry(it) }) 
                }
                composable("entry/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                    EntryScreen(backStack.arguments?.getString("id")!!, viewModel, onBack = { nav.popBackStack() }, onGraph = { nav.navigate("graph/$it") })
                }
                composable("graph/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                    GraphScreen(backStack.arguments?.getString("id")!!, viewModel, onBack = { nav.popBackStack() }, onNodeClick = { viewModel.navigateToEntry(it) })
                }
            }

            if (navIntent is NavigationTarget.Disambiguation) {
                val intent = navIntent as NavigationTarget.Disambiguation
                DisambiguationDialog(
                    candidates = intent.candidates,
                    onSelect = { viewModel.navigateToEntry(it.id) },
                    onDismiss = { viewModel.consumeNavigation() }
                )
            }
        }
    }
}

@Composable
fun DisambiguationDialog(candidates: List<LexicalEntry>, onSelect: (LexicalEntry) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Multiple Meanings Found") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates) { candidate ->
                    ElevatedCard(onClick = { onSelect(candidate) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(candidate.headword, fontWeight = FontWeight.Bold)
                            Text(candidate.partOfSpeech ?: "word", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(viewModel: OmniLexViewModel, onEntry: (String) -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    val preferGraph by viewModel.preferGraphView.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importStatus) {
        when (val status = importStatus) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar("Import successful! ${status.reports.sumOf { it.inserted }} items added.")
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar("Import failed: ${status.message}")
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            TopAppBar(
                title = { Text("OmniLex") }, 
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View:", style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = viewModel::toggleViewPreference) {
                            Icon(if (preferGraph) Icons.Default.Share else Icons.AutoMirrored.Filled.List, 
                                contentDescription = "Toggle View Default",
                                tint = if (preferGraph) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::triggerImport, 
                        enabled = importStatus !is ImportState.Running,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (importStatus is ImportState.Running) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Importing...")
                        } else {
                            Text("Import Data")
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Explore words as connected language", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = query, onValueChange = viewModel::search, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Search word, fragment, or phonetic match") }, placeholder = { Text("Try: bank, riv, save") })
                
                Text("Phase 3.5: Context-Aware Navigation. Click any word in a definition to explore related meanings.", style = MaterialTheme.typography.bodySmall)
                
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
            
            if (importStatus is ImportState.Running) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(id: String, viewModel: OmniLexViewModel, onBack: () -> Unit, onGraph: (String) -> Unit) {
    val detail by viewModel.entry(id).collectAsState(initial = null)
    Scaffold(topBar = { TopAppBar(title = { Text(detail?.entry?.headword ?: "Entry") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        if (detail == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else EntryContent(detail!!, id, viewModel, Modifier.padding(padding), onGraph = { onGraph(id) })
    }
}

@Composable
private fun EntryContent(detail: EntryDetail, currentEntryId: String, viewModel: OmniLexViewModel, modifier: Modifier = Modifier, onGraph: () -> Unit) {
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(detail.entry.headword, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(detail.entry.partOfSpeech, detail.entry.ipa, detail.entry.dialect).joinToString(" · "))
            
            detail.entry.frequency?.let { freq ->
                Text("Usage Frequency: ${"%.2f".format(freq)} (Zipf)", style = MaterialTheme.typography.labelMedium)
            }
            
            LinearProgressIndicator(progress = { detail.entry.completeness / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("Data completeness: ${detail.entry.completeness}%", style = MaterialTheme.typography.labelSmall)
            
            if (detail.entry.completeness < 100) {
                val missing = mutableListOf<String>()
                if (detail.entry.ipa == null) missing.add("IPA")
                if (detail.entry.etymologyText == null) missing.add("Etymology")
                if (detail.relationships.none { it.relationship.type == RelationshipType.ANTONYM }) missing.add("Antonyms")
                if (missing.isNotEmpty()) {
                    Text("Missing: ${missing.joinToString(", ")}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        detail.entry.etymologyText?.let {
            item {
                Text("Etymology", style = MaterialTheme.typography.titleLarge)
                ClickableLexicalText(it) { word -> viewModel.resolveAndNavigate(word, currentEntryId) }
            }
        }

        item { Text("Meanings", style = MaterialTheme.typography.titleLarge) }
        items(detail.senses, key = { it.id }) { sense ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Experiential Layer (White)
                    Column(Modifier.padding(16.dp)) {
                        Text("EXPERIENTIAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        ClickableLexicalText(sense.experientialDefinition, style = MaterialTheme.typography.bodyLarge) { word -> 
                            viewModel.resolveAndNavigate(word, currentEntryId) 
                        }
                    }
                    
                    // Academic Layer (Blue)
                    sense.academicDefinition?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE3F2FD) // Soft Academic Blue
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("ACADEMIC", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                                ClickableLexicalText(it, style = MaterialTheme.typography.bodyMedium) { word -> 
                                    viewModel.resolveAndNavigate(word, currentEntryId) 
                                }
                            }
                        }
                    }

                    // Contextual Notes
                    if (sense.domain != null || sense.contextualNote != null) {
                        Column(Modifier.padding(16.dp)) {
                            sense.domain?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                            sense.contextualNote?.let { 
                                Row {
                                    Text("Context: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    ClickableLexicalText(it, style = MaterialTheme.typography.bodySmall) { word -> 
                                        viewModel.resolveAndNavigate(word, currentEntryId) 
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("Connections", style = MaterialTheme.typography.titleLarge) }
        if (detail.relationships.isEmpty()) item { Text("No linked entries in the local dataset.") }
        items(detail.relationships, key = { it.relationship.id }) { link ->
            ListItem(
                headlineContent = { Text(link.headword, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }, 
                supportingContent = { Text(link.relationship.type.label() + " · ${"%.0f".format(link.relationship.confidence * 100)}% confidence") },
                modifier = Modifier.clickable { viewModel.navigateToEntry(link.relationship.toEntryId) }
            )
            HorizontalDivider()
        }
        item { Text("Lexical Web", style = MaterialTheme.typography.titleLarge) }
        item {
            ElevatedButton(
                onClick = onGraph,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Interactive Graph")
            }
        }
        item { Text("Explore connections visually. The radial web view renders relationships with type, provenance, and confidence weighting.") }
    }
}

@Composable
fun ClickableLexicalText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    onWordClick: (String) -> Unit
) {
    val words = text.split(" ")
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            val cleanWord = word.replace(Regex("[^a-zA-Z0-9]"), "")
            if (cleanWord.length > 2) {
                pushStringAnnotation(tag = "WORD", annotation = cleanWord)
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                    append(word)
                }
                pop()
            } else {
                append(word)
            }
            if (index < words.size - 1) append(" ")
        }
    }

    ClickableText(
        text = annotatedString,
        style = style,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "WORD", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onWordClick(annotation.item)
                }
        }
    )
}

private fun RelationshipType.label() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
